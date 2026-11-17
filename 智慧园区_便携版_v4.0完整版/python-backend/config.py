from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    APP_NAME: str = "建筑能耗数字孪生管理系统"
    VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    DATABASE_URL: str = "sqlite:///./energy_management.db"
    
    JWT_SECRET: str = "energy-twin-secret-key-2024-python-version"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION: int = 86400
    
    class Config:
        env_file = ".env"

settings = Settings()
