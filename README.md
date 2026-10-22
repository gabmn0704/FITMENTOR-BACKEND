# FitMentor Backend (Java 21 · Spring Boot 3.4)

API REST y canal WebSocket/STOMP de FitMentor: autenticación (Google e invitado), sesiones de entrenamiento, estadísticas, rutina adaptativa, logros, catálogo de ejercicios, análisis de postura en tiempo real y coach. Persiste en PostgreSQL (H2 en desarrollo) y delega el análisis de visión y el coach en el servicio de IA (FITMENTOR-IA).

## Arquitectura

```
Frontend ──HTTP /api──► api (controllers, errores) ──► training (TrainingFacade)
   │                                                     ├─► domain (cálculo puro: calorías, stats, rutina, logros)
   │                                                     ├─► user / auth / security (JWT)
   │                                                     └─► persistence (JPA: users, training_sessions, ...)
   └──STOMP /ws──► ws (TrainingSocketController, LiveTrainingService)
                      ├─► domain.pose.validation (cadena de frames)
                      ├─► domain.pose (Strategy: AI o local)  ──► ai (AiServiceClient, Adapter) ──► FITMENTOR-IA
                      └─► domain.session (LiveTraining: State, Command, Memento, Observer)
```

Paquetes:

| Paquete | Responsabilidad |
| --- | --- |
| `api` | Controladores REST y manejo global de errores `{error, message}` |
| `auth`, `security`, `user` | Login Google/invitado, JWT HS256, perfil `/api/me` |
| `training` | Sesiones, estadísticas, rutina y logros; `TrainingFacade` |
| `domain` | Lógica pura sin Spring: ejercicios, pose, sesión, rutina, stats, logros |
| `ai` | Cliente HTTP al servicio de IA (adaptador) y estrategia de IA |
| `ws` | Canal STOMP por sesión, observadores y decoradores |
| `coach` | Coach con respuesta de IA o reglas offline |

## Patrones de diseño

Los cinco patrones del caso de estudio (Strategy, State, Observer, Command, Template Method) se mejoraron y tienen uso real en el flujo. Se añadieron Factory Method, Builder, Facade, Decorator, Chain of Responsibility, Adapter, Memento y Singleton (vía Spring).

| Patrón | Clase(s) | Por qué / dónde se usa |
| --- | --- | --- |
| Strategy | `PoseAnalysisStrategy`, `AiServicePoseStrategy`, `MediaPipePoseStrategy`, `PoseAnalyzer` | El análisis de postura cambia según disponibilidad: la IA es la fuente de verdad (reps y fase); si no responde, se usa el analizador local sin contar reps. |
| State | `TrainingState`, `TrainingStates`, `TrainingContext` | Una sesión está en `WARMUP`, `ACTIVE_SET`, `REST`, `PAUSED` o `FINISHED`; cada control significa algo distinto según el estado. `PAUSED` recuerda a qué estado volver. |
| Observer | `FeedbackSubject`, `FeedbackObserver`, `StompFeedbackObserver` | Cada sesión en vivo notifica feedback y cambios de estado a sus suscriptores sin acoplarse al broker. |
| Command | `TrainingCommand`, `TrainingCommands`, `TrainingCommandInvoker` | `pause`, `resume`, `skip`, `finish` son objetos ejecutables y registrables. |
| Template Method | `RoutineTemplate` (final `build`), `AdaptiveRoutine` | El algoritmo de rutina es fijo (validar, elegir ejercicios, personalizar, descanso, explicar); la política adaptativa sólo implementa los pasos. |
| Factory Method | `ExerciseFactory`, `Exercise`, `Exercises.*` | Crear un ejercicio por id delega en el creador registrado; añadir uno no toca catálogo ni analizador. |
| Builder | `SessionSummary.Builder` | Resumen de sesión inmutable con invariantes validados (reps ≥ 0, precisión 0–100). |
| Facade | `TrainingFacade` | Una sola puerta para sesiones, stats, rutina y logros; oculta repositorios y cálculos. |
| Decorator | `FeedbackChannel`, `LoggingFeedbackChannel`, `DuplicateSuppressingFeedbackChannel` | Añade registro (sólo destinos, nunca payloads) y supresión de mensajes idénticos sobre el canal STOMP. |
| Chain of Responsibility | `FrameHandler`, `ConfidenceHandler`, `RateLimitHandler`, `SizeHandler`, `FrameValidationChain` | Valida cada frame: confianza → límite de 150 ms por sesión → tamaño; el primero que rechaza corta la cadena. |
| Adapter | `AiServiceClient` | Traduce la API HTTP de la IA (`/analyze`, `/coach`, timeout 1500 ms) a métodos Java que devuelven `Optional`. |
| Memento | `TrainingContext.Memento`, `TrainingCommandInvoker` | Antes de cada comando se guarda una instantánea; `undo` la restaura (historial acotado a 20). |
| Singleton (Spring) | `LiveTrainingRegistry`, `FrameValidationChain`, `JwtService` | Un único bean por aplicación: el estado de las sesiones en vivo y la limitación de tasa se comparten entre conexiones. |

Cada clase lleva javadoc que indica el patrón y su motivo, y tiene pruebas JUnit (`src/test/java`).

## Contrato REST

