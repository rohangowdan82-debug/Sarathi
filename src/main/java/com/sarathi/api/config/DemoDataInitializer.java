package com.sarathi.api.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sarathi.demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    public static final String DEMO_OWNER_EMAIL = "demo.owner@sarathi.test";
    public static final String DEMO_OWNER_PASSWORD = "SarathiDemo#2026";
    public static final String DEMO_OPERATOR_EMAIL = "demo.operator@sarathi.test";
    public static final String DEMO_OPERATOR_PASSWORD = "SarathiOperator#2026";

    private record DemoEquipment(String name, String type, String brand, String model, int year,
                                 String description, String village, String district,
                                 long dailyPricePaise, long transportPaise, boolean operator,
                                 String specifications) { }

    private static final List<DemoEquipment> EQUIPMENT = List.of(
        new DemoEquipment("Mahindra 575 DI Tractor", "TRACTOR", "Mahindra", "575 DI", 2022,
            "52 HP tractor with rotavator hitch, ideal for ploughing and field preparation.", "Sinnar", "Nashik", 140000, 25000, true, "{\"horsepower\":52,\"fuel\":\"Diesel\"}"),
        new DemoEquipment("Swaraj 744 FE Tractor", "TRACTOR", "Swaraj", "744 FE", 2021,
            "Reliable 48 HP tractor for tilling, hauling, and sowing work.", "Dindori", "Nashik", 165000, 30000, true, "{\"horsepower\":48,\"fuel\":\"Diesel\"}"),
        new DemoEquipment("Preet 987 Harvester", "HARVESTER", "Preet", "987", 2023,
            "Combine harvester for wheat and paddy with an experienced operator available.", "Niphad", "Nashik", 320000, 65000, true, "{\"cuttingWidthFeet\":7,\"crop\":\"Wheat, paddy\"}"),
        new DemoEquipment("Fieldking Rotavator", "ROTAVATOR", "Fieldking", "FKRTMG-200", 2022,
            "Two metre rotavator for seedbed preparation and residue mixing.", "Ojhar", "Nashik", 95000, 15000, false, "{\"workingWidthMetres\":2,\"blades\":48}"),
        new DemoEquipment("Shaktiman Sprayer", "SPRAYER", "Shaktiman", "Boom 600", 2024,
            "Tractor mounted boom sprayer for uniform crop protection coverage.", "Sinnar", "Nashik", 60000, 10000, false, "{\"tankLitres\":600,\"boomMetres\":12}"),
        new DemoEquipment("Mahindra 2BFX Seed Drill", "SEED_DRILL", "Mahindra", "2BFX", 2023,
            "Seed drill with fertilizer attachment for timely sowing.", "Nashik", "Nashik", 110000, 18000, false, "{\"rows\":9,\"use\":\"Sowing\"}"),
        new DemoEquipment("John Deere Cultivator", "CULTIVATOR", "John Deere", "9 Tyne", 2021,
            "Nine tine cultivator for inter-row cultivation and weed control.", "Pimpalgaon", "Nashik", 85000, 15000, false, "{\"tines\":9,\"use\":\"Cultivation\"}"),
        new DemoEquipment("Farm Utility Trailer", "OTHER", "Local make", "4 tonne", 2022,
            "Farm trailer for moving produce, compost, and supplies.", "Yeola", "Nashik", 70000, 12000, false, "{\"capacityTonnes\":4,\"use\":\"Transport\"}")
    );

    private final JdbcTemplate db;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(JdbcTemplate db, PasswordEncoder passwordEncoder) {
        this.db = db;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        var existing = db.queryForList("SELECT id,role FROM users WHERE email=?", DEMO_OWNER_EMAIL);
        String ownerId;
        if (existing.isEmpty()) {
            ownerId = UUID.randomUUID().toString();
            db.update("INSERT INTO users(id,email,password_hash,full_name,role,village,district,state) VALUES(?,?,?,?,?,?,?,?)",
                ownerId, DEMO_OWNER_EMAIL, passwordEncoder.encode(DEMO_OWNER_PASSWORD), "Sarathi Demo Owner",
                "OWNER", "Sinnar", "Nashik", "Maharashtra");
        } else {
            ownerId = String.valueOf(existing.get(0).get("id"));
            if (!"OWNER".equals(existing.get(0).get("role"))) {
                throw new IllegalStateException("The reserved local demo account must have the OWNER role.");
            }
        }
        seedDemoOperator();
        Integer count = db.queryForObject("SELECT COUNT(*) FROM equipment WHERE owner_id=?", Integer.class, ownerId);
        if (count != null && count > 0) return;
        for (DemoEquipment item : EQUIPMENT) {
            db.update("INSERT INTO equipment(id,owner_id,name,type,brand,model,manufacturing_year,description,specifications,photos,village,district,state,price_per_day_paise,transport_fee_paise,operator_available) VALUES(?,?,?,?,?,?,?,?,CAST(? AS JSON),JSON_ARRAY(),?,?, 'Maharashtra',?,?,?)",
                UUID.randomUUID().toString(), ownerId, item.name(), item.type(), item.brand(), item.model(), item.year(),
                item.description(), item.specifications(), item.village(), item.district(), item.dailyPricePaise(),
                item.transportPaise(), item.operator());
        }
    }
    private void seedDemoOperator() {
        var existing = db.queryForList("SELECT id,role FROM users WHERE email=?", DEMO_OPERATOR_EMAIL);
        String operatorId;
        if (existing.isEmpty()) {
            operatorId = UUID.randomUUID().toString();
            db.update("INSERT INTO users(id,email,password_hash,full_name,role,village,district,state) VALUES(?,?,?,?,?,?,?,?)",
                operatorId, DEMO_OPERATOR_EMAIL, passwordEncoder.encode(DEMO_OPERATOR_PASSWORD), "Kiran Patil",
                "OPERATOR", "Sinnar", "Nashik", "Maharashtra");
        } else {
            operatorId = String.valueOf(existing.get(0).get("id"));
            if (!"OPERATOR".equals(existing.get(0).get("role"))) {
                throw new IllegalStateException("The reserved local demo account must have the OPERATOR role.");
            }
        }
        db.update("INSERT INTO operator_profiles(user_id,skills,daily_rate_paise,service_area_km,experience_years,available) VALUES(?,CAST(? AS JSON),?,?,?,TRUE) ON DUPLICATE KEY UPDATE skills=VALUES(skills),daily_rate_paise=VALUES(daily_rate_paise),service_area_km=VALUES(service_area_km),experience_years=VALUES(experience_years),available=TRUE",
            operatorId, "[\"TRACTOR\",\"HARVESTER\",\"ROTAVATOR\",\"SPRAYER\",\"SEED_DRILL\",\"CULTIVATOR\"]", 75000, 40, 6);
    }
}
