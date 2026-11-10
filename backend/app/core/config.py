from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "APIO API"
    environment: str = "development"

    database_url: str = "postgresql://apio:apio@localhost:5432/apio"
    graphhopper_url: str = "http://localhost:8989"
    tomtom_api_key: str = ""

    # IA (Google Gemini, free tier de Google AI Studio)
    gemini_api_key: str = ""
    gemini_model: str = "gemini-flash-lite-latest"

    cors_origins: list[str] = ["http://localhost:5173"]

    class Config:
        env_file = ".env"


settings = Settings()
