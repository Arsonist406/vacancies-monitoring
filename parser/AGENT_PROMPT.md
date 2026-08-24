# Implement the `parser` FastAPI service

## Mission

`vacancies-monitoring` is a two-module system. `core` (Java/Spring Boot) is **fully implemented and frozen — do not read this as an invitation to change anything under `core/`.** 
It fetches a job-board listing page, gzips the raw HTML, saves it to MongoDB as a `Snapshot`, then calls `GET {PARSER_URL}/parse/{snapshotId}` on this service and turns the response into a Telegram message.

Your job is the other module: `parser`, a small FastAPI app that:
1. Loads a `Snapshot` document from MongoDB by id.
2. Decides which site-specific parser to use from `jobBoard`.
3. Gunzips the stored HTML and extracts **every** vacancyDto card out of it (the snapshot is a whole listing page, not a single vacancyDto — see "The DOU parser" below).
4. Writes `parserVersion` + `parsingStatus` back onto the same Mongo document.
5. Returns either a JSON **array** of `VacancyDto` objects (success — may contain many items, one per vacancyDto card found) or a single `FastApiError` object (anything went wrong) — always as JSON, always one of exactly these two shapes.

Note on why it's a list: `core` used to notify about a single vacancyDto per snapshot; it's since been changed (`NewVacancyMessageBuilder` now deserializes `Vacancy[]` and formats all of them into one Telegram message) so it can announce every vacancyDto found in one page fetch. Deduplication against previously-seen vacancies will be added later, on the Java side, by some stable key. **You don't need to worry about "is this new" at all — just return everything currently on the page, every time.**

**Guiding principle: keep it simple.** This is a one-endpoint scraper glue service, not a platform. 
Don't add layers, config options, retries, caching, auth, or abstractions "for later."
A straightforward, readable implementation beats a clever one. 
If you're tempted to add a third file just to hold one function, don't.

There is already scaffolding in `parser/app/` (`main.py`, `db.py`, `config.py`) — read it first, it already does the boring plumbing 
(Motor client, pydantic-settings, a `/health` endpoint). `parser/todo.md` is the original one-paragraph spec this document formalizes; you can delete it once this is done.

## Non-negotiable constraints (from the project owner)

1. **Only MongoDB.** No MySQL, no Redis, nothing else — the current scaffold already only has `motor`, which is correct, don't add more storage.
2. **Exactly two HTTP endpoints total**: health-check and `GET /parse/{snapshot_id}`. Nothing else — no `/parsers`, no `/status`, no debug routes.
3. Snapshot data is read from Mongo by `snapshot_id`. There is no other input to the endpoint (no request body, no query params).
4. The JSON you read/write must be **byte-for-byte field-name-compatible** with the Java side — see "Cross-language contract" below. Get this wrong and the Java side throws exceptions.
5. `gzippedHtml` is gzip-compressed — you must decompress it before parsing.
6. All Python tooling/library choices are yours; concrete recommendations are given below to remove ambiguity, but you're not required to follow them slavishly if you find a good reason not to.
7. On read, `parserVersion` and `parsingStatus` are `null`. You determine the parser from `jobBoard`, then **after** attempting to parse, write `parserVersion` and the correct `parsingStatus` (`"OK"` or `"ERROR"`) back onto the same document — in both the success and the failure case (if you got far enough to know which parser applies).
8. The HTTP response is **always** either a JSON array of `VacancyDto` objects or a single `FastApiError` object — never FastAPI's default `{"detail": ...}` shape, not even for a bug you didn't anticipate. See "Never break the contract" below.
9. Comprehensive, genuinely useful console logging — not decorative. This service will be maintained by an AI coding agent going forward with no other observability, so the logs are its primary debugging tool. Every request should leave a readable trail: what snapshot, what job board, what parser/version, how big the decompressed HTML was, what happened, and — on failure — a full stack trace.
10. Don't over-engineer. Simpler is better. Resist adding anything not asked for here.

## Source of truth: the Java models

Read these files directly (paths relative to repo root) rather than trusting this document if anything looks stale:

- `core/src/main/java/dev/arsonist/vacanciesmonitoring/model/Snapshot.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/model/JobBoard.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/model/ParsingStatus.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/dto/Vacancy.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/dto/FastApiError.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/repository/SnapshotRepository.java`
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/service/MainFlowExecutor.java` (how the parser is called)
- `core/src/main/java/dev/arsonist/vacanciesmonitoring/service/messagebuilder/{MessageBuilderFactory,NewVacancyMessageBuilder,ErrorMessageBuilder}.java` (how the response is consumed)

As of writing:

```java
// Snapshot.java — @Document("snapshots")
String id;                  // @Id
JobBoard jobBoard;
LocalDateTime fetchTime;
byte[] gzippedHtml;
String parserVersion;       // null until you set it
ParsingStatus parsingStatus; // null until you set it

