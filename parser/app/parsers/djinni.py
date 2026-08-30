import re

from app.models import Vacancy
from app.parsers.errors import ParsingError
from bs4 import BeautifulSoup

VERSION = "djinni-v1"

BASE_URL = "https://djinni.co"
_DATE_RE = re.compile(r"^\d{2}:\d{2} \d{2}\.\d{2}\.\d{4}$")


def _clean_text(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def parse(html: str, job_board: str) -> list[Vacancy]:
    soup = BeautifulSoup(html, "lxml")
    cards = soup.select("div.job-item")
    if not cards:
        raise ParsingError("No 'div.job-item' elements found on the page")

    vacancies: list[Vacancy] = []
    for card in cards:
        title_el = card.select_one(".job-item__position")
        link_el = card.select_one("a.job_item__header-link")
        company_el = card.select_one(".text-gray-800")
        date_el = card.find("span", attrs={"title": _DATE_RE})
        if title_el is None or link_el is None or not link_el.get("href") or company_el is None or date_el is None:
            raise ParsingError(f"Vacancy card missing required field(s): {card}")

        location_el = card.select_one(".location-text")
        location = _clean_text(location_el.get_text()) if location_el else ""

        vacancies.append(
            Vacancy(
                jobBoard=job_board,
                title=_clean_text(title_el.get_text()),
                location=location,
                publishTime=date_el["title"],
                companyName=_clean_text(company_el.get_text()),
                url=BASE_URL + link_el["href"],
            )
        )

    return vacancies
