import gzip
import logging
from app.db import database
from app.models import FastApiError
from app.parsers.registry import PARSERS
from datetime import datetime
from zoneinfo import ZoneInfo
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)

KYIV_TZ = ZoneInfo("Europe/Kyiv")

app = FastAPI(title="vacancies-monitoring parser")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


def error_response(code: int, snapshot_id: str, message: str, parser_version: str | None) -> JSONResponse:
    body = FastApiError(
        code=code,
        snapshotId=snapshot_id,
        message=message,
        parserVersion=parser_version,
        timestamp=datetime.now(KYIV_TZ).isoformat(),
    )
    return JSONResponse(status_code=code, content=body.model_dump())


async def set_parsing_result(snapshot_id: str, parser_version: str | None, status: str) -> None:
    await database["snapshots"].update_one(
        {"_id": snapshot_id},
        {"$set": {"parserVersion": parser_version, "parsingStatus": status}},
    )
    logger.info("[%s] mongo updated: parserVersion=%s parsingStatus=%s", snapshot_id, parser_version, status)


@app.get("/parse/{snapshot_id}")
async def parse_snapshot(snapshot_id: str):
    logger.info("[%s] parse request received", snapshot_id)

    doc = await database["snapshots"].find_one({"_id": snapshot_id})
    if doc is None:
        logger.error("[%s] snapshot not found", snapshot_id)
        return error_response(404, snapshot_id, "Snapshot not found", parser_version=None)

    job_board = doc.get("jobBoard")
    entry = PARSERS.get(job_board)
    if entry is None:
        logger.error("[%s] no parser registered for jobBoard=%r", snapshot_id, job_board)
        await set_parsing_result(snapshot_id, None, "ERROR")
        return error_response(500, snapshot_id, f"No parser available for jobBoard '{job_board}'", None)

    parse_fn, parser_version = entry
    logger.info("[%s] jobBoard=%s -> parser=%s", snapshot_id, job_board, parser_version)

    try:
        html = gzip.decompress(bytes(doc["gzippedHtml"])).decode("utf-8")
        logger.info("[%s] decompressed html: %d chars", snapshot_id, len(html))
    except Exception as e:
        logger.exception("[%s] failed to gunzip/decode stored html", snapshot_id)
        await set_parsing_result(snapshot_id, parser_version, "ERROR")
        return error_response(500, snapshot_id, f"Failed to decompress stored HTML: {e}", parser_version)

    try:
        vacancies = parse_fn(html, job_board)
        logger.info(
            "[%s] parsed %d vacancies (first: %r @ %r)",
            snapshot_id,
            len(vacancies),
            vacancies[0].title,
            vacancies[0].companyName,
        )
    except Exception as e:
        logger.exception("[%s] parsing failed", snapshot_id)
        await set_parsing_result(snapshot_id, parser_version, "ERROR")
        return error_response(500, snapshot_id, str(e), parser_version)

    await set_parsing_result(snapshot_id, parser_version, "OK")
    logger.info("[%s] done, parsingStatus=OK, %d vacancies returned", snapshot_id, len(vacancies))
    return vacancies


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled exception while handling %s", request.url)
    body = FastApiError(
        code=500,
        snapshotId=request.path_params.get("snapshot_id", "unknown"),
        message=str(exc),
        parserVersion=None,
        timestamp=datetime.now(KYIV_TZ).isoformat(),
    )
    return JSONResponse(status_code=500, content=body.model_dump())
