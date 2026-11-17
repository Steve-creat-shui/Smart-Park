from datetime import datetime, timedelta
from typing import Optional
from jose import jwt
from passlib.context import CryptContext
from config import settings
from sqlalchemy.orm import Session
from models.user import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(seconds=settings.JWT_EXPIRATION))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)

def decode_token(token: str) -> dict:
    return jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])

def authenticate_user(db: Session, phone: str, password: str) -> Optional[User]:
    user = db.query(User).filter(User.phone == phone).first()
    if not user or not verify_password(password, user.password):
        return None
    return user

def send_code(phone: str) -> str:
    code = "123456"
    print(f"验证码已发送到 {phone}: {code}")
    return code

def register_user(db: Session, phone: str, password: str, nickname: Optional[str] = None) -> User:
    existing_user = db.query(User).filter(User.phone == phone).first()
    if existing_user:
        raise ValueError("该手机号已注册")
    
    user = User(
        phone=phone,
        password=get_password_hash(password),
        nickname=nickname or f"用户{phone[-4:]}",
        role="user"
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

def login_user(db: Session, phone: str, password: str) -> dict:
    user = authenticate_user(db, phone, password)
    if not user:
        raise ValueError("手机号或密码错误")
    
    token_data = {"sub": str(user.id), "phone": user.phone, "role": user.role}
    token = create_access_token(token_data)
    
    user.last_login_at = datetime.utcnow()
    db.commit()
    
    return {
        "token": token,
        "user_id": user.id,
        "phone": user.phone,
        "nickname": user.nickname,
        "role": user.role
    }
