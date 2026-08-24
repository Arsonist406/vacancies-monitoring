# CLAUDE.md

## Preferences
### General
- Keep code simple — no over-engineering
- After ANY correction from the user: update list of **Preferences** in CLAUDE.md with a specific and actionable rule to prevent the same mistake. If the file exceeds 150 lines, prune stale/obvious rules before adding new ones
- You are strictly prohibited from executing and suggesting any Git commands that modify, create, or alter the repository history, branches, or state in any form

### Workflow
- When something goes sideways, stop and re-plan — don't keep pushing
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- Check in with the user before starting implementation

## Project overview
Vacancies-monitoring is a multi-module Spring Boot core + Fast API parser system that fetches html with 
vacancies from external job-boards, parses and sends it over Telegram API to single user.

## Stack
- Language: Java 21, Python 3.12
- Frameworks: Spring Boot 4.0.1, FastAPI
- Databases: MongoDB
- Building tools: Maven, pip

## Module map
- `core` — main logic, includes: setting up cron jobs, fetching html with vacancies, persisting data to mongo, sending 
parse request to Fast API, sending resieved parsed data to user over Telegram API
- `parser` — Fast API based app that only purpose is to parse html from mongo
