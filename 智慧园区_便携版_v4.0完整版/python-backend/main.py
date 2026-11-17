from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from database import engine, Base, get_db
from models import User, Building, EnergyConsumption
from services.auth_service import get_password_hash
from datetime import datetime, timedelta
import random

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="建筑能耗数字孪生管理系统",
    version="1.0.0",
    description="Python FastAPI后端 - 替代原Java Spring Boot"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from routers.auth import router as auth_router
from routers.energy import router as energy_router

app.include_router(auth_router)
app.include_router(energy_router)

@app.on_event("startup")
async def startup_event():
    db = next(get_db())
    
    if db.query(User).count() == 0:
        admin = User(
            phone="13800138000",
            password=get_password_hash("123456"),
            nickname="管理员",
            role="admin"
        )
        test_user = User(
            phone="13900139000",
            password=get_password_hash("123456"),
            nickname="测试用户",
            role="user"
        )
        db.add_all([admin, test_user])
        db.commit()
        print("✅ 默认用户数据初始化完成")
        print("   管理员账号: 13800138000 / 123456")
        print("   测试用户账号: 13900139000 / 123456")
    
    if db.query(Building).count() == 0:
        buildings_data = [
            Building(building_id="B001", building_name="行政楼", building_type="行政楼", floor_count=8, total_area_m2=10000.0, location="校区中心", construction_year=2010, energy_rating="A", ac_system_type="中央空调"),
            Building(building_id="B002", building_name="图书馆", building_type="图书馆", floor_count=5, total_area_m2=8000.0, location="校区东区", construction_year=2012, energy_rating="A", ac_system_type="变频中央空调"),
            Building(building_id="B003", building_name="实验楼", building_type="实验楼", floor_count=6, total_area_m2=9500.0, location="校区西区", construction_year=2015, energy_rating="B", ac_system_type="变频中央空调"),
            Building(building_id="B004", building_name="教学楼", building_type="教学楼", floor_count=10, total_area_m2=15000.0, location="校区中心", construction_year=2008, energy_rating="B", ac_system_type="中央空调"),
            Building(building_id="B005", building_name="宿舍楼", building_type="宿舍楼", floor_count=12, total_area_m2=20000.0, location="校区北区", construction_year=2009, energy_rating="C", ac_system_type="分体空调"),
            Building(building_id="B006", building_name="餐厅", building_type="餐厅", floor_count=3, total_area_m2=4000.0, location="校区南区", construction_year=2011, energy_rating="B", ac_system_type="分体空调"),
            Building(building_id="B007", building_name="室内体育馆", building_type="体育馆", floor_count=2, total_area_m2=12000.0, location="校区东南区", construction_year=2018, energy_rating="A", ac_system_type="中央空调"),
        ]
        db.add_all(buildings_data)
        db.commit()
        print(f"✅ 默认建筑数据初始化完成，共 {len(buildings_data)} 栋建筑")
    
    if db.query(EnergyConsumption).count() == 0:
        now = datetime.now()
        building_ids = ["B001", "B002", "B003", "B004", "B005", "B006", "B007"]
        building_types = ["行政楼", "图书馆", "实验楼", "教学楼", "宿舍楼", "餐厅", "体育馆"]
        
        energy_records = []
        
        for i, (bid, btype) in enumerate(zip(building_ids, building_types)):
            base_power = 100 + (i * 20)
            
            for day in range(30):
                for hour in range(0, 24, 6):
                    timestamp = now - timedelta(days=day) - timedelta(hours=hour % 24)
                    timestamp = timestamp.replace(minute=0, second=0, microsecond=0)
                    
                    power = base_power + (random.random() * 50)
                    water = power * 0.1
                    carbon = power * 0.785
                    ac = power * 0.4
                    lighting = power * 0.2
                    other = power - ac - lighting
                    temperature = 20 + (random.random() * 5)
                    humidity = 40 + (random.random() * 30)
                    occupancy = 0.3 + (random.random() * 0.5)
                    status = "warning" if random.random() < 0.1 else "normal"
                    
                    record = EnergyConsumption(
                        building_id=bid,
                        building_type=btype,
                        timestamp=timestamp,
                        power_consumption_kwh=round(power, 2),
                        water_consumption_m3=round(water, 2),
                        carbon_emission_kg=round(carbon, 2),
                        ac_consumption_kwh=round(ac, 2),
                        lighting_consumption_kwh=round(lighting, 2),
                        other_consumption_kwh=round(other, 2),
                        temperature_celsius=round(temperature, 1),
                        humidity_percent=round(humidity, 1),
                        occupancy_rate=round(occupancy, 2),
                        status=status
                    )
                    energy_records.append(record)
        
        db.add_all(energy_records)
        db.commit()
        print(f"✅ 默认能耗数据初始化完成，共 {len(energy_records)} 条记录")
    
    db.close()

@app.get("/")
async def root():
    return {
        "message": "欢迎使用建筑能耗数字孪生管理系统 Python版",
        "version": "1.0.0",
        "docs": "/docs",
        "status": "运行中"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8081)
