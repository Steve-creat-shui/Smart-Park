from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class SendCodeRequest(BaseModel):
    phone: str = Field(..., min_length=11, max_length=11)

class RegisterRequest(BaseModel):
    phone: str = Field(..., min_length=11, max_length=11)
    password: str = Field(..., min_length=6)
    code: str = Field(..., min_length=6, max_length=6)
    nickname: Optional[str] = None

class LoginRequest(BaseModel):
    phone: str = Field(..., min_length=11, max_length=11)
    password: str

class AuthResponse(BaseModel):
    token: str
    user_id: int
    phone: str
    nickname: Optional[str] = None
    role: str = "user"

class ApiResponse(BaseModel):
    success: bool = True
    message: str = "操作成功"
    data: Optional[dict] = None
