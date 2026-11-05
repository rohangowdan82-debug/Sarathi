package com.sarathi.api.common;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.UUID;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e,HttpServletRequest r){return ResponseEntity.status(e.status()).body(Map.of("error",Map.of("code",e.code(),"message",e.getMessage(),"requestId",id(r))));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e,HttpServletRequest r){var details=e.getBindingResult().getFieldErrors().stream().map(x->Map.of("field",x.getField(),"message",String.valueOf(x.getDefaultMessage()))).toList();return ResponseEntity.badRequest().body(Map.of("error",Map.of("code","VALIDATION_ERROR","message","Please check the submitted fields.","details",details,"requestId",id(r))));}
 @ExceptionHandler({ConstraintViolationException.class,HttpMessageNotReadableException.class}) ResponseEntity<?> malformed(Exception e,HttpServletRequest r){return ResponseEntity.badRequest().body(Map.of("error",Map.of("code","VALIDATION_ERROR","message","The submitted information is invalid.","requestId",id(r))));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> integrity(HttpServletRequest r){return ResponseEntity.status(409).body(Map.of("error",Map.of("code","DATA_CONFLICT","message","That change conflicts with an existing record.","requestId",id(r))));}
 @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<?> duplicate(HttpServletRequest r){return ResponseEntity.status(409).body(Map.of("error",Map.of("code","DUPLICATE_VALUE","message","A record with those details already exists.","requestId",id(r))));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e,HttpServletRequest r){return ResponseEntity.internalServerError().body(Map.of("error",Map.of("code","INTERNAL_ERROR","message","An unexpected error occurred.","requestId",id(r))));}
 private String id(HttpServletRequest r){Object id=r.getAttribute("requestId");return id==null?UUID.randomUUID().toString():id.toString();}
}
