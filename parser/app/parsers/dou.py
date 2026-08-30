import re

from app.models import Vacancy
from app.parsers.errors import ParsingError
from bs4 import BeautifulSoup

VERSION = "dou-v2"


def _clean_text(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def parse(html: str, job_board: str) -> list[Vacancy]:
    soup = BeautifulSoup(html, "lxml")
    cards = soup.select("li.l-vacancy")
    if not cards:
        raise ParsingError("No 'li.l-vacancy' elements found on the page")

    vacancies: list[Vacancy] = []
    for card in cards:
        title_el = card.select_one(".title a.vt")
        company_el = card.select_one(".title .company")
        date_el = card.select_one(".date")
        if title_el is None or not title_el.get("href") or company_el is None or date_el is None:
            raise ParsingError(f"Vacancy card missing required field(s): {card}")

        location_el = card.select_one(".cities")
        location = _clean_text(location_el.get_text()) if location_el else ""

        vacancies.append(
            Vacancy(
                jobBoard=job_board,
                title=_clean_text(title_el.get_text()),
                location=location,
                publishTime=_clean_text(date_el.get_text()),
                companyName=_clean_text(company_el.get_text()),
                url=title_el["href"],
            )
        )

    return vacancies