// JobBoard.java
enum JobBoard { DOU("https://jobs.dou.ua/vacancies/?category=Java") }

// ParsingStatus.java
enum ParsingStatus { OK, ERROR }

// Vacancy.java
record Vacancy(String title, String location, String publishDate, String companyName, String url)
// NewVacancyMessageBuilder.java now deserializes the success body as Vacancy[] — a JSON array

// FastApiError.java
record FastApiError(Integer code, String snapshotId, String message, String parserVersion, String timestamp)
```

### How Mongo actually stores this (Spring Data Mongo defaults, no custom converters are configured anywhere in `core`)

- Collection name: `snapshots`.
- `id` → stored as `_id`. **It is a plain string** (`UUID.randomUUID().toString()`), not a 24-hex-char ObjectId, so Spring Data Mongo leaves it as a raw BSON string rather than converting it to `ObjectId`. **Query with `{"_id": snapshot_id}` as a plain string — do NOT wrap it in `ObjectId(...)`.** This is the single easiest thing to get wrong here.
- `jobBoard` → stored as the enum's plain name, e.g. `"DOU"` (a string).
- `gzippedHtml` → stored as BSON binary. Motor/PyMongo will hand it back as `bson.Binary`, which is a `bytes` subclass — you can pass it straight into `gzip.decompress(...)`.
- `parserVersion` → plain string field, currently absent/`null`.
- `parsingStatus` → will be written back as the plain enum name string, i.e. `"OK"` or `"ERROR"` (matching how `jobBoard` is stored) — **not** a nested object.
- `fetchTime` → irrelevant to you, don't touch it.

## Cross-language JSON contract

Jackson (Java) serializes the two DTOs using their record component names as-is — no naming strategy, no annotations. So your Pydantic models' JSON keys must match **exactly**, including case:

```json
// success — HTTP 200, a JSON array, even if it contains exactly one item or (rarely) zero
[
  {
    "title": "string",
    "location": "string",
    "publishDate": "string",
    "companyName": "string",
    "url": "string"
  }
]
```

```json
// FastApiError — any non-2xx
{
  "code": 404,
  "snapshotId": "string",
  "message": "string",
  "parserVersion": "string or null",
  "timestamp": "string"
}
```

Simplest way to guarantee this: declare the Pydantic model fields in camelCase directly (not idiomatic Python, but it removes an entire class of alias/serialization bugs):

```python
# app/models.py
from pydantic import BaseModel

class Vacancy(BaseModel):
    title: str
    location: str
    publishDate: str
    companyName: str
    url: str

class FastApiError(BaseModel):
    code: int
    snapshotId: str
    message: str
    parserVersion: str | None
    timestamp: str
```

`timestamp` should be an ISO-8601 string, e.g. `datetime.now(timezone.utc).isoformat()`. The `/parse/{snapshot_id}` route itself returns `list[Vacancy]` on success (FastAPI serializes a Python list of these models straight into a JSON array, nothing special needed) and a single `FastApiError` on failure — don't wrap the success list in an envelope object like `{"vacancies": [...]}`, Java expects a bare array.

### Status code matters, not just the body

Look at `MessageBuilderFactory.create()`: it picks the `Vacancy[]` deserializer for any `2xx` response and the `FastApiError` deserializer for anything else — it does not look at the body shape to decide. So:

- Success → HTTP `200` + a JSON array of `VacancyDto` objects.
- Any failure → a non-2xx status + a single `FastApiError` JSON object, and the `code` field in the body should mirror the actual HTTP status you returned (it gets printed directly in the Telegram error message).

Keep the status codes simple, two tiers is enough since Java only branches on 2xx vs. not:
- `404` — snapshot id not found in Mongo.
- `500` — everything else (unsupported `jobBoard`, corrupt gzip, parser couldn't find what it needed, unexpected exception).

### Never break the contract

Because Java trusts the body shape completely (it deserializes straight into `VacancyDto`/`FastApiError` with no fallback), **any unhandled exception must still come back as a valid `FastApiError` JSON**, not FastAPI's default `{"detail": "Internal Server Error"}` — that shape doesn't match `FastApiError`'s fields and will make Jackson throw on the Java side, crashing the whole notify flow.

Register a catch-all exception handler in `main.py`:

```python
@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled exception while handling %s", request.url)
    body = FastApiError(
        code=500,
        snapshotId=request.path_params.get("snapshot_id", "unknown"),
        message=str(exc),
        parserVersion=None,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )
    return JSONResponse(status_code=500, content=body.model_dump())
