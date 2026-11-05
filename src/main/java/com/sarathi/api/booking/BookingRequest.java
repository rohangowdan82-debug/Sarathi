package com.sarathi.api.booking;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record BookingRequest(@NotBlank String equipmentId,@NotNull LocalDate startDate,@NotNull LocalDate endDate,Double acres,String operatorId,@Size(max=1000) String note,@Size(max=100) String idempotencyKey,@Size(max=100) String farmVillage,@Size(max=100) String farmDistrict,Double farmLatitude,Double farmLongitude) {}
