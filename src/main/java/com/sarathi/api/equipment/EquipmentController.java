package com.sarathi.api.equipment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarathi.api.common.ApiException;
import com.sarathi.api.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
 private final JdbcTemplate db;private final NamedParameterJdbcTemplate named;private final ObjectMapper json;
 public EquipmentController(JdbcTemplate db,NamedParameterJdbcTemplate named,ObjectMapper json){this.db=db;this.named=named;this.json=json;}
 @GetMapping public Map<String,Object> search(@RequestParam(required=false) String q,@RequestParam(required=false) String type,@RequestParam(required=false) String district,@RequestParam(required=false) Double lat,@RequestParam(required=false) Double lng,@RequestParam(defaultValue="25") double radiusKm,@RequestParam(required=false) Long minPricePaise,@RequestParam(required=false) Long maxPricePaise,@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,@RequestParam(defaultValue="rating") String sort,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit){
  if((lat==null)!=(lng==null))throw ApiException.badRequest("Latitude and longitude must be supplied together.");if((startDate==null)!=(endDate==null)||startDate!=null&&!endDate.isAfter(startDate))throw ApiException.badRequest("Provide a valid start and end date.");
  int safePage=Math.max(1,page),safeLimit=Math.min(50,Math.max(1,limit));var p=new MapSqlParameterSource();StringBuilder where=new StringBuilder(" WHERE e.status='AVAILABLE'");
  if(q!=null&&!q.isBlank()){where.append(" AND (e.name LIKE :q OR e.description LIKE :q OR e.brand LIKE :q OR e.model LIKE :q)");p.addValue("q","%"+q.trim().substring(0,Math.min(100,q.trim().length()))+"%");}
  if(type!=null&&!type.isBlank()){where.append(" AND e.type=:type");p.addValue("type",type.toUpperCase(Locale.ROOT).replace(' ','_'));}
  if(district!=null&&!district.isBlank()){where.append(" AND lower(e.district)=lower(:district)");p.addValue("district",district.trim());}
  if(minPricePaise!=null){where.append(" AND e.price_per_day_paise>=:minPrice");p.addValue("minPrice",minPricePaise);}if(maxPricePaise!=null){where.append(" AND e.price_per_day_paise<=:maxPrice");p.addValue("maxPrice",maxPricePaise);}
  String distance="NULL AS distance_km";if(lat!=null){p.addValue("lat",lat).addValue("lng",lng).addValue("radius",Math.max(1,Math.min(500,radiusKm)));distance="(6371*2*ASIN(SQRT(POWER(SIN(RADIANS(e.latitude-:lat)/2),2)+COS(RADIANS(:lat))*COS(RADIANS(e.latitude))*POWER(SIN(RADIANS(e.longitude-:lng)/2),2)))) AS distance_km";where.append(" AND e.latitude IS NOT NULL AND ").append(distance.substring(0,distance.indexOf(" AS distance_km"))).append("<=:radius");}
  if(startDate!=null){p.addValue("startDate",startDate).addValue("endDate",endDate);where.append(" AND NOT EXISTS(SELECT 1 FROM bookings b WHERE b.equipment_id=e.id AND b.status IN ('PENDING','CONFIRMED','IN_PROGRESS') AND b.start_date<:endDate AND b.end_date>:startDate)");}
  String order=switch(sort){case "price"->"e.price_per_day_paise ASC";case "distance"->lat==null?"e.created_at DESC":"distance_km ASC";case "newest"->"e.created_at DESC";default->"e.average_rating DESC,e.created_at DESC";};
  String from=" FROM equipment e JOIN users owner ON owner.id=e.owner_id"+where;Long total=named.queryForObject("SELECT COUNT(*)"+from,p,Long.class);p.addValue("limit",safeLimit).addValue("offset",(safePage-1)*safeLimit);
  String sql="SELECT e.id,e.owner_id,owner.full_name AS owner_name,e.name,e.type,e.brand,e.model,e.manufacturing_year,e.description,e.specifications,e.photos,e.village,e.district,e.state,e.latitude,e.longitude,e.price_per_day_paise,e.price_per_acre_paise,e.transport_fee_paise,e.operator_available,e.status,e.average_rating,e.rating_count,"+distance+from+" ORDER BY "+order+" LIMIT :limit OFFSET :offset";
  return Map.of("items",named.queryForList(sql,p),"page",safePage,"limit",safeLimit,"total",total==null?0:total);
 }
 @GetMapping("/mine") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") public Map<String,Object> mine(@AuthenticationPrincipal UserPrincipal user){return Map.of("items",db.queryForList("SELECT * FROM equipment WHERE owner_id=? ORDER BY created_at DESC",user.id()));}
 @PostMapping @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") @Transactional public Map<String,Object> create(@AuthenticationPrincipal UserPrincipal user,@Valid @RequestBody EquipmentRequest r){
  checkCoords(r.latitude(),r.longitude());String id=UUID.randomUUID().toString();
  db.update("INSERT INTO equipment(id,owner_id,name,type,brand,model,manufacturing_year,description,specifications,photos,village,district,state,latitude,longitude,price_per_day_paise,price_per_acre_paise,transport_fee_paise,operator_available) VALUES(?,?,?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON),?,?,?,?,?,?,?,?,?)",id,user.id(),r.name().trim(),r.type(),r.brand(),r.model(),r.manufacturingYear(),r.description()==null?"":r.description(),jsonValue(r.specifications()==null?Map.of():r.specifications()),jsonValue(r.photos()==null?List.of():r.photos()),r.village().trim(),r.district().trim(),r.state()==null?"Maharashtra":r.state(),r.latitude(),r.longitude(),r.pricePerDayPaise(),r.pricePerAcrePaise(),r.transportFeePaise()==null?0:r.transportFeePaise(),Boolean.TRUE.equals(r.operatorAvailable()));audit(user.id(),"EQUIPMENT_CREATED","EQUIPMENT",id);return Map.of("item",db.queryForMap("SELECT * FROM equipment WHERE id=?",id));
 }
 @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") @Transactional public Map<String,Object> update(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id,@Valid @RequestBody EquipmentRequest r){checkCoords(r.latitude(),r.longitude());int n=db.update("UPDATE equipment SET name=?,type=?,brand=?,model=?,manufacturing_year=?,description=?,specifications=CAST(? AS JSON),photos=CAST(? AS JSON),village=?,district=?,state=?,latitude=?,longitude=?,price_per_day_paise=?,price_per_acre_paise=?,transport_fee_paise=?,operator_available=? WHERE id=? AND owner_id=?",r.name().trim(),r.type(),r.brand(),r.model(),r.manufacturingYear(),r.description()==null?"":r.description(),jsonValue(r.specifications()==null?Map.of():r.specifications()),jsonValue(r.photos()==null?List.of():r.photos()),r.village().trim(),r.district().trim(),r.state()==null?"Maharashtra":r.state(),r.latitude(),r.longitude(),r.pricePerDayPaise(),r.pricePerAcrePaise(),r.transportFeePaise()==null?0:r.transportFeePaise(),Boolean.TRUE.equals(r.operatorAvailable()),id,user.id());if(n==0)throw ApiException.notFound("Equipment not found.");return Map.of("item",db.queryForMap("SELECT * FROM equipment WHERE id=?",id));}
 @PatchMapping("/{id}/availability") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") public Map<String,Object> availability(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id,@RequestBody Map<String,String> body){String status=body.getOrDefault("status","");if(!Set.of("AVAILABLE","UNAVAILABLE","MAINTENANCE","INACTIVE").contains(status))throw ApiException.badRequest("Unknown equipment status.");int n=db.update("UPDATE equipment SET status=? WHERE id=? AND owner_id=?",status,id,user.id());if(n==0)throw ApiException.notFound("Equipment not found.");return Map.of("item",db.queryForMap("SELECT id,status,updated_at FROM equipment WHERE id=?",id));}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") public Map<String,Object> deactivate(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id){int n=db.update("UPDATE equipment SET status='INACTIVE' WHERE id=? AND owner_id=?",id,user.id());if(n==0)throw ApiException.notFound("Equipment not found.");return Map.of("message","Equipment listing deactivated.");}
 private void checkCoords(Double lat,Double lng){if((lat==null)!=(lng==null))throw ApiException.badRequest("Latitude and longitude must be supplied together.");if(lat!=null&&(lat < -90||lat>90||lng < -180||lng>180))throw ApiException.badRequest("Location coordinates are invalid.");}
 private String jsonValue(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw ApiException.badRequest("Invalid equipment details.");}}
 private void audit(String actor,String action,String type,String entity){db.update("INSERT INTO audit_events(actor_id,action,entity_type,entity_id,metadata) VALUES(?,?,?,?,JSON_OBJECT())",actor,action,type,entity);}
}
