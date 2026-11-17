"""
Python FastAPI backend - drop-in replacement for Java Spring Boot backend
Matches the Java API response format: {code, message, data}
Uses SQLite instead of MySQL for zero-config setup
"""
import os
import random
import hashlib
import time
import jwt as pyjwt
from datetime import datetime, timedelta
from typing import Optional, List
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Depends, Request, UploadFile, File, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from passlib.context import CryptContext

from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, Text, Boolean, BigInteger, func
from sqlalchemy.orm import declarative_base, sessionmaker, Session

# ─── Database ───
DB_PATH = os.path.join(os.path.dirname(__file__), "app.db")
DATABASE_URL = f"sqlite:///{DB_PATH}"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ─── Models ───
class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, autoincrement=True)
    phone = Column(String(11), unique=True, nullable=False, index=True)
    password = Column(String(255), nullable=False)
    nickname = Column(String(50))
    real_name = Column(String(50))
    role = Column(String(20), default="user")
    user_type = Column(String(20), default="normal")
    status = Column(String(20), default="active")
    department = Column(String(100))
    employee_id = Column(String(50))
    position = Column(String(50))
    engineer_cert = Column(String(100))
    company_email = Column(String(100))
    id_card_last4 = Column(String(4))
    remark = Column(Text)
    reject_reason = Column(String(255))
    current_enterprise_id = Column(Integer)
    created_at = Column(DateTime, default=datetime.now)
    last_login_at = Column(DateTime)

class Building(Base):
    __tablename__ = "buildings"
    id = Column(Integer, primary_key=True, autoincrement=True)
    building_id = Column(String(20), unique=True, nullable=False, index=True)
    building_name = Column(String(100), nullable=False)
    building_type = Column(String(50))
    floor_count = Column(Integer, default=1)
    total_area_m2 = Column(Float, default=0)
    location = Column(String(100))
    construction_year = Column(Integer)
    energy_rating = Column(String(10))
    ac_system_type = Column(String(50))

class EnergyConsumption(Base):
    __tablename__ = "energy_consumption"
    id = Column(Integer, primary_key=True, autoincrement=True)
    building_id = Column(String(20), nullable=False, index=True)
    building_type = Column(String(50))
    enterprise_id = Column(Integer)
    timestamp = Column(DateTime, nullable=False)
    power_consumption_kwh = Column(Float, default=0)
    water_consumption_m3 = Column(Float, default=0)
    carbon_emission_kg = Column(Float, default=0)
    ac_consumption_kwh = Column(Float, default=0)
    lighting_consumption_kwh = Column(Float, default=0)
    other_consumption_kwh = Column(Float, default=0)
    temperature_celsius = Column(Float)
    humidity_percent = Column(Float)
    occupancy_rate = Column(Float)
    status = Column(String(20), default="normal")

class Enterprise(Base):
    __tablename__ = "enterprises"
    id = Column(Integer, primary_key=True, autoincrement=True)
    enterprise_name = Column(String(100))
    owner_id = Column(Integer)
    status = Column(String(20), default="active")
    created_at = Column(DateTime, default=datetime.now)

class Alert(Base):
    __tablename__ = "alerts"
    id = Column(Integer, primary_key=True, autoincrement=True)
    building_id = Column(String(20))
    alert_type = Column(String(50))
    level = Column(String(20), default="warning")
    message = Column(Text)
    status = Column(String(20), default="active")
    created_at = Column(DateTime, default=datetime.now)
    acknowledged_at = Column(DateTime)
    resolved_at = Column(DateTime)

class WorkOrder(Base):
    __tablename__ = "work_orders"
    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(200))
    description = Column(Text)
    building_id = Column(String(20))
    priority = Column(String(20), default="medium")
    status = Column(String(20), default="pending")
    assigned_to = Column(String(50))
    created_at = Column(DateTime, default=datetime.now)
    started_at = Column(DateTime)
    completed_at = Column(DateTime)
    resolution_notes = Column(Text)

# ─── Security ───
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
JWT_SECRET = "energy-twin-secret-key-2024-competition-very-long-string"
JWT_ALGORITHM = "HS256"
JWT_EXPIRATION = 86400000  # ms

verification_codes = {}

def create_token(phone, nickname, role, user_type="normal", enterprise_id=None, user_id=None):
    payload = {
        "sub": phone,
        "nickname": nickname,
        "role": role,
        "userType": user_type,
        "enterpriseId": enterprise_id,
        "userId": user_id,
        "exp": datetime.utcnow() + timedelta(seconds=JWT_EXPIRATION // 1000)
    }
    return pyjwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)

# ─── Schemas ───
class ApiResp(BaseModel):
    code: int = 200
    message: str = "success"
    data: Optional[dict | list | str] = None

