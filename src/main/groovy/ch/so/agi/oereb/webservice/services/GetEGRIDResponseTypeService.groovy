package ch.so.agi.oereb.webservice.services

import javax.sql.DataSource
import javax.xml.bind.JAXBElement

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import ch.admin.geo.schemas.v_d.oereb._1_0.extract.GetEGRIDResponseType
import ch.admin.geo.schemas.v_d.oereb._1_0.extract.ObjectFactory
import groovy.sql.*

@Service
class GetEGRIDResponseTypeService {
    private final Logger log = LoggerFactory.getLogger(this.getClass())
    
    @Autowired
    DataSource dataSource
    
    public GetEGRIDResponseType getGetEGRIDResponseTypeByXY(double easting, double northing) {        
        Sql sql = new Sql(dataSource)
        String query = """
SELECT parcels.t_id, 
  CASE 
    WHEN g.egris_egrid IS NULL THEN 'CH-DUMMY' 
    ELSE g.egris_egrid 
  END AS egrid, 
  g.nbident AS identdn, 
  g.nummer AS number,
  'valid' AS validitytype, -- opposite: planned
  CASE
    WHEN g.art = 'Liegenschaft' THEN 'RealEstate' 
    ELSE 'Distinct_and_permanent_rights.BuildingRight'
  END AS realestatetype
FROM 
  ( 
    SELECT t_id,  
      geometrie AS geometrie, 
      liegenschaft_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_liegenschaft 
    UNION ALL 
    SELECT t_id,  
      geometrie AS geometrie, 
      selbstrecht_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_selbstrecht 
  ) AS parcels 
  LEFT JOIN agi_avdpool.liegenschaften_grundstueck AS g 
  ON g.t_id = parcels.grundstueck_fk 
WHERE 
  ST_Intersects(ST_SetSRID(ST_MakePoint(:x, :y),2056), geometrie) 
;
"""
        List parcelList = sql.rows(query, [x:easting, y:northing])
        return createGetEGRIDResponseType(parcelList)
    }
    
    public GetEGRIDResponseType getGetEGRIDResponseTypeByGNSS(double latitude, double longitude) {
        Sql sql = new Sql(dataSource)
        String query = """
SELECT parcels.t_id, 
  CASE 
    WHEN g.egris_egrid IS NULL THEN 'CH-DUMMY' 
    ELSE g.egris_egrid 
  END AS egrid, 
  g.nbident AS identdn, 
  g.nummer AS number,
  'valid' AS validitytype, -- opposite: planned
  CASE
    WHEN g.art = 'Liegenschaft' THEN 'RealEstate' 
    ELSE 'Distinct_and_permanent_rights.BuildingRight'
  END AS realestatetype
FROM 
  ( 
    SELECT t_id,  
      geometrie AS geometrie, 
      liegenschaft_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_liegenschaft 
    UNION ALL 
    SELECT t_id,  
      geometrie AS geometrie, 
      selbstrecht_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_selbstrecht 
  ) AS parcels 
  LEFT JOIN agi_avdpool.liegenschaften_grundstueck AS g 
  ON g.t_id = parcels.grundstueck_fk 
WHERE 
  ST_Intersects(ST_Transform(ST_SetSRID(ST_MakePoint(:x, :y),4326),2056), geometrie) 
;
"""
        List parcelList = sql.rows(query, [x:latitude, y:longitude])
        return createGetEGRIDResponseType(parcelList)
    }

    public GetEGRIDResponseType getGetEGRIDResponseTypeByNumberAndIdentDN(String number, String identdn) {
        Sql sql = new Sql(dataSource)
        String query = """
SELECT parcels.t_id, 
  CASE 
    WHEN g.egris_egrid IS NULL THEN 'CH-DUMMY' 
    ELSE g.egris_egrid 
  END AS egrid, 
  g.nbident AS identdn, 
  g.nummer AS number,
  'valid' AS validitytype, -- opposite: planned
  CASE
    WHEN g.art = 'Liegenschaft' THEN 'RealEstate' 
    ELSE 'Distinct_and_permanent_rights.BuildingRight'
  END AS realestatetype
FROM 
  ( 
    SELECT t_id,  
      geometrie AS geometrie, 
      liegenschaft_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_liegenschaft 
    UNION ALL 
    SELECT t_id,  
      geometrie AS geometrie, 
      selbstrecht_von AS grundstueck_fk 
    FROM 
      agi_avdpool.liegenschaften_selbstrecht 
  ) AS parcels 
  LEFT JOIN agi_avdpool.liegenschaften_grundstueck AS g 
  ON g.t_id = parcels.grundstueck_fk 
WHERE 
  g.nbident = :identdn AND g.nummer = :number 
;
"""
        List parcelList = sql.rows(query, [number:number, identdn:identdn])
        return createGetEGRIDResponseType(parcelList)
    }
    private GetEGRIDResponseType createGetEGRIDResponseType(List parcelList) {
        ObjectFactory objectFactory = new ObjectFactory()
        GetEGRIDResponseType getEGRIDResponseType = objectFactory.createGetEGRIDResponseType()
        
        parcelList.each { parcelRow ->
            JAXBElement<String> egridEl = objectFactory.createGetEGRIDResponseTypeEgrid(parcelRow.get("egrid"))
            getEGRIDResponseType.getEgridAndNumberAndIdentDN().add(egridEl)
            
            JAXBElement<String> numberEl = objectFactory.createGetEGRIDResponseTypeNumber(parcelRow.get("number"))
            getEGRIDResponseType.getEgridAndNumberAndIdentDN().add(numberEl);

            JAXBElement<String> identdnEl = objectFactory.createGetEGRIDResponseTypeIdentDN(parcelRow.get("identdn"))
            getEGRIDResponseType.getEgridAndNumberAndIdentDN().add(identdnEl);
        }
        return getEGRIDResponseType
    }
}
