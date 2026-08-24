from app.config import settings
from motor.motor_asyncio import AsyncIOMotorClient

client = AsyncIOMotorClient(
    host=settings.mongo_host,
    port=settings.mongo_port,
    username=settings.mongo_app_username,
    password=settings.mongo_app_password,
    authSource=settings.mongo_initdb_database,
)
database = client[settings.mongo_initdb_database]
