package com.sarathi.api.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
@Component
public class RequestIdFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String supplied=req.getHeader("X-Request-Id");String id=supplied!=null&&supplied.matches("[A-Za-z0-9._-]{1,80}")?supplied:UUID.randomUUID().toString();req.setAttribute("requestId",id);res.setHeader("X-Request-Id",id);chain.doFilter(req,res);}
}
