from sqlalchemy import Column, Integer, String, Float, DateTime, Index
from sqlalchemy.sql import func
from database import Base

class EnergyConsumption(Base):
    __tablename__ = "energy_consumption"
    
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    building_id = Column(String(50), nullable=False, index=True)
    building_type = Column(String(50))
    timestamp = Column(DateTime(timezone=True), nullable=False, index=True)
    power_consumption_kwh = Column(Float)
    water_consumption_m3 = Column(Float)
    carbon_emission_kg = Column(Float)
    ac_consumption_kwh = Column(Float)
    lighting_consumption_kwh = Column(Float)
    other_consumption_kwh = Column(Float)
    temperature_celsius = Column(Float)
    humidity_percent = Column(Float)
    occupancy_rate = Column(Float)
    status = Column(String(20), default="normal")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    __table_args__ = (
        Index('idx_building_timestamp', 'building_id', 'timestamp'),
    )
