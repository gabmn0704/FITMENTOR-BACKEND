package com.fitmentor.domain.exercise;

import java.util.List;

/**
 * Concrete products of the Factory Method pattern. Each nested class is created only
 * through {@link ExerciseFactory}, which keeps construction in one place.
 */
public final class Exercises {

    private Exercises() {
    }

    public static final class Squat extends Exercise {
        public String id() { return "squat"; }
        public String label() { return "Squat"; }
        public String muscle() { return "legs"; }
        public Difficulty difficulty() { return Difficulty.INTERMEDIATE; }
        public double met() { return 5.0; }
        public List<AngleRule> postureRules() {
            return List.of(
                new AngleRule("left_hip", "left_knee", "left_ankle", 70, 110, "Lower your hips until your thighs are near parallel."),
                new AngleRule("left_shoulder", "left_hip", "left_knee", 40, 110, "Keep your chest up and your back straight.")
            );
        }
    }

    public static final class PushUp extends Exercise {
        public String id() { return "push_up"; }
        public String label() { return "Push-up"; }
        public String muscle() { return "chest"; }
        public Difficulty difficulty() { return Difficulty.BEGINNER; }
        public double met() { return 8.0; }
        public List<AngleRule> postureRules() {
            return List.of(
                new AngleRule("left_shoulder", "left_elbow", "left_wrist", 60, 100, "Bend your elbows to about 90 degrees."),
                new AngleRule("left_shoulder", "left_hip", "left_ankle", 160, 190, "Keep your body in a straight line.")
            );
        }
    }

    public static final class Plank extends Exercise {
        public String id() { return "plank"; }
        public String label() { return "Plank"; }
        public String muscle() { return "core"; }
        public Difficulty difficulty() { return Difficulty.BEGINNER; }
        public double met() { return 3.8; }
        public List<AngleRule> postureRules() {
            return List.of(
                new AngleRule("left_shoulder", "left_hip", "left_ankle", 160, 190, "Do not let your hips sag or pike up."),
                new AngleRule("left_hip", "left_shoulder", "left_elbow", 70, 110, "Keep your elbows under your shoulders.")
            );
        }
    }

    public static final class Deadlift extends Exercise {
        public String id() { return "deadlift"; }
        public String label() { return "Deadlift"; }
        public String muscle() { return "back"; }
        public Difficulty difficulty() { return Difficulty.ADVANCED; }
        public double met() { return 6.0; }
        public List<AngleRule> postureRules() {
            return List.of(
                new AngleRule("left_shoulder", "left_hip", "left_knee", 150, 180, "Keep your back flat; do not round it."),
                new AngleRule("left_hip", "left_knee", "left_ankle", 100, 150, "Hinge at the hips with a slight knee bend.")
            );
        }
    }

    public static final class Lunge extends Exercise {
        public String id() { return "lunge"; }
        public String label() { return "Lunge"; }
        public String muscle() { return "legs"; }
        public Difficulty difficulty() { return Difficulty.INTERMEDIATE; }
        public double met() { return 4.0; }
        public List<AngleRule> postureRules() {
            return List.of(
                new AngleRule("left_hip", "left_knee", "left_ankle", 75, 115, "Bend the front knee to about 90 degrees."),
                new AngleRule("left_shoulder", "left_hip", "left_knee", 150, 180, "Keep your torso upright.")
            );
        }
    }
}