```

This is a real correctness requirement, not defensive over-engineering — it's the only thing standing between a bug in your code and a crash in `core`.

## Endpoint spec

### `GET /health` — already implemented, leave it as-is (or trivially extend it, e.g. ping Mongo — optional, not required).

### `GET /parse/{snapshot_id}`

`snapshot_id` is a plain path string — don't add `UUID` type validation, just accept whatever string comes in and match it against Mongo verbatim.

Suggested flow (adapt as needed, but keep the shape — one file for the route is fine at this size, don't split into a `service` layer + `repository` layer + `controller` layer for one endpoint):

```python
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
        vacancies = parse_fn(html)
        logger.info("[%s] parsed %d vacancies (first: %r @ %r)", snapshot_id, len(vacancies), vacancies[0].title, vacancies[0].companyName)
    except Exception as e:
        logger.exception("[%s] parsing failed", snapshot_id)
        await set_parsing_result(snapshot_id, parser_version, "ERROR")
        return error_response(500, snapshot_id, str(e), parser_version)

    await set_parsing_result(snapshot_id, parser_version, "OK")
    logger.info("[%s] done, parsingStatus=OK, %d vacancies returned", snapshot_id, len(vacancies))
    return vacancies
```

with a small helper:

```python
async def set_parsing_result(snapshot_id: str, parser_version: str | None, status: str) -> None:
    await database["snapshots"].update_one(
        {"_id": snapshot_id},
        {"$set": {"parserVersion": parser_version, "parsingStatus": status}},
    )
```

`error_response` just builds and returns a `JSONResponse(status_code=..., content=FastApiError(...).model_dump())`.

## The DOU parser — verified against the live site today

`JobBoard.DOU.url` is `https://jobs.dou.ua/vacancies/?category=Java` — a **listing page** with ~20 vacancyDto cards. Extract **all of them**, in the order they appear on the page. Concretely, verified by fetching the raw (non-JS-rendered) page — the listing is fully server-rendered, a plain HTTP GET (like Java's `RestClient` does) gets the complete list:

