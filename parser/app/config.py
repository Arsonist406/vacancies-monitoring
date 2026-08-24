from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(case_sensitive=False)

    mongo_host: str
    mongo_port: int
    mongo_app_username: str
    mongo_app_password: str
    mongo_initdb_database: str


settings = Settings()