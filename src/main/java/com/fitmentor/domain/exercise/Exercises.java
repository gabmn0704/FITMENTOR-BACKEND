package com.fitmentor.domain.exercise;

import java.util.List;

/**
 * Concrete products of {@link ExerciseFactory}. Each nested class is created only
 * through the factory, which keeps construction in one place.
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
                new AngleRule("left_hip", "left_knee", "left_ankle", 70, 110, "Baja la cadera hasta que los muslos queden casi paralelos al suelo."),
                new AngleRule("left_shoulder", "left_hip", "left_knee", 40, 110, "Mantén el pecho arriba y la espalda recta.")
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
                new AngleRule("left_shoulder", "left_elbow", "left_wrist", 60, 100, "Flexiona los codos hasta unos 90 grados."),
                new AngleRule("left_shoulder", "left_hip", "left_ankle", 160, 190, "Mantén el cuerpo en línea recta.")
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
                new AngleRule("left_shoulder", "left_hip", "left_ankle", 160, 190, "No dejes que la cadera caiga ni suba."),
                new AngleRule("left_hip", "left_shoulder", "left_elbow", 70, 110, "Mantén los codos debajo de los hombros.")
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
                new AngleRule("left_shoulder", "left_hip", "left_knee", 150, 180, "Mantén la espalda neutra, sin redondearla."),
                new AngleRule("left_hip", "left_knee", "left_ankle", 100, 150, "Lleva la cadera atrás con una ligera flexión de rodillas.")
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
                new AngleRule("left_hip", "left_knee", "left_ankle", 75, 115, "Flexiona la rodilla delantera hasta unos 90 grados."),
                new AngleRule("left_shoulder", "left_hip", "left_knee", 150, 180, "Mantén el torso erguido.")
            );
        }
    }
}
