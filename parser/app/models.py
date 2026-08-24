from pydantic import BaseModel


class Vacancy(BaseModel):
    jobBoard: str
    title: str
    location: str
    publishTime: str
    companyName: str
    url: str


class FastApiError(BaseModel):
    code: int
    snapshotId: str
    message: str
    parserVersion: str | None
    timestamp: str
