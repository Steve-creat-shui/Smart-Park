from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from typing import List, Optional
from datetime import datetime, timedelta
from database import get_db
from models.building import Building
from models.energy_consumption import EnergyConsumption
from schemas.energy import (
    EnergyConsumptionCreate, EnergyConsumptionResponse, 
    BuildingCreate, BuildingResponse, DashboardStats
)
from schemas.auth import ApiResponse

router = APIRouter(prefix="/api", tags=["能耗管理"])

@router.get("/buildings", response_model=ApiResponse)
async def get_all_buildings(db: Session = Depends(get_db)):
    buildings = db.query(Building).all()
    return ApiResponse(data=[b.__dict__ for b in buildings])

@router.get("/buildings/{building_id}", response_model=ApiResponse)
async def get_building_by_id(building_id: str, db: Session = Depends(get_db)):
    building = db.query(Building).filter(Building.building_id == building_id).first()
    if not building:
        raise HTTPException(status_code=404, detail="建筑未找到")
    return ApiResponse(data=building.__dict__)

@router.post("/buildings", response_model=ApiResponse)
async def add_building(building: BuildingCreate, db: Session = Depends(get_db)):
    new_building = Building(**building.model_dump())
    db.add(new_building)
    db.commit()
    db.refresh(new_building)
    return ApiResponse(message="建筑信息保存成功", data=new_building.__dict__)

@router.get("/buildings/ids", response_model=ApiResponse)
async def get_all_building_ids(db: Session = Depends(get_db)):
    ids = [b.building_id for b in db.query(Building.building_id).all()]
    return ApiResponse(data=ids)

@router.get("/buildings/list", response_model=ApiResponse)
async def get_building_list(db: Session = Depends(get_db)):
    buildings = db.query(Building).all()
    list_data = [{"id": b.building_id, "name": b.building_name or b.building_id} for b in buildings]
    return ApiResponse(data=list_data)

@router.get("/energy-consumption/{building_id}", response_model=ApiResponse)
async def get_energy_data(
    building_id: str,
    start: Optional[datetime] = None,
    end: Optional[datetime] = None,
    db: Session = Depends(get_db)
):
    query = db.query(EnergyConsumption).filter(EnergyConsumption.building_id == building_id)
    
    if start and end:
        query = query.filter(EnergyConsumption.timestamp >= start, EnergyConsumption.timestamp <= end)
    
    data = query.order_by(desc(EnergyConsumption.timestamp)).all()
    return ApiResponse(data=[e.__dict__ for e in data])

@router.get("/energy-consumption/{building_id}/latest", response_model=ApiResponse)
async def get_latest_energy_data(
    building_id: str,
    limit: int = Query(default=24),
    db: Session = Depends(get_db)
):
    data = db.query(EnergyConsumption)\
        .filter(EnergyConsumption.building_id == building_id)\
        .order_by(desc(EnergyConsumption.timestamp))\
        .limit(limit)\
        .all()
    return ApiResponse(data=[e.__dict__ for e in data])

@router.get("/energy-consumption/{building_id}/anomalies", response_model=ApiResponse)
async def get_anomalies(building_id: str, db: Session = Depends(get_db)):
    anomalies = db.query(EnergyConsumption)\
        .filter(EnergyConsumption.building_id == building_id, EnergyConsumption.status == "warning")\
        .all()
    return ApiResponse(data=[a.__dict__ for a in anomalies])

@router.post("/energy-consumption", response_model=ApiResponse)
async def add_energy_data(energy: EnergyConsumptionCreate, db: Session = Depends(get_db)):
    new_energy = EnergyConsumption(**energy.model_dump())
    db.add(new_energy)
    db.commit()
    db.refresh(new_energy)
    return ApiResponse(message="数据保存成功", data=new_energy.__dict__)

@router.get("/dashboard/stats", response_model=ApiResponse)
async def get_dashboard_stats(db: Session = Depends(get_db)):
    total_buildings = db.query(Building).count()
    
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    today_energy = db.query(func.sum(EnergyConsumption.power_consumption_kwh))\
        .filter(EnergyConsumption.timestamp >= today).scalar() or 0
    
    avg_temp = db.query(func.avg(EnergyConsumption.temperature_celsius)).scalar() or 0
    
    alert_count = db.query(EnergyConsumption)\
        .filter(EnergyConsumption.status == "warning").count()
    
    stats = {
        "total_buildings": total_buildings,
        "total_energy_today": round(today_energy, 2),
        "avg_temperature": round(avg_temp, 1),
        "alert_count": alert_count
    }
    return ApiResponse(data=stats)

@router.get("/energy-consumption/{building_id}/available-dates", response_model=ApiResponse)
async def get_available_dates(building_id: str, db: Session = Depends(get_db)):
    dates = db.query(func.date(EnergyConsumption.timestamp))\
        .filter(EnergyConsumption.building_id == building_id)\
        .distinct()\
        .all()
    return ApiResponse(data=[str(d[0]) for d in dates])

@router.get("/energy-consumption/{building_id}/monthly-trend", response_model=ApiResponse)
async def get_monthly_trend(building_id: str, db: Session = Depends(get_db)):
    from sqlalchemy import extract
    
    monthly_data = db.query(
        extract('year', EnergyConsumption.timestamp).label('year'),
        extract('month', EnergyConsumption.timestamp).label('month'),
        func.sum(EnergyConsumption.power_consumption_kwh).label('total')
    ).filter(EnergyConsumption.building_id == building_id)\
     .group_by('year', 'month')\
     .order_by('year', 'month')\
     .all()
    
    trend = {
        "labels": [f"{d.year}-{d.month:02d}" for d in monthly_data],
        "values": [round(d.total, 2) for d in monthly_data]
    }
    return ApiResponse(data=trend)
