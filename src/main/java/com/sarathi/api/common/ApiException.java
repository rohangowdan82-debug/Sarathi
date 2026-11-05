package com.sarathi.api.common;
public class ApiException extends RuntimeException {
 private final int status; private final String code;
 public ApiException(int status,String code,String message){super(message);this.status=status;this.code=code;}
 public int status(){return status;} public String code(){return code;}
 public static ApiException badRequest(String message){return new ApiException(400,"VALIDATION_ERROR",message);}
 public static ApiException unauthorized(String message){return new ApiException(401,"UNAUTHENTICATED",message);}
 public static ApiException forbidden(String message){return new ApiException(403,"FORBIDDEN",message);}
 public static ApiException notFound(String message){return new ApiException(404,"NOT_FOUND",message);}
 public static ApiException tooManyRequests(String message){return new ApiException(429,"RATE_LIMITED",message);}
 public static ApiException conflict(String message){return new ApiException(409,"CONFLICT",message);}
}
