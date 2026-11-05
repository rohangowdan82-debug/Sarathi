package com.sarathi.api.auth;
import com.sarathi.api.common.ApiException;
import com.sarathi.api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 private static final String COOKIE="sarathi_refresh";private final AuthService service;private final int refreshDays;private final boolean secure;
 public AuthController(AuthService service,@Value("${sarathi.security.refresh-ttl-days:30}") int refreshDays,@Value("${sarathi.security.cookie-secure:false}") boolean secure){this.service=service;this.refreshDays=refreshDays;this.secure=secure;}
 @PostMapping("/register") ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,HttpServletRequest req,HttpServletResponse res){var result=service.register(request,req);setCookie(res,result.refreshToken());return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("accessToken",result.accessToken(),"user",result.user()));}
 @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody LoginRequest request,HttpServletRequest req,HttpServletResponse res){var result=service.login(request,req);setCookie(res,result.refreshToken());return Map.of("accessToken",result.accessToken(),"user",result.user());}
 @PostMapping("/refresh") Map<String,Object> refresh(@CookieValue(value=COOKIE,required=false) String token,HttpServletRequest req,HttpServletResponse res){var result=service.refresh(token,req);if(result.rejected()){clearCookie(res);throw ApiException.unauthorized("Your session has expired. Sign in again.");}setCookie(res,result.refreshToken());return Map.of("accessToken",result.accessToken(),"user",result.user());}
 @PostMapping("/logout") ResponseEntity<Void> logout(@CookieValue(value=COOKIE,required=false) String token,HttpServletResponse res){service.logout(token);clearCookie(res);return ResponseEntity.noContent().build();}
 @GetMapping("/me") @PreAuthorize("isAuthenticated()") Map<String,Object> me(@AuthenticationPrincipal UserPrincipal principal){return Map.of("user",service.me(principal.id()));}
 private void setCookie(HttpServletResponse res,String value){res.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,value).httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(Duration.ofDays(refreshDays)).build().toString());}
 private void clearCookie(HttpServletResponse res){res.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,"").httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(Duration.ZERO).build().toString());}
}
