from sqlalchemy import Column, String, Integer, Float, DateTime
from sqlalchemy.sql import func
from database import Base

class Building(Base):
    __tablename__ = "buildings"
    
    building_id = Column(String(50), primary_key=True)
    building_name = Column(String(100))
    building_type = Column(String(50))
    floor_count = Column(Integer)
    total_area_m2 = Column(Float)
    location = Column(String(200))
    construction_year = Column(Integer)
    energy_rating = Column(String(10))
    ac_system_type = Column(String(50))
    status = Column(String(20), default="active")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
