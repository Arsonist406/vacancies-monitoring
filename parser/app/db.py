from app.config import settings
from motor.motor_asyncio import AsyncIOMotorClient

client = AsyncIOMotorClient(
    host=settings.mongo_host,
    port=settings.mongo_port,
    username=settings.mongo_initdb_root_username,
    password=settings.mongo_initdb_root_password,
    authSource="admin",
)
database = client[settings.mongo_initdb_database]
