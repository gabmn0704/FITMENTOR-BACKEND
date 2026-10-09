# FitMentor Backend

Spring Boot API and WebSocket service. The `mediapipe` pose strategy sends normalized landmarks to the FitMentor IA service over HTTP and returns its score and coaching cues through `/topic/feedback`.

## Development

```bash
mvn spring-boot:run
```

The API listens on port `8080`; the WebSocket/STOMP endpoint is `/ws`. Set `AI_SERVICE_URL` to the pose service URL. The development default is `http://localhost:8000`.

## Endpoints

- `GET /api/health`
- `GET /api/dashboard`
- `GET /api/routines?level=BEGINNER`
- STOMP endpoint `/ws`, application destination `/app/pose`, feedback topic `/topic/feedback`

The REST fallback returns a zero score and an explicit unavailable message if the IA service cannot be reached. It does not report an unverified form score.