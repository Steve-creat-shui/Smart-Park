from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class EnergyConsumptionBase(BaseModel):
    building_id: str
    building_type: Optional[str] = None
    timestamp: datetime
    power_consumption_kwh: Optional[float] = None
    water_consumption_m3: Optional[float] = None
    carbon_emission_kg: Optional[float] = None
    ac_consumption_kwh: Optional[float] = None
    lighting_consumption_kwh: Optional[float] = None
    other_consumption_kwh: Optional[float] = None
    temperature_celsius: Optional[float] = None
    humidity_percent: Optional[float] = None
    occupancy_rate: Optional[float] = None
    status: Optional[str] = "normal"

class EnergyConsumptionCreate(EnergyConsumptionBase):
    pass

class EnergyConsumptionResponse(EnergyConsumptionBase):
    id: int
    created_at: datetime
    
    class Config:
        from_attributes = True

class BuildingBase(BaseModel):
    building_id: str
    building_name: Optional[str] = None
    building_type: Optional[str] = None
    floor_count: Optional[int] = None
    total_area_m2: Optional[float] = None
    location: Optional[str] = None
    construction_year: Optional[int] = None
    energy_rating: Optional[str] = None
    ac_system_type: Optional[str] = None
    status: Optional[str] = "active"

class BuildingCreate(BuildingBase):
    pass

class BuildingResponse(BuildingBase):
    created_at: datetime
    
    class Config:
        from_attributes = True

class DashboardStats(BaseModel):
    total_buildings: int
    total_energy_today: float
    avg_temperature: float
    alert_count: int
