package com.sarathi.api.booking;
import com.sarathi.api.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {
 private final BookingService service;public BookingController(BookingService service){this.service=service;}
 @GetMapping Map<String,Object> list(@AuthenticationPrincipal UserPrincipal user){return Map.of("items",service.list(user.id()));}
 @PostMapping @PreAuthorize("hasAnyAuthority('FARMER','ADMIN')") Map<String,Object> create(@AuthenticationPrincipal UserPrincipal user,@Valid @RequestBody BookingRequest request){return service.create(user,request);}
 @PatchMapping("/{id}/decision") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") Map<String,Object> decision(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id,@Valid @RequestBody BookingDecision request){return service.decision(user,id,request.decision(),request.note());}
 @PostMapping("/{id}/cancel") Map<String,Object> cancel(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id){return service.cancel(user,id);}
 @PutMapping("/{id}/operator") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") Map<String,Object> assign(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id,@Valid @RequestBody OperatorAssignment request){return service.assign(user,id,request.operatorId());}
 @PostMapping("/{id}/complete") @PreAuthorize("hasAnyAuthority('OWNER','ADMIN')") Map<String,Object> complete(@AuthenticationPrincipal UserPrincipal user,@PathVariable String id){return service.complete(user,id);}
}
