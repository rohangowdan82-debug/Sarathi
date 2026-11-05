package com.sarathi.api.equipment;
import com.sarathi.api.common.ApiException;
import com.sarathi.api.security.UserPrincipal;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
@RestController
@RequestMapping("/api/allocation")
@PreAuthorize("isAuthenticated()")
public class AllocationController {
 private final NamedParameterJdbcTemplate db;
 public AllocationController(NamedParameterJdbcTemplate db){this.db=db;}
 @GetMapping("/recommendations") public Map<String,Object> recommend(@AuthenticationPrincipal UserPrincipal user,@RequestParam double lat,@RequestParam double lng,@RequestParam String type,@RequestParam LocalDate startDate,@RequestParam LocalDate endDate,@RequestParam(defaultValue="false") boolean operatorRequired,@RequestParam(defaultValue="25") double maxDistanceKm,@RequestParam(defaultValue="5") int urgency){
  if(lat < -90||lat>90||lng < -180||lng>180||!endDate.isAfter(startDate))throw ApiException.badRequest("Provide valid farm coordinates and booking dates.");if(urgency<1||urgency>5)throw ApiException.badRequest("Urgency must be between 1 and 5.");
  String category=type.toUpperCase(Locale.ROOT).replace(' ','_');double radius=Math.max(1,Math.min(500,maxDistanceKm));long days=ChronoUnit.DAYS.between(startDate,endDate);
  String dist="(6371*2*ASIN(SQRT(POWER(SIN(RADIANS(e.latitude-:lat)/2),2)+COS(RADIANS(:lat))*COS(RADIANS(e.latitude))*POWER(SIN(RADIANS(e.longitude-:lng)/2),2))))";
  var p=new MapSqlParameterSource().addValue("lat",lat).addValue("lng",lng).addValue("radius",radius).addValue("type",category).addValue("start",startDate).addValue("end",endDate).addValue("days",days);
  String operatorSql="(SELECT MIN(op.daily_rate_paise) FROM operator_profiles op JOIN users ou ON ou.id=op.user_id WHERE op.available=1 AND ou.active=1 AND JSON_CONTAINS(op.skills,JSON_QUOTE(e.type))=1 AND NOT EXISTS(SELECT 1 FROM bookings ob WHERE ob.operator_id=op.user_id AND ob.status IN ('PENDING','CONFIRMED','IN_PROGRESS') AND ob.start_date<:end AND ob.end_date>:start))";
  String sql="SELECT e.id,e.name,e.type,e.village,e.district,e.price_per_day_paise,e.transport_fee_paise,e.operator_available,"+dist+" AS distance_km, (e.price_per_day_paise*:days+e.transport_fee_paise) AS base_cost_paise,"+operatorSql+" AS operator_rate_paise FROM equipment e WHERE e.status='AVAILABLE' AND e.type=:type AND e.latitude IS NOT NULL AND "+dist+"<=:radius AND NOT EXISTS(SELECT 1 FROM bookings b WHERE b.equipment_id=e.id AND b.status IN ('PENDING','CONFIRMED','IN_PROGRESS') AND b.start_date<:end AND b.end_date>:start) ORDER BY distance_km ASC LIMIT 100";
  var rows=db.queryForList(sql,p);if(operatorRequired)rows=rows.stream().filter(r->r.get("operator_rate_paise")!=null).toList();if(rows.isEmpty())return Map.of("items",List.of(),"message","No equipment matched those dates and distance. Try a wider search.","scoring","Availability 30 · distance 35 · estimated total cost 25 · operator fit 10");
  double best=rows.stream().mapToDouble(r->cost(r,days,operatorRequired)).min().orElse(1);final double max=radius;List<Map<String,Object>> ranked=new ArrayList<>();
  for(var r:rows){double distance=((Number)r.get("distance_km")).doubleValue();double total=cost(r,days,operatorRequired);double distanceScore=35*Math.max(0,1-distance/max);double costScore=25*Math.min(1,best/Math.max(best,total));double operatorScore=operatorRequired?10:10;double urgencyBonus=(urgency>=4?2:0);int score=(int)Math.round(Math.min(100,30+distanceScore+costScore+operatorScore+urgencyBonus));Map<String,Object> item=new LinkedHashMap<>(r);item.put("estimated_total_paise",(long)total);item.put("estimated_transport_paise",r.get("transport_fee_paise"));item.put("recommendation_score",score);item.put("explanation",operatorRequired?"Available for these dates, within your distance limit, and a qualified operator is available.":"Available for these dates, within your distance limit, with a competitive estimated total cost.");ranked.add(item);}
  ranked.sort(Comparator.comparingInt((Map<String,Object> x)->((Number)x.get("recommendation_score")).intValue()).reversed().thenComparingDouble(x->((Number)x.get("distance_km")).doubleValue()));return Map.of("items",ranked,"scoring","Availability 30 · distance 35 · estimated total cost 25 · operator fit 10","notice","Illustrative, deterministic demo ranking; not an AI prediction. Transport cost is the equipment listing fee.");
 }
 private long cost(Map<String,Object> row,long days,boolean withOperator){long amount=((Number)row.get("base_cost_paise")).longValue();if(withOperator&&row.get("operator_rate_paise")!=null)amount+=((Number)row.get("operator_rate_paise")).longValue()*days;return amount;}
}
