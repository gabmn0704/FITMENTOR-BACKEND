package com.fitmentor.api;

import com.fitmentor.domain.routine.AdaptiveRoutine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrainingController {
    private final AdaptiveRoutine routine;

    public TrainingController(AdaptiveRoutine routine) { this.routine = routine; }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return new DashboardResponse("Buenos dias, Alex", 4, 87.0, 6, routine.build("INTERMEDIATE"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/routines")
    public java.util.List<String> routines(@RequestParam(defaultValue = "BEGINNER") String level) {
        return routine.build(level);
    }
}
