package com.sarathi.api.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import java.util.Map;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 @Bean FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(JwtAuthenticationFilter filter){FilterRegistrationBean<JwtAuthenticationFilter> bean=new FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;}
 @Bean FilterRegistrationBean<OriginGuardFilter> originFilterServletRegistration(OriginGuardFilter filter){FilterRegistrationBean<OriginGuardFilter> bean=new FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean CorsConfigurationSource cors(@Value("${sarathi.app-origin}") String origin){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Request-Id"));c.setAllowCredentials(true);c.setMaxAge(3600L);UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
 @Bean SecurityFilterChain security(HttpSecurity http,JwtAuthenticationFilter jwt,OriginGuardFilter origin,ObjectMapper mapper)throws Exception{
  return http.csrf(csrf->csrf.disable()).cors(cors->{}).headers(h->h.contentSecurityPolicy(csp->csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))).sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/sarathi.js","/favicon.ico","/api/auth/**","/api/health/**","/actuator/health/**").permitAll().requestMatchers(HttpMethod.GET,"/api/equipment/**","/api/operators/**","/api/community/**").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,err)->{res.setStatus(401);res.setContentType("application/json");mapper.writeValue(res.getWriter(),Map.of("error",Map.of("code","UNAUTHENTICATED","message","Sign in to continue.")));}).accessDeniedHandler((req,res,err)->{res.setStatus(403);res.setContentType("application/json");mapper.writeValue(res.getWriter(),Map.of("error",Map.of("code","FORBIDDEN","message","You do not have permission to do that.")));}))
   .addFilterBefore(origin,UsernamePasswordAuthenticationFilter.class).addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
 }
}

