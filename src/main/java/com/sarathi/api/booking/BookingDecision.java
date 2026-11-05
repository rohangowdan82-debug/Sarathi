package com.sarathi.api.booking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record BookingDecision(@NotBlank String decision,@Size(max=1000) String note) {}
