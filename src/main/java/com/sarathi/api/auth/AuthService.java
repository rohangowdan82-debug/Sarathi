package com.sarathi.api.auth;
import com.sarathi.api.common.ApiException;
import com.sarathi.api.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
@Service
public class AuthService {
 private record SessionRow(String id,String userId,String familyId,Instant expiresAt,Timestamp revokedAt){}
 private record LoginRow(String id,String role,String fullName,String passwordHash,boolean active){}
 private record DbUser(String id,String role,String fullName,boolean active){}
 private final JdbcTemplate db;private final PasswordEncoder passwords;private final JwtService jwt;private final int refreshDays;private final SecureRandom random=new SecureRandom();
 public AuthService(JdbcTemplate db,PasswordEncoder passwords,JwtService jwt,@Value("${sarathi.security.refresh-ttl-days:30}") int refreshDays){this.db=db;this.passwords=passwords;this.jwt=jwt;this.refreshDays=refreshDays;}
 @Transactional public AuthOutcome register(RegisterRequest r,HttpServletRequest request){
  if((r.email()==null||r.email().isBlank())&&(r.phone()==null||r.phone().isBlank()))throw ApiException.badRequest("Email or phone is required.");
  if((r.latitude()==null)!=(r.longitude()==null))throw ApiException.badRequest("Latitude and longitude must be supplied together.");
  String id=UUID.randomUUID().toString(),email=r.email()==null||r.email().isBlank()?null:r.email().trim().toLowerCase(),phone=normalizePhone(r.phone()),role=r.role()==null?"FARMER":r.role();
  db.update("INSERT INTO users(id,email,phone,password_hash,full_name,role,village,district,state,latitude,longitude) VALUES(?,?,?,?,?,?,?,?,?,?,?)",id,email,phone,passwords.encode(r.password()),r.fullName().trim(),role,r.village().trim(),r.district().trim(),r.state()==null||r.state().isBlank()?"Maharashtra":r.state().trim(),r.latitude(),r.longitude());
  if(role.equals("OPERATOR"))db.update("INSERT INTO operator_profiles(user_id,skills) VALUES(?,JSON_ARRAY())",id);
  audit(id,"USER_REGISTERED","USER",id);
  return issue(id,role,r.fullName(),request);
 }
 public AuthOutcome login(LoginRequest r,HttpServletRequest request){
  db.update("DELETE FROM login_attempts WHERE window_started<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 DAY)");String value=r.identifier().trim();String email=value.contains("@")?value.toLowerCase():value;String phone=normalizePhone(value);String attemptKey=hash(String.valueOf(request.getRemoteAddr())+"|"+value.toLowerCase());db.update("INSERT INTO login_attempts(attempt_key,attempts,window_started) VALUES(?,1,CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE attempts=IF(window_started<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 15 MINUTE),1,attempts+1),window_started=IF(window_started<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 15 MINUTE),CURRENT_TIMESTAMP(6),window_started)",attemptKey);Integer tries=db.queryForObject("SELECT attempts FROM login_attempts WHERE attempt_key=?",Integer.class,attemptKey);if(tries!=null&&tries>10)throw ApiException.tooManyRequests("Too many sign-in attempts. Wait a few minutes and try again.");
  var matches=db.query("SELECT id,role,full_name,password_hash,active FROM users WHERE (email IS NOT NULL AND lower(email)=lower(?)) OR (phone IS NOT NULL AND phone=?) LIMIT 1",(rs,n)->new LoginRow(rs.getString("id"),rs.getString("role"),rs.getString("full_name"),rs.getString("password_hash"),rs.getBoolean("active")),email,phone);
  if(matches.isEmpty())throw ApiException.unauthorized("The email/phone or password is incorrect.");var u=matches.get(0);
  if(!u.active()||!passwords.matches(r.password(),u.passwordHash()))throw ApiException.unauthorized("The email/phone or password is incorrect.");
  db.update("DELETE FROM login_attempts WHERE attempt_key=?",attemptKey);return issue(u.id(),u.role(),u.fullName(),request);
 }
 @Transactional public AuthOutcome refresh(String raw,HttpServletRequest request){
  if(raw==null||raw.isBlank())return AuthOutcome.denied();String hash=hash(raw);
  var rows=db.query("SELECT id,user_id,family_id,expires_at,revoked_at FROM refresh_sessions WHERE token_hash=? FOR UPDATE",(rs,n)->new SessionRow(rs.getString("id"),rs.getString("user_id"),rs.getString("family_id"),rs.getTimestamp("expires_at").toInstant(),rs.getTimestamp("revoked_at")),hash);
  if(rows.isEmpty())return AuthOutcome.denied();var old=rows.get(0);
  if(old.revokedAt()!=null){db.update("UPDATE refresh_sessions SET revoked_at=COALESCE(revoked_at,CURRENT_TIMESTAMP(6)) WHERE family_id=?",old.familyId());return AuthOutcome.denied();}
  if(old.expiresAt().isBefore(Instant.now()))return AuthOutcome.denied();
  var users=db.query("SELECT id,role,full_name,active FROM users WHERE id=?",(rs,n)->new DbUser(rs.getString("id"),rs.getString("role"),rs.getString("full_name"),rs.getBoolean("active")),old.userId());if(users.isEmpty()||!users.get(0).active())return AuthOutcome.denied();DbUser u=users.get(0);
  String next=refreshToken(),nextHash=hash(next);db.update("UPDATE refresh_sessions SET revoked_at=CURRENT_TIMESTAMP(6),replaced_by_hash=? WHERE id=?",nextHash,old.id());saveSession(u.id(),old.familyId(),nextHash,request);
  return new AuthOutcome(false,jwt.create(u.id(),u.role()),next,new UserView(u.id(),u.fullName(),u.role()));
 }
 @Transactional public void logout(String raw){if(raw!=null&&!raw.isBlank())db.update("UPDATE refresh_sessions SET revoked_at=COALESCE(revoked_at,CURRENT_TIMESTAMP(6)) WHERE token_hash=?",hash(raw));}
 public Map<String,Object> me(String id){try{return db.queryForMap("SELECT id,email,phone,full_name AS fullName,role,village,district,state,latitude,longitude,created_at AS createdAt FROM users WHERE id=? AND active=true",id);}catch(org.springframework.dao.EmptyResultDataAccessException e){throw ApiException.unauthorized("Account is unavailable.");}}
 private AuthOutcome issue(String id,String role,String fullName,HttpServletRequest request){String refresh=refreshToken(),family=UUID.randomUUID().toString();saveSession(id,family,hash(refresh),request);return new AuthOutcome(false,jwt.create(id,role),refresh,new UserView(id,fullName,role));}
 private void saveSession(String id,String family,String hash,HttpServletRequest r){db.update("INSERT INTO refresh_sessions(id,user_id,family_id,token_hash,expires_at,user_agent,ip_address) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,family,hash,Timestamp.from(Instant.now().plus(refreshDays,ChronoUnit.DAYS)),truncate(r.getHeader("User-Agent"),500),truncate(r.getRemoteAddr(),45));}
 private String refreshToken(){byte[] b=new byte[48];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private String normalizePhone(String p){return p==null?null:p.replaceAll("[ ()-]","");}
 private String truncate(String s,int n){return s==null?null:s.substring(0,Math.min(n,s.length()));}
 private void audit(String actor,String action,String type,String entity){db.update("INSERT INTO audit_events(actor_id,action,entity_type,entity_id,metadata) VALUES(?,?,?,?,JSON_OBJECT())",actor,action,type,entity);}
}