Base: `/api`. Salvo los públicos, todas las rutas exigen `Authorization: Bearer <jwt>`; sin token responden `401 {"error":"unauthorized","message":"..."}`.

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/health` | Público. `{"status":"ok"}` |
| GET | `/actuator/health` | Público. Sonda de salud para Render. |
| POST | `/api/auth/google` | `{credential}` → `{token, user}`. Verifica el ID token de Google contra `GOOGLE_CLIENT_ID`. |
| POST | `/api/auth/guest` | `{name?}` → `{token, user}`. Invitado con email `guest-<uuid>@guest.fitmentor.local`. |
| GET | `/api/me` | Perfil del usuario. |
| PATCH | `/api/me` | `{name?, level?, goal?}`. `level`: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`. |
| POST | `/api/sessions` | `{exercise, reps, accuracy, durationSeconds, avgAngles?}` → `201` con la sesión y sus calorías (MET × 70 kg × horas). |
| GET | `/api/sessions?limit=20` | Últimas sesiones (1–100). |
| GET | `/api/stats` | `{totalSessions, totalReps, totalCalories, avgAccuracy, streakDays, weekly[7], byExercise[], level}`. Días en UTC. |
| GET | `/api/routine` | `{level, restSeconds, items:[{exercise, sets, reps, note}], reason}`. Se adapta a la precisión media y a la constancia semanal. |
| GET | `/api/achievements` | `[{id, title, description, unlocked, progress}]` con progreso 0–100. |
| GET | `/api/exercises?q=` | Catálogo, filtro sin tildes por id, nombre o músculo. |
| GET | `/api/exercises/next?from=squat` | Progresiones más difíciles, con `distance`. |
| POST | `/api/coach` | `{question (≤500), context?}` → `{answer, source}`, `source` = `ai` o `rules`. |

Errores: `{error, message}` sin trazas. Códigos habituales: `validation_error` (400), `bad_request` (400), `unknown_exercise` (400), `unauthorized` (401), `invalid_google_token` (401), `not_found` (404), `google_not_configured` / `google_unavailable` (503).

## Contrato WebSocket (STOMP)

- Endpoint: `/ws` (SockJS no se usa; WebSocket nativo). CONNECT admite `Authorization: Bearer <jwt>` opcional; sin token la conexión es anónima; con token inválido se rechaza.
- `SEND /app/pose` con `{exercise, points:[{name,x,y,confidence}], frame_width, frame_height, session_id}`.
  - Respuesta sólo a `/topic/feedback.{session_id}`: `{type:"feedback", score, message, feedback:[...], phase, reps, state, exercise}`.
  - Frames con confianza baja, a menos de 150 ms del anterior de la misma sesión, o de tamaño inválido se descartan en silencio.
- `SEND /app/command` con `{session_id, name}` donde `name` ∈ `pause | resume | skip | finish | undo`.
  - Respuesta a `/topic/state.{session_id}`: `{type:"state", state, exercise}`, con `state` ∈ `WARMUP | ACTIVE_SET | REST | PAUSED | FINISHED`.
- Las sesiones en vivo sin actividad durante 30 minutos se eliminan.

## Variables de entorno

| Variable | Obligatoria | Descripción |
| --- | --- | --- |
| `PORT` | No (8080) | Puerto HTTP. |
| `SPRING_PROFILES_ACTIVE` | No | `render` para PostgreSQL. Vacío para desarrollo con H2. |
| `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD` | Sólo perfil `render` | Conexión PostgreSQL. |
| `JWT_SECRET` | Sí en `prod`/`render` (≥ 32 caracteres) | Clave HS256. En desarrollo se genera una efímera y se avisa en el log. |
| `GOOGLE_CLIENT_ID` | Sí para login Google | Audiencia de los ID tokens de Google. |
| `FRONTEND_ORIGIN` | No (`*`) | Origen permitido en CORS y en el handshake WebSocket. |
| `AI_SERVICE_URL` | No (`http://localhost:8000`) | URL base del servicio FITMENTOR-IA. |

Ver `.env.example`. No subas `.env` al repositorio.

## Desarrollo

Requisitos: Java 21. Maven no hace falta: usa el wrapper.

```bash
./mvnw verify          # pruebas y empaquetado
./mvnw spring-boot:run # arranca en http://localhost:8080 con H2 en memoria
```

Ejemplo rápido:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/guest -H 'Content-Type: application/json' -d '{"name":"Ana"}' | jq -r .token)
curl -s localhost:8080/api/stats -H "Authorization: Bearer $TOKEN"
```

## Pruebas

- Unitarias: cálculo de calorías, stats, rutina (Template Method), logros, ejercicios (Factory Method), máquina de estados, invocador de comandos con Memento, cadena de validación de frames, análisis local, JWT.
- Integración: flujo completo de sesiones con MockMvc (incluido aislamiento entre usuarios), autenticación Google simulada, cliente STOMP real contra el servidor embebido, cliente de IA con un servidor HTTP de prueba.

## Despliegue (Render)

1. Crea el servicio desde `render.yaml` (Blueprint). Crea también la base PostgreSQL `fitmentor-db`.
2. Define `GOOGLE_CLIENT_ID`, `FRONTEND_ORIGIN` y `AI_SERVICE_URL` en el panel. `JWT_SECRET` se genera solo.
3. El `Dockerfile` es multi-etapa: compila con `./mvnw` en un JDK 21 y ejecuta el jar en un JRE 21 como usuario no root. El health check es `/actuator/health`.
4. El esquema lo gestiona JPA (`ddl-auto: update`) y es compatible con las columnas de FITMENTOR-BD (`feat-schema-v2`).

CI: `.github/workflows/ci.yml` ejecuta `./mvnw verify` con Java 21 en cada push y PR a `main`.
