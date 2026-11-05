package com.sarathi.api.activity;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public final class ActivityDtos {
 private ActivityDtos(){}
 public record OperatorUpdate(@NotEmpty @Size(max=7) List<@Pattern(regexp="TRACTOR|HARVESTER|ROTAVATOR|SPRAYER|SEED_DRILL|CULTIVATOR|OTHER") String> skills,@Min(0) @Max(10000000) long dailyRatePaise,@Min(0) @Max(60) int experienceYears,@Min(1) @Max(500) int serviceAreaKm,boolean available){}
 public record MaintenanceRequest(@NotBlank String equipmentId,@NotNull LocalDate serviceDate,@DecimalMin("0") Double operatingHours,@NotBlank @Size(max=2000) String summary,@Min(0) @Max(100000000) long costPaise,boolean makesUnavailable,LocalDate nextServiceDate){}
 public record FeedbackRequest(@Min(1) @Max(5) int rating,@Pattern(regexp="GENERAL|BOOKING|EQUIPMENT|PAYMENTS|OPERATORS|OTHER") String category,@NotBlank @Size(min=5,max=3000) String message){}
 public record PostRequest(@NotBlank @Size(max=2000) String body){}
 public record MarkPaidRequest(@Size(max=30) String method,@Size(max=100) String reference){}
}