- Each vacancyDto is `<li class="l-vacancyDto">` (or `<li class="l-vacancyDto __hot">` for paid/promoted listings). **Include `__hot` cards too — return every `<li class="l-vacancyDto">` on the page, hot or not.** (An earlier version of this spec said to skip `__hot` cards on the assumption the parser would only ever return one vacancyDto; that's no longer the case — `core` now accepts a list and will handle de-duplication/filtering of already-seen vacancies on its side later. Don't try to filter or de-duplicate anything here, just report what's on the page.)
- For every such `<li>`:
  - Title + URL: `li.select_one(".title a.vt")` → text = title, `href` attribute = url.
  - Company: `li.select_one(".title .company")` → text (strip whitespace/`&nbsp;`; Python's `.strip()` already handles the NBSP character correctly).
  - Publish date: `li.select_one(".date")` → text, e.g. `"14 серпня"` (day + Ukrainian month name, no year). **Do not try to parse this into a real date** — `Vacancy.publishDate` is a plain `String` on the Java side too, so just pass the raw scraped text through as-is.
  - Location: `li.select_one(".cities")` → text, e.g. `"віддалено"` (remote) or a city name. If absent, default to `""` rather than failing — it's the least critical field.
- Treat a card missing title/url/company/date as a hard parsing error for the whole request (raise `ParsingError`) rather than silently skipping it — a broken card usually means DOU changed its markup and you'd rather find out than silently under-report. A missing location on an otherwise-fine card is not fatal.
- If **zero** `<li class="l-vacancyDto">` elements are found at all, that's also a parsing error — don't return an empty array as if that were a normal "no vacancies right now" result. In practice an active category page always has listings, so an empty match set almost certainly means the selector broke (site markup changed), not that DOU is genuinely empty. This also means a successful parse is guaranteed to return at least one item, so the route handler can safely assume `vacancies[0]` exists.
- Charset: the page declares `utf-8`; decode the decompressed bytes as `utf-8`.

Example verified card markup (for reference, don't hardcode this exact HTML anywhere, it's just illustrating the selectors):

```html
<li class="l-vacancyDto">
  <div class="date">14 серпня</div>
  <div class="title">
    <a class="vt" href="https://jobs.dou.ua/companies/mirko/vacancies/369860/">Middle Fullstack Developer (Node.js + React)</a>
    <strong>в <a class="company" href="...">Mirko</a></strong>
    <span class="salary">$500–3000</span>
    <span class="cities bi bi-geo-alt-fill"> за кордоном</span>
  </div>
  <div class="sh-info">...</div>
</li>
```

Suggested layout — one small module per job board plus a tiny registry, so adding a second board later means adding one file + one dict entry, nothing more:

```
app/parsers/
  __init__.py
  errors.py     # class ParsingError(Exception): pass
  registry.py   # PARSERS = {"DOU": (dou.parse, dou.VERSION)}
  dou.py        # VERSION = "dou-v1"; def parse(html: str) -> list[Vacancy]: ...
```

`parse(html)` should raise `ParsingError` (or let a natural exception like `AttributeError` bubble — either is fine, the route handler catches `Exception` broadly) when it can't find what it needs; the route handler is responsible for turning that into the `ERROR` status + `FastApiError` response, not the parser itself.

## Tech stack

Already scaffolded correctly (`parser/requirements.txt`, `parser/app/{config.py,db.py,main.py}`, `parser/Dockerfile`) — FastAPI + Uvicorn + Motor (async Mongo driver) + pydantic-settings, Python 3.12, no MySQL/Redis anywhere. You need to add:

- `beautifulsoup4` and `lxml` to `requirements.txt`, for HTML parsing (`BeautifulSoup(html, "lxml")`).

No new environment variables or settings should be needed — this service only talks to Mongo (already configured) and does local computation, no outbound HTTP calls, no new secrets.

## Logging

Set this up once in `main.py` (`logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")` or similar) — no need for structured/JSON logging or a logging framework, plain readable lines to stdout are what an agent debugging this via `docker logs` needs. Every request should log, at minimum: request received (with snapshot_id), snapshot found/not found, resolved job board + parser version, decompressed HTML size, parse outcome (success with a short summary of what was extracted, or failure), and the Mongo write. Use `logger.exception(...)` (not just `logger.error(...)`) wherever you catch an exception you're about to turn into an error response, so the full traceback lands in the console — that's the whole point of #9 above.

## Explicitly out of scope — do not do these

- Do not touch anything under `core/`.
- Do not add endpoints beyond `/health` and `/parse/{snapshot_id}`.
- Do not add auth, rate limiting, retries/backoff, caching, or a message queue.
- Do not filter, de-duplicate, or rank the vacancies you find (by date, by hot/non-hot, or anything else) — return every card as-is, in page order, and let `core` decide later what's new. Don't change the `VacancyDto`/`FastApiError` field shapes.
- Do not add MySQL/Redis or any other datastore.
- Don't write a test framework/suite unless it's trivial to add — manual verification (below) is enough for this.

## Verify it works

1. `docker compose up -d mongodb` (uses the repo's `.env` for credentials).
2. Insert a real test `Snapshot` document: fetch `https://jobs.dou.ua/vacancies/?category=Java` with `curl`, gzip it, and insert a document with that as `gzippedHtml`, `jobBoard: "DOU"`, `parserVersion: null`, `parsingStatus: null`, and some test `_id` string.
3. Run the app locally (`uvicorn app.main:app --reload`, pointing `MONGO_HOST` etc. at localhost) or via `docker compose up parser`.
4. `curl http://localhost:8000/health` → `{"status": "ok"}`.
5. `curl -i http://localhost:8000/parse/<your-test-id>` → expect `200` + a JSON array (roughly ~20 items, matching the number of `<li class="l-vacancyDto">` on the real page, hot cards included) of `VacancyDto` objects that match the real, current DOU listing; then check the Mongo doc got `parsingStatus: "OK"` and `parserVersion: "dou-v1"`.
6. `curl -i http://localhost:8000/parse/does-not-exist` → expect `404` + a `FastApiError` JSON.
7. Corrupt the `gzippedHtml` on a test doc (or point `jobBoard` at a value with no registered parser) and re-parse → expect `500` + `FastApiError`, and confirm `parsingStatus: "ERROR"` was actually written to Mongo.
8. Read the console output for all of the above and confirm it would actually help someone debug a failure without needing to attach a debugger.
