from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env")

    mongo_host: str = "mongodb"
    mongo_port: int = 27017
    mongo_initdb_root_username: str
    mongo_initdb_root_password: str
    mongo_initdb_database: str


settings = Settings()
