package com.sarathi.api.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
@Component
public class OriginGuardFilter extends OncePerRequestFilter {
 private final String appOrigin; private final ObjectMapper mapper;
 public OriginGuardFilter(@Value("${sarathi.app-origin}") String appOrigin,ObjectMapper mapper){this.appOrigin=appOrigin;this.mapper=mapper;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String method=req.getMethod();String origin=req.getHeader("Origin");boolean write=!(method.equals("GET")||method.equals("HEAD")||method.equals("OPTIONS"));
  if(write&&origin!=null&&!origin.equals(appOrigin)){res.setStatus(403);res.setContentType("application/json");mapper.writeValue(res.getWriter(),Map.of("error",Map.of("code","ORIGIN_NOT_ALLOWED","message","This website origin is not allowed.")));return;}
  chain.doFilter(req,res);
 }
}
