package com.fitmentor.api;

import com.fitmentor.domain.exercise.Exercise;
import com.fitmentor.domain.exercise.ExerciseCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exercise catalog and progressions. Requires authentication. */
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseCatalog catalog;

    public ExerciseController(ExerciseCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ExerciseResponse> search(@RequestParam(name = "q", required = false) String query) {
        return catalog.search(query).stream().map(ExerciseResponse::from).toList();
    }

    @GetMapping("/next")
    public List<ProgressionResponse> next(@RequestParam("from") String from) {
        return catalog.next(from).stream()
            .map(progression -> new ProgressionResponse(
                progression.exercise().id(),
                progression.exercise().label(),
                progression.exercise().muscle(),
                progression.exercise().difficulty().name(),
                progression.distance()))
            .toList();
    }

    /** Exercise as exposed by the API. */
    public record ExerciseResponse(String id, String label, String muscle, String difficulty) {
        static ExerciseResponse from(Exercise exercise) {
            return new ExerciseResponse(exercise.id(), exercise.label(), exercise.muscle(), exercise.difficulty().name());
        }
    }

    /** Progression step: an exercise plus its distance from the starting one. */
    public record ProgressionResponse(String id, String label, String muscle, String difficulty, int distance) {
    }
}
