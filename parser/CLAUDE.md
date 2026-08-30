# CLAUDE.md (parser)

## Preferences
- Simple is better — no over-engineering, no shared abstractions until actually needed
- Any change to a parser (`app/parsers/<name>.py`) must bump its `VERSION` string
- Before creating a new parser, read the existing ones in `app/parsers/` to reuse the same structure/helpers instead of inventing a new pattern

## What to know about this module
- Each parser lives in `app/parsers/<name>.py` and exposes:
  - `VERSION: str` — bumped on every change to the parser
  - `parse(html: str, job_board: str) -> list[Vacancy]`
- New parsers must be registered in `app/parsers/registry.py`, mapping job-board keys to `(parse_fn, VERSION)`. Keys must match the `JobBoard` enum values defined in `core` (`core/src/main/java/dev/arsonist/vacanciesmonitoring/model/JobBoard.java`) — core sends these keys as-is.
- `Vacancy` (`app/models.py`) fields: `jobBoard`, `title`, `location`, `publishTime`, `companyName`, `url`.
- Raise `ParsingError` (`app/parsers/errors.py`) when required fields are missing from a card — never silently skip or fabricate data. `location` is the one field allowed to default to `""` if absent.
- Sample fixtures live in `resources/examples/<provider>/<provider>-before-parsing.html` + `-after-parsing.json`. When adding/changing a parser, regenerate the `-after-parsing.json` by actually running the parser against the `-before-parsing.html`, don't hand-write it.