def ok(data=None, message="success"):
    return {"code": 200, "message": message, "data": data}

def err(code=500, message="error"):
    return {"code": code, "message": message, "data": None}

# ─── Startup ───
@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        init_data(db)
    finally:
        db.close()
    print("=" * 50)
    print("  Python FastAPI Backend started on port 8080")
    print("  API docs: http://localhost:8080/docs")
    print("=" * 50)
    yield

def init_data(db: Session):
    if db.query(User).count() == 0:
        users = [
            User(phone="13800138000", password=pwd_context.hash("123456"),
                 nickname="管理员", real_name="管理员", role="admin", user_type="platform_admin", status="active"),
            User(phone="13900139000", password=pwd_context.hash("123456"),
                 nickname="企业用户", real_name="企业用户", role="enterprise", user_type="enterprise", status="active",
                 current_enterprise_id=1),
            User(phone="13811111111", password=pwd_context.hash("123456"),
                 nickname="测试用户", real_name="测试用户", role="user", user_type="normal", status="active"),
        ]
        db.add_all(users)
        db.commit()
        print("[OK] Default users initialized")
        print("   Admin: 13800138000 / 123456")
        print("   Enterprise: 13900139000 / 123456")
        print("   User: 13811111111 / 123456")

    if db.query(Enterprise).count() == 0:
        ent = Enterprise(enterprise_name="鼎筑科技有限公司", owner_id=2, status="active")
        db.add(ent)
        db.commit()
        print("[OK] Default enterprise initialized")

    if db.query(Building).count() == 0:
        buildings = [
            Building(building_id="B001", building_name="行政楼", building_type="行政楼", floor_count=8, total_area_m2=10000, location="园区中心", construction_year=2010, energy_rating="A", ac_system_type="中央空调"),
            Building(building_id="B002", building_name="图书馆", building_type="图书馆", floor_count=5, total_area_m2=8000, location="园区东区", construction_year=2012, energy_rating="A", ac_system_type="变频中央空调"),
            Building(building_id="B003", building_name="实验楼", building_type="实验楼", floor_count=6, total_area_m2=9500, location="园区西区", construction_year=2015, energy_rating="B", ac_system_type="变频中央空调"),
            Building(building_id="B004", building_name="教学楼", building_type="教学楼", floor_count=10, total_area_m2=15000, location="园区中心", construction_year=2008, energy_rating="B", ac_system_type="中央空调"),
            Building(building_id="B005", building_name="宿舍楼", building_type="宿舍楼", floor_count=12, total_area_m2=20000, location="园区北区", construction_year=2009, energy_rating="C", ac_system_type="分体空调"),
            Building(building_id="B006", building_name="餐厅", building_type="餐厅", floor_count=3, total_area_m2=4000, location="园区南区", construction_year=2011, energy_rating="B", ac_system_type="分体空调"),
            Building(building_id="B007", building_name="室内体育馆", building_type="体育馆", floor_count=2, total_area_m2=12000, location="园区东南区", construction_year=2018, energy_rating="A", ac_system_type="中央空调"),
        ]
        db.add_all(buildings)
        db.commit()
        print(f"[OK] Buildings initialized: {len(buildings)}")

    if db.query(EnergyConsumption).count() == 0:
        now = datetime.now()
        building_ids = ["B001", "B002", "B003", "B004", "B005", "B006", "B007"]
        building_types = ["行政楼", "图书馆", "实验楼", "教学楼", "宿舍楼", "餐厅", "体育馆"]
        records = []
        for i, (bid, btype) in enumerate(zip(building_ids, building_types)):
            base_power = 100 + i * 20
            for day in range(30):
                for hour in range(0, 24, 6):
                    ts = now - timedelta(days=day, hours=hour)
                    ts = ts.replace(minute=0, second=0, microsecond=0)
                    power = base_power + random.random() * 50
                    records.append(EnergyConsumption(
                        building_id=bid, building_type=btype, enterprise_id=1,
                        timestamp=ts,
                        power_consumption_kwh=round(power, 2),
                        water_consumption_m3=round(power * 0.1, 2),
                        carbon_emission_kg=round(power * 0.785, 2),
                        ac_consumption_kwh=round(power * 0.4, 2),
                        lighting_consumption_kwh=round(power * 0.2, 2),
                        other_consumption_kwh=round(power * 0.4, 2),
                        temperature_celsius=round(20 + random.random() * 5, 1),
                        humidity_percent=round(40 + random.random() * 30, 1),
                        occupancy_rate=round(0.3 + random.random() * 0.5, 2),
                        status="warning" if random.random() < 0.1 else "normal"
                    ))
        db.add_all(records)
        db.commit()
        print(f"[OK] Energy records initialized: {len(records)}")

    if db.query(Alert).count() == 0:
        alerts = []
        for bid in ["B001", "B002", "B003"]:
            for j in range(3):
                alerts.append(Alert(
                    building_id=bid, alert_type="能耗异常",
                    level=["warning", "critical", "info"][j % 3],
                    message=f"{bid} 建筑能耗超出阈值 {10+j*5}%",
                    status=["active", "acknowledged", "resolved"][j % 3],
                    created_at=datetime.now() - timedelta(hours=j * 12)
                ))
        db.add_all(alerts)
        db.commit()
        print(f"[OK] Alerts initialized")

    if db.query(WorkOrder).count() == 0:
        orders = []
        for i, bid in enumerate(["B001", "B002", "B004"]):
            orders.append(WorkOrder(
                title=f"{bid} 空调系统维护", description=f"定期维护检查",
                building_id=bid, priority=["high", "medium", "low"][i % 3],
                status=["pending", "in_progress", "completed"][i % 3],
                assigned_to="维护工程师", created_at=datetime.now() - timedelta(days=i)
            ))
        db.add_all(orders)
        db.commit()
        print(f"[OK] Work orders initialized")

