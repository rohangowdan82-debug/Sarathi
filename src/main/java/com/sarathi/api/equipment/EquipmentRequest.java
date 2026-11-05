package com.sarathi.api.equipment;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
public record EquipmentRequest(@NotBlank @Size(min=3,max=120) String name,@NotBlank @Pattern(regexp="TRACTOR|HARVESTER|ROTAVATOR|SPRAYER|SEED_DRILL|CULTIVATOR|OTHER") String type,@Size(max=100) String brand,@Size(max=100) String model,@Min(1900) @Max(2100) Integer manufacturingYear,@Size(max=3000) String description,Map<String,Object> specifications,List<@Size(max=1000) String> photos,@NotBlank @Size(max=100) String village,@NotBlank @Size(max=100) String district,@Size(max=100) String state,Double latitude,Double longitude,@NotNull @Min(0) @Max(100000000) Long pricePerDayPaise, @Min(0) @Max(100000000) Long pricePerAcrePaise,@Min(0) @Max(10000000) Long transportFeePaise,Boolean operatorAvailable) {}
