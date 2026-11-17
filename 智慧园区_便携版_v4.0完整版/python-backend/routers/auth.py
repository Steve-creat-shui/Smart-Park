from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from schemas.auth import SendCodeRequest, RegisterRequest, LoginRequest, AuthResponse, ApiResponse
from services.auth_service import send_code, register_user, login_user

router = APIRouter(prefix="/api/auth", tags=["认证"])

@router.post("/send-code", response_model=ApiResponse)
async def send_verification_code(req: SendCodeRequest, db: Session = Depends(get_db)):
    send_code(req.phone)
    return ApiResponse(message="验证码已发送，请查看后端控制台", data={"message": "验证码已发送"})

@router.post("/register", response_model=ApiResponse)
async def register(req: RegisterRequest, db: Session = Depends(get_db)):
    try:
        user = register_user(db, req.phone, req.password, req.nickname)
        return ApiResponse(data={
            "user_id": user.id,
            "phone": user.phone,
            "nickname": user.nickname,
            "role": user.role
        })
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/login", response_model=ApiResponse)
async def login(req: LoginRequest, db: Session = Depends(get_db)):
    try:
        result = login_user(db, req.phone, req.password)
        return ApiResponse(data=result)
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))