# ─── App ───
app = FastAPI(title="建筑能耗数字孪生管理系统", version="1.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

# Global exception handler to match Java format
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content=err(exc.status_code, exc.detail))

@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    return JSONResponse(status_code=500, content=err(500, str(exc)))

# ═══════════════════════════════════════════════
# AUTH ENDPOINTS
# ═══════════════════════════════════════════════
class LoginReq(BaseModel):
    phone: str
    password: str

class RegisterReq(BaseModel):
    phone: str
    password: str
    code: str = ""
    nickname: Optional[str] = None
    requestedRole: Optional[str] = "user"
    realName: Optional[str] = None
    department: Optional[str] = None
    employeeId: Optional[str] = None
    position: Optional[str] = None
    engineerCert: Optional[str] = None
    companyEmail: Optional[str] = None
    idCardLast4: Optional[str] = None
    remark: Optional[str] = None

class SendCodeReq(BaseModel):
    phone: str

@app.post("/api/auth/send-code")
async def send_code(req: SendCodeReq):
    code = f"{random.randint(100000, 999999)}"
    verification_codes[req.phone] = {"code": code, "time": time.time()}
    print(f"[SMS] Code sent to {req.phone}: {code}")
    return ok({"message": "验证码已发送，请查看后端控制台"})

@app.post("/api/auth/register")
async def register(req: RegisterReq, db: Session = Depends(get_db)):
    if db.query(User).filter(User.phone == req.phone).first():
        return err(400, "该手机号已注册")
    role = "enterprise" if req.requestedRole == "enterprise" else "user"
    user = User(
        phone=req.phone, password=pwd_context.hash(req.password),
        nickname=req.nickname or req.realName or f"用户{req.phone[-4:]}",
        real_name=req.realName, role=role, user_type="normal",
        status="pending", department=req.department, employee_id=req.employeeId,
        position=req.position, engineer_cert=req.engineerCert,
        company_email=req.companyEmail, id_card_last4=req.idCardLast4,
        remark=req.remark
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return ok({
        "token": None, "phone": user.phone, "nickname": user.nickname, "role": user.role,
        "realName": user.real_name, "department": user.department, "employeeId": user.employee_id
    })

@app.post("/api/auth/login")
async def login(req: LoginReq, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.phone == req.phone).first()
    if not user:
        return err(401, "手机号未注册")
    if not pwd_context.verify(req.password, user.password):
        return err(401, "密码错误")
    if user.status == "pending":
        return err(403, "PENDING:您的账号正在审核中，请耐心等待管理员审批")
    if user.status == "rejected":
        reason = user.reject_reason or "未说明"
        return err(403, f"REJECTED:您的注册申请已被拒绝，原因：{reason}")
    if user.status == "disabled":
        return err(403, "DISABLED:账号已被禁用，请联系管理员")

    user.last_login_at = datetime.now()
    db.commit()

    eid = user.current_enterprise_id
    token = create_token(user.phone, user.nickname, user.role, user.user_type or "normal", eid, user.id)

    enterprise_name = None
    if eid:
        ent = db.query(Enterprise).filter(Enterprise.id == eid).first()
        if ent:
            enterprise_name = ent.enterprise_name

    has_data = db.query(EnergyConsumption).count() > 0

    return ok({
        "token": token, "phone": user.phone, "nickname": user.nickname, "role": user.role,
        "userType": user.user_type or "normal",
        "currentEnterpriseId": eid, "enterpriseName": enterprise_name,
        "realName": user.real_name, "department": user.department,
        "employeeId": user.employee_id, "hasData": has_data
    })

# ═══════════════════════════════════════════════
# BUILDING ENDPOINTS
# ═══════════════════════════════════════════════
@app.get("/api/buildings")
async def get_buildings(db: Session = Depends(get_db)):
    buildings = db.query(Building).all()
    return ok([{
        "id": b.id, "buildingId": b.building_id, "buildingName": b.building_name,
        "buildingType": b.building_type, "floorCount": b.floor_count,
        "totalAreaM2": b.total_area_m2, "location": b.location,
        "constructionYear": b.construction_year, "energyRating": b.energy_rating,
        "acSystemType": b.ac_system_type
    } for b in buildings])

@app.get("/api/buildings/list")
async def get_building_list(db: Session = Depends(get_db)):
    buildings = db.query(Building).all()
    return ok([{
        "id": b.id, "buildingId": b.building_id, "buildingName": b.building_name,
        "buildingType": b.building_type, "floorCount": b.floor_count,
        "totalAreaM2": b.total_area_m2, "location": b.location,
        "constructionYear": b.construction_year, "energyRating": b.energy_rating,
        "acSystemType": b.ac_system_type
    } for b in buildings])

@app.get("/api/buildings/ids")
async def get_building_ids(db: Session = Depends(get_db)):
    buildings = db.query(Building).all()
    return ok([b.building_id for b in buildings])

@app.get("/api/buildings/{building_id}")
async def get_building(building_id: str, db: Session = Depends(get_db)):
    b = db.query(Building).filter(Building.building_id == building_id).first()
    if not b:
        return err(404, "建筑不存在")
    return ok({
        "id": b.id, "buildingId": b.building_id, "buildingName": b.building_name,
        "buildingType": b.building_type, "floorCount": b.floor_count,
        "totalAreaM2": b.total_area_m2, "location": b.location,
        "constructionYear": b.construction_year, "energyRating": b.energy_rating,
        "acSystemType": b.ac_system_type
    })

@app.post("/api/buildings")
async def add_building(data: dict, db: Session = Depends(get_db)):
    b = Building(
        building_id=data.get("buildingId", f"B{random.randint(100,999)}"),
        building_name=data.get("buildingName", "新建筑"),
        building_type=data.get("buildingType", "其他"),
        floor_count=data.get("floorCount", 1),
        total_area_m2=data.get("totalAreaM2", 0),
        location=data.get("location", ""),
        construction_year=data.get("constructionYear", 2024),
        energy_rating=data.get("energyRating", "B"),
        ac_system_type=data.get("acSystemType", "分体空调")
    )
    db.add(b)
    db.commit()
    return ok({"buildingId": b.building_id, "buildingName": b.building_name})

# ═══════════════════════════════════════════════
# ENERGY CONSUMPTION ENDPOINTS
# ═══════════════════════════════════════════════
def energy_to_dict(e):
    return {
        "id": e.id, "buildingId": e.building_id, "buildingType": e.building_type,
        "timestamp": e.timestamp.strftime("%Y-%m-%d %H:%M:%S") if e.timestamp else None,
        "powerConsumptionKwh": e.power_consumption_kwh,
        "waterConsumptionM3": e.water_consumption_m3,
        "carbonEmissionKg": e.carbon_emission_kg,
        "acConsumptionKwh": e.ac_consumption_kwh,
        "lightingConsumptionKwh": e.lighting_consumption_kwh,
        "otherConsumptionKwh": e.other_consumption_kwh,
        "temperatureCelsius": e.temperature_celsius,
        "humidityPercent": e.humidity_percent,
        "occupancyRate": e.occupancy_rate,
        "status": e.status
    }

@app.get("/api/energy-consumption/{building_id}")
async def get_energy(building_id: str, startDate: str = None, endDate: str = None,
                     page: int = 1, size: int = 20, db: Session = Depends(get_db)):
    q = db.query(EnergyConsumption).filter(EnergyConsumption.building_id == building_id)
    if startDate:
        q = q.filter(EnergyConsumption.timestamp >= startDate)
    if endDate:
        q = q.filter(EnergyConsumption.timestamp <= endDate)
    total = q.count()
    records = q.order_by(EnergyConsumption.timestamp.desc()).offset((page - 1) * size).limit(size).all()
    return ok({
        "content": [energy_to_dict(r) for r in records],
        "totalElements": total, "totalPages": (total + size - 1) // size,
        "number": page, "size": size
    })

@app.get("/api/energy-consumption/{building_id}/latest")
async def get_latest_energy(building_id: str, limit: int = 24, db: Session = Depends(get_db)):
    records = db.query(EnergyConsumption).filter(
        EnergyConsumption.building_id == building_id
    ).order_by(EnergyConsumption.timestamp.desc()).limit(limit).all()
    return ok([energy_to_dict(r) for r in records])

@app.get("/api/energy-consumption/{building_id}/anomalies")
async def get_anomalies(building_id: str, db: Session = Depends(get_db)):
    records = db.query(EnergyConsumption).filter(
        EnergyConsumption.building_id == building_id,
        EnergyConsumption.status == "warning"
    ).order_by(EnergyConsumption.timestamp.desc()).limit(10).all()
    return ok([energy_to_dict(r) for r in records])

@app.get("/api/energy-consumption/{building_id}/monthly-trend")
async def get_monthly_trend(building_id: str, db: Session = Depends(get_db)):
    records = db.query(EnergyConsumption).filter(
        EnergyConsumption.building_id == building_id
    ).order_by(EnergyConsumption.timestamp.desc()).limit(120).all()
    monthly = {}
    for r in records:
        key = r.timestamp.strftime("%Y-%m") if r.timestamp else "unknown"
        if key not in monthly:
            monthly[key] = {"month": key, "totalPower": 0, "totalWater": 0, "totalCarbon": 0, "count": 0}
        monthly[key]["totalPower"] += r.power_consumption_kwh or 0
        monthly[key]["totalWater"] += r.water_consumption_m3 or 0
        monthly[key]["totalCarbon"] += r.carbon_emission_kg or 0
        monthly[key]["count"] += 1
    result = sorted(monthly.values(), key=lambda x: x["month"])
    return ok(result)

@app.get("/api/energy-consumption/{building_id}/available-dates")
async def get_available_dates(building_id: str, db: Session = Depends(get_db)):
    records = db.query(EnergyConsumption.timestamp).filter(
        EnergyConsumption.building_id == building_id
    ).distinct().all()
    dates = sorted(set(r[0].strftime("%Y-%m-%d") for r in records if r[0]))
    return ok(dates)

@app.post("/api/energy-consumption")
async def add_energy(data: dict, db: Session = Depends(get_db)):
    record = EnergyConsumption(
        building_id=data.get("buildingId"),
        building_type=data.get("buildingType"),
        timestamp=datetime.strptime(data.get("timestamp", datetime.now().strftime("%Y-%m-%d %H:%M:%S")), "%Y-%m-%d %H:%M:%S"),
        power_consumption_kwh=data.get("powerConsumptionKwh", 0),
        water_consumption_m3=data.get("waterConsumptionM3", 0),
        carbon_emission_kg=data.get("carbonEmissionKg", 0),
        ac_consumption_kwh=data.get("acConsumptionKwh", 0),
        lighting_consumption_kwh=data.get("lightingConsumptionKwh", 0),
        other_consumption_kwh=data.get("otherConsumptionKwh", 0),
        temperature_celsius=data.get("temperatureCelsius"),
        humidity_percent=data.get("humidityPercent"),
        occupancy_rate=data.get("occupancyRate"),
        status=data.get("status", "normal")
    )
    db.add(record)
    db.commit()
    return ok(energy_to_dict(record))

# ═══════════════════════════════════════════════
# DASHBOARD
# ═══════════════════════════════════════════════
@app.get("/api/dashboard/stats")
async def get_dashboard_stats(db: Session = Depends(get_db)):
    building_count = db.query(Building).count()
    total_power = db.query(func.sum(EnergyConsumption.power_consumption_kwh)).scalar() or 0
    total_water = db.query(func.sum(EnergyConsumption.water_consumption_m3)).scalar() or 0
    total_carbon = db.query(func.sum(EnergyConsumption.carbon_emission_kg)).scalar() or 0
    anomaly_count = db.query(EnergyConsumption).filter(EnergyConsumption.status == "warning").count()
    return ok({
        "buildingCount": building_count,
        "totalPowerConsumption": round(total_power, 2),
        "totalWaterConsumption": round(total_water, 2),
        "totalCarbonEmission": round(total_carbon, 2),
        "anomalyCount": anomaly_count,
        "deviceOnlineRate": 0.95,
        "energySavingRate": 0.12
    })

# ═══════════════════════════════════════════════
# ENTERPRISE ENDPOINTS
# ═══════════════════════════════════════════════
@app.post("/api/enterprise/apply")
async def enterprise_apply(data: dict, db: Session = Depends(get_db)):
    return ok({"message": "申请已提交"})

@app.get("/api/enterprise/my-applications")
async def my_applications():
    return ok([])

@app.get("/api/enterprise/my-enterprises")
async def my_enterprises(db: Session = Depends(get_db)):
    enterprises = db.query(Enterprise).all()
    return ok([{"id": e.id, "enterpriseName": e.enterprise_name, "status": e.status} for e in enterprises])

@app.post("/api/enterprise/switch/{enterprise_id}")
async def switch_enterprise(enterprise_id: int, db: Session = Depends(get_db)):
    ent = db.query(Enterprise).filter(Enterprise.id == enterprise_id).first()
    if not ent:
        return err(404, "企业不存在")
    token = create_token("user", "user", "enterprise", "enterprise", enterprise_id)
    return ok({"token": token, "enterpriseId": enterprise_id, "enterpriseName": ent.enterprise_name})

@app.get("/api/enterprise/info")
async def enterprise_info(db: Session = Depends(get_db)):
    ent = db.query(Enterprise).first()
    if ent:
        return ok({"id": ent.id, "enterpriseName": ent.enterprise_name, "status": ent.status})
    return ok(None)

@app.get("/api/enterprise/members")
async def enterprise_members(db: Session = Depends(get_db)):
    users = db.query(User).filter(User.current_enterprise_id != None).all()
    return ok([{"id": u.id, "phone": u.phone, "nickname": u.nickname, "role": u.role, "realName": u.real_name} for u in users])

@app.post("/api/enterprise/members/add")
async def add_member(data: dict):
    return ok({"message": "成员已添加"})

@app.delete("/api/enterprise/members/{member_id}")
async def remove_member(member_id: int):
    return ok({"message": "成员已移除"})

@app.put("/api/enterprise/members/{member_id}/role")
async def update_member_role(member_id: int, data: dict):
    return ok({"message": "角色已更新"})

@app.get("/api/enterprise/notifications")
async def enterprise_notifications():
    return ok([])

@app.get("/api/enterprise/notifications/unread-count")
async def unread_count():
    return ok({"count": 0})

@app.put("/api/enterprise/notifications/{nid}/read")
async def mark_read(nid: int):
    return ok(None)

@app.put("/api/enterprise/notifications/read-all")
async def mark_all_read():
    return ok(None)

@app.get("/api/enterprise/has-data")
async def has_data(db: Session = Depends(get_db)):
    count = db.query(EnergyConsumption).count()
    return ok({"hasData": count > 0})

# ═══════════════════════════════════════════════
# ADMIN ENDPOINTS
# ═══════════════════════════════════════════════
@app.get("/api/admin/users/pending")
async def pending_users(db: Session = Depends(get_db)):
    users = db.query(User).filter(User.status == "pending").all()
    return ok([{"id": u.id, "phone": u.phone, "nickname": u.nickname, "role": u.role,
                "realName": u.real_name, "department": u.department, "status": u.status,
                "createdAt": u.created_at.strftime("%Y-%m-%d %H:%M:%S") if u.created_at else None} for u in users])

@app.get("/api/admin/users")
async def all_users(page: int = 1, size: int = 20, db: Session = Depends(get_db)):
    total = db.query(User).count()
    users = db.query(User).offset((page - 1) * size).limit(size).all()
    return ok({
        "content": [{"id": u.id, "phone": u.phone, "nickname": u.nickname, "role": u.role,
                      "realName": u.real_name, "department": u.department, "status": u.status,
                      "createdAt": u.created_at.strftime("%Y-%m-%d %H:%M:%S") if u.created_at else None} for u in users],
        "totalElements": total
    })

@app.post("/api/admin/users/{uid}/approve")
async def approve_user(uid: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.status = "active"
        db.commit()
    return ok({"message": "已批准"})

@app.post("/api/admin/users/{uid}/reject")
async def reject_user(uid: int, data: dict = {}, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.status = "rejected"
        user.reject_reason = data.get("reason", "")
        db.commit()
    return ok({"message": "已拒绝"})

@app.post("/api/admin/users/{uid}/disable")
async def disable_user(uid: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.status = "disabled"
        db.commit()
    return ok({"message": "已禁用"})

@app.post("/api/admin/users/{uid}/enable")
async def enable_user(uid: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.status = "active"
        db.commit()
    return ok({"message": "已启用"})

@app.put("/api/admin/users/{uid}/role")
async def update_role(uid: int, data: dict, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.role = data.get("role", user.role)
        db.commit()
    return ok({"message": "角色已更新"})

@app.post("/api/admin/users/{uid}/reset-password")
async def reset_password(uid: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == uid).first()
    if user:
        user.password = pwd_context.hash("123456")
        db.commit()
    return ok({"message": "密码已重置为123456"})

@app.get("/api/admin/stats")
async def admin_stats(db: Session = Depends(get_db)):
    return ok({
        "totalUsers": db.query(User).count(),
        "activeUsers": db.query(User).filter(User.status == "active").count(),
        "pendingUsers": db.query(User).filter(User.status == "pending").count(),
        "totalBuildings": db.query(Building).count(),
        "totalEnterprises": db.query(Enterprise).count()
    })

@app.get("/api/admin/enterprise-applications")
async def enterprise_applications(status: str = None):
    return ok([])

@app.post("/api/admin/enterprise-applications/{aid}/approve")
async def approve_application(aid: int):
    return ok({"message": "已批准"})

@app.post("/api/admin/enterprise-applications/{aid}/reject")
async def reject_application(aid: int, data: dict = {}):
    return ok({"message": "已拒绝"})

@app.get("/api/admin/enterprises")
async def admin_enterprises(db: Session = Depends(get_db)):
    enterprises = db.query(Enterprise).all()
    return ok([{"id": e.id, "enterpriseName": e.enterprise_name, "status": e.status} for e in enterprises])

# ═══════════════════════════════════════════════
# ALERTS
# ═══════════════════════════════════════════════
@app.get("/api/alerts")
async def list_alerts(db: Session = Depends(get_db)):
    alerts = db.query(Alert).order_by(Alert.created_at.desc()).all()
    return ok([{
        "id": a.id, "buildingId": a.building_id, "alertType": a.alert_type,
        "level": a.level, "message": a.message, "status": a.status,
        "createdAt": a.created_at.strftime("%Y-%m-%d %H:%M:%S") if a.created_at else None
    } for a in alerts])

@app.get("/api/alerts/stats")
async def alert_stats(db: Session = Depends(get_db)):
    return ok({
        "total": db.query(Alert).count(),
        "active": db.query(Alert).filter(Alert.status == "active").count(),
        "acknowledged": db.query(Alert).filter(Alert.status == "acknowledged").count(),
        "resolved": db.query(Alert).filter(Alert.status == "resolved").count()
    })

@app.post("/api/alerts")
async def create_alert(data: dict, db: Session = Depends(get_db)):
    a = Alert(building_id=data.get("buildingId"), alert_type=data.get("alertType", "manual"),
              level=data.get("level", "warning"), message=data.get("message", ""))
    db.add(a)
    db.commit()
    return ok({"id": a.id})

@app.put("/api/alerts/{aid}/acknowledge")
async def acknowledge_alert(aid: int, db: Session = Depends(get_db)):
    a = db.query(Alert).filter(Alert.id == aid).first()
    if a:
        a.status = "acknowledged"
        a.acknowledged_at = datetime.now()
        db.commit()
    return ok(None)

@app.put("/api/alerts/{aid}/resolve")
async def resolve_alert(aid: int, db: Session = Depends(get_db)):
    a = db.query(Alert).filter(Alert.id == aid).first()
    if a:
        a.status = "resolved"
        a.resolved_at = datetime.now()
        db.commit()
    return ok(None)

# ═══════════════════════════════════════════════
# WORK ORDERS
# ═══════════════════════════════════════════════
@app.get("/api/work-orders")
async def list_work_orders(db: Session = Depends(get_db)):
    orders = db.query(WorkOrder).order_by(WorkOrder.created_at.desc()).all()
    return ok([{
        "id": o.id, "title": o.title, "description": o.description,
        "buildingId": o.building_id, "priority": o.priority, "status": o.status,
        "assignedTo": o.assigned_to,
        "createdAt": o.created_at.strftime("%Y-%m-%d %H:%M:%S") if o.created_at else None
    } for o in orders])

@app.get("/api/work-orders/stats")
async def work_order_stats(db: Session = Depends(get_db)):
    return ok({
        "total": db.query(WorkOrder).count(),
        "pending": db.query(WorkOrder).filter(WorkOrder.status == "pending").count(),
        "inProgress": db.query(WorkOrder).filter(WorkOrder.status == "in_progress").count(),
        "completed": db.query(WorkOrder).filter(WorkOrder.status == "completed").count()
    })

@app.put("/api/work-orders/{oid}/start")
async def start_order(oid: int, db: Session = Depends(get_db)):
    o = db.query(WorkOrder).filter(WorkOrder.id == oid).first()
    if o:
        o.status = "in_progress"
        o.started_at = datetime.now()
        db.commit()
    return ok(None)

@app.put("/api/work-orders/{oid}/complete")
async def complete_order(oid: int, data: dict = {}, db: Session = Depends(get_db)):
    o = db.query(WorkOrder).filter(WorkOrder.id == oid).first()
    if o:
        o.status = "completed"
        o.completed_at = datetime.now()
        o.resolution_notes = data.get("resolutionNotes", "")
        db.commit()
    return ok(None)

@app.put("/api/work-orders/{oid}/assign")
async def assign_order(oid: int, data: dict, db: Session = Depends(get_db)):
    o = db.query(WorkOrder).filter(WorkOrder.id == oid).first()
    if o:
        o.assigned_to = data.get("assignedTo", "")
        db.commit()
    return ok(None)

@app.get("/api/work-orders/assignable-users")
async def assignable_users(db: Session = Depends(get_db)):
    users = db.query(User).filter(User.status == "active").all()
    return ok([{"id": u.id, "nickname": u.nickname, "phone": u.phone} for u in users])

# ═══════════════════════════════════════════════
# AI / AGENT (stubs)
# ═══════════════════════════════════════════════
@app.post("/api/agent/query")
async def agent_query(data: dict):
    return ok({"answer": "AI分析功能需要配置API密钥后才能使用。", "sources": []})

@app.get("/api/agent/knowledge")
async def agent_knowledge():
    return ok([])

@app.post("/api/agent/knowledge")
async def add_knowledge(data: dict):
    return ok({"id": 1, "message": "已添加"})

@app.put("/api/agent/knowledge/{kid}")
async def update_knowledge(kid: int, data: dict):
    return ok({"message": "已更新"})

@app.delete("/api/agent/knowledge/{kid}")
async def delete_knowledge(kid: int):
    return ok({"message": "已删除"})

@app.post("/api/agent/feedback")
async def save_feedback(data: dict):
    return ok({"message": "反馈已保存"})

@app.get("/api/agent/feedback/all")
async def get_all_feedback():
    return ok([])

@app.delete("/api/agent/feedback/{fid}")
async def delete_feedback(fid: int):
    return ok({"message": "已删除"})

@app.get("/api/agent/stats")
async def agent_stats():
    return ok({"totalQueries": 0, "avgResponseTime": 0, "satisfactionRate": 0})

# ═══════════════════════════════════════════════
# ENERGY ANALYSIS (stubs)
# ═══════════════════════════════════════════════
@app.post("/api/energy-analysis")
async def analyze_energy(data: dict, db: Session = Depends(get_db)):
    bid = data.get("buildingId", "B001")
    records = db.query(EnergyConsumption).filter(EnergyConsumption.building_id == bid).limit(10).all()
    avg_power = sum(r.power_consumption_kwh for r in records) / max(len(records), 1)
    return ok({
        "buildingId": bid,
        "avgPower": round(avg_power, 2),
        "suggestion": "建议优化空调运行时间，可节约约15%能耗",
        "anomalies": []
    })

@app.post("/api/ai-query")
async def ai_query(data: dict):
    return ok({"answer": "AI查询功能暂未配置，请联系管理员配置API密钥。", "buildingId": data.get("buildingId")})

@app.get("/api/energy-analysis/{building_id}/issue")
async def analyze_issue(building_id: str, timeRange: str = "7d"):
    return ok({"buildingId": building_id, "issues": [], "suggestions": ["运行状态良好，暂无异常"]})

# ═══════════════════════════════════════════════
# REPORT
# ═══════════════════════════════════════════════
@app.get("/api/report/{building_id}")
async def get_report(building_id: str, db: Session = Depends(get_db)):
    b = db.query(Building).filter(Building.building_id == building_id).first()
    records = db.query(EnergyConsumption).filter(
        EnergyConsumption.building_id == building_id
    ).order_by(EnergyConsumption.timestamp.desc()).limit(30).all()
    total_power = sum(r.power_consumption_kwh for r in records)
    return ok({
        "buildingId": building_id,
        "buildingName": b.building_name if b else building_id,
        "period": "近30天",
        "totalPower": round(total_power, 2),
        "avgPower": round(total_power / max(len(records), 1), 2),
        "records": [energy_to_dict(r) for r in records[:10]]
    })

@app.get("/api/report/{building_id}/export")
async def export_report(building_id: str):
    return ok({"message": "报告导出功能开发中"})

# ═══════════════════════════════════════════════
# DATA MANAGEMENT
# ═══════════════════════════════════════════════
@app.post("/api/data/preview")
async def preview_data(file: UploadFile = File(None)):
    return ok({"columns": [], "rows": [], "message": "预览功能"})

@app.post("/api/data/import")
async def import_data(file: UploadFile = File(None)):
    return ok({"imported": 0, "message": "数据导入功能"})

@app.get("/api/data/import-history")
async def import_history():
    return ok([])

# ═══════════════════════════════════════════════
# ROOT
# ═══════════════════════════════════════════════
@app.get("/")
async def root():
    return ok({"message": "建筑能耗数字孪生管理系统 Python版", "version": "1.0.0", "status": "运行中"})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
