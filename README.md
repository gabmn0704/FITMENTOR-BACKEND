# FitMentor Backend (Java 21 · Spring Boot 3.4)

API REST y canal WebSocket/STOMP de FitMentor: autenticación (Google e invitado), sesiones de entrenamiento, estadísticas, rutina adaptativa, logros, catálogo de ejercicios, análisis de postura en tiempo real y coach. Persiste en PostgreSQL (H2 en desarrollo) y delega el análisis de visión y el coach en el servicio de IA (FITMENTOR-IA).

## Arquitectura

```
Frontend ──HTTP /api──► api (controllers, errores) ──► training (TrainingFacade)
   │                                                     ├─► domain (cálculo puro: calorías, stats, rutina, logros)
   │                                                     ├─► user / auth / security (JWT, límite de peticiones)
   │                                                     └─► persistence (JPA: users, training_sessions, ...)
   └──STOMP /ws──► ws (TrainingSocketController, LiveTrainingService)
                      ├─► domain.pose.validation (cadena de frames)
                      ├─► domain.pose (Strategy: AI o local)  ──► ai (AiServiceClient, Adapter) ──► FITMENTOR-IA
                      └─► domain.session (LiveTraining: State, Command, Memento, Observer)
```

| Paquete | Responsabilidad |
| --- | --- |
| `api` | Controladores REST y manejo global de errores `{error, message}` |
| `auth`, `security`, `user` | Login Google/invitado, JWT HS256, límite de peticiones, perfil `/api/me` |
| `training` | Sesiones, estadísticas, rutina y logros; `TrainingFacade` |
| `domain` | Lógica pura sin Spring: ejercicios, pose, sesión, rutina, stats, logros |
| `ai` | Cliente HTTP al servicio de IA (adaptador) y estrategia de IA |
| `ws` | Canal STOMP por sesión, observadores, decoradores y control de acceso a destinos |
| `coach` | Coach con respuesta de la IA (Claude o reglas) o reglas locales |

## Patrones de diseño

Los cinco patrones del caso de estudio (Strategy, State, Observer, Command, Template Method) tienen uso real en el flujo, y se añadieron ocho más. La tabla indica también los límites de cada uso, para no exagerar.

| Patrón | Clase(s) | Propósito real | Límites (nota honesta) |
| --- | --- | --- | --- |
| Strategy | `PoseAnalysisStrategy`, `AiServicePoseStrategy`, `MediaPipePoseStrategy`, `PoseAnalyzer` | Analizar cada frame con la IA (fuente de verdad de reps y fase) o con el analizador local cuando la IA falla. El cambio ocurre en tiempo de ejecución, frame a frame. | El socket siempre pide `ai-service`; esa estrategia compone la local como respaldo. `MediaPipePoseStrategy` analiza landmarks de MediaPipe, no ejecuta MediaPipe. |
| State | `TrainingState`, `TrainingStates`, `TrainingContext` | `WARMUP`, `ACTIVE_SET`, `REST`, `PAUSED`, `FINISHED`: cada control (`skip`, `pause`…) significa algo distinto según el estado; `PAUSED` recuerda a cuál volver. | Los estados son inmutables y sin datos, salvo `PAUSED`. |
| Observer | `FeedbackSubject`, `FeedbackObserver`, `StompFeedbackObserver` | Cada sesión en vivo publica feedback y cambios de estado sin conocer el broker STOMP. | En producción hay un solo observador por sesión (el de STOMP); los tests suscriben varios. |
| Command | `TrainingCommand`, `TrainingCommands`, `TrainingCommandInvoker` | `pause`, `resume`, `skip`, `finish` llegan por `/app/command` y se ejecutan como objetos registrados en un historial. | `undo` no es un comando: lo resuelve el invocador con Memento. |
| Template Method | `RoutineTemplate` (`build` final), `AdaptiveRoutine` | Algoritmo fijo de `/api/routine` (validar, elegir ejercicios, personalizar, descanso, explicar); la política adaptativa implementa los pasos. | Hay una sola subclase concreta. |
| Factory (registro de creadores) | `ExerciseFactory`, `Exercise`, `Exercises.*` | Crear ejercicios por id (catálogo, analizador local, calorías) sin `new` ni `switch`; añadir uno es una clase y un registro. | Variante "fábrica parametrizada" con `Supplier`; no es la forma GoF de Factory Method con subclases creadoras. |
| Builder | `SessionSummary.Builder` | Resumen de sesión inmutable con invariantes validados en `build()` (reps ≥ 0, precisión 0–100). | — |
| Facade | `TrainingFacade` | Única puerta de controladores y coach hacia sesiones, stats, rutina y logros; oculta repositorios y calculadoras. | — |
| Decorator | `FeedbackChannel`, `FeedbackChannelDecorator`, `LoggingFeedbackChannel`, `DuplicateSuppressingFeedbackChannel`, `StompFeedbackChannel` | Apila registro (sólo destinos, nunca payloads) y supresión de mensajes idénticos sobre el canal STOMP. | — |
| Chain of Responsibility | `FrameHandler`, `ConfidenceHandler`, `RateLimitHandler`, `SizeHandler`, `FrameValidationChain` | Valida cada frame: confianza → 150 ms por sesión → tamaño; el primero que rechaza corta la cadena. | — |
| Adapter | `AiServiceClient`; `GoogleIdTokenVerifierAdapter` (puerto `GoogleTokenVerifier`) | Traduce la API HTTP de la IA y la librería de Google a métodos Java con errores propios (`Optional`, `ApiException`). | — |
| Memento | `TrainingContext.Memento`, `TrainingCommandInvoker` | Antes de cada comando se guarda una instantánea; `undo` la restaura (historial acotado a 20). | — |
| Singleton | `LiveTrainingRegistry`, `FrameValidationChain`, `JwtService` (beans singleton de Spring); `TrainingStates.*.INSTANCE` | Un único registro de sesiones en vivo y un único estado de limitación compartidos por todas las conexiones. | Es el ámbito singleton del contenedor, no un Singleton GoF escrito a mano; los estados sin datos se comparten como constantes. |

Otras técnicas que no se presentan como patrones GoF: `AiServiceClient` pausa las llamadas 5 s tras una caída de la IA (un "cool-down" simple, no un Circuit Breaker completo) y `RateLimitFilter` limita peticiones por cliente.

Cada clase lleva javadoc con el patrón y su motivo, y hay pruebas JUnit de cada uno (`src/test/java`).

## Contrato REST

Base: `/api`. Salvo los públicos, todas las rutas exigen `Authorization: Bearer <jwt>`; sin token responden `401 {"error":"unauthorized","message":"..."}`.

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/health` | Público. `{"status":"ok"}` |
| GET | `/actuator/health` | Público. Sonda de salud para Render. |
| POST | `/api/auth/google` | `{credential}` → `{token, user}`. Verifica firma, caducidad, emisor (`accounts.google.com`) y audiencia `GOOGLE_CLIENT_ID`. |
| POST | `/api/auth/guest` | `{name?}` (≤ 120, sin caracteres de control) → `{token, user}`. Invitado con email `guest-<uuid>@guest.fitmentor.local`. |
| POST | `/api/auth/register` | `{name (1–60), email (válido, ≤ 180), password (8–72 caracteres; ≤ 72 bytes UTF-8)}` → `{token, user}`. El correo se guarda en minúsculas. Correo repetido → `409 email_taken` («Ese correo ya está registrado»). La contraseña se guarda solo como hash BCrypt. |
| POST | `/api/auth/login` | `{email, password}` → `{token, user}`. Credenciales no válidas → `401 invalid_credentials` («Correo o contraseña incorrectos»), igual para correo inexistente, contraseña errónea o cuenta sin contraseña (Google o invitado). |
| GET | `/api/me` | Perfil del usuario. |
| PATCH | `/api/me` | `{name?, level?, goal?}`. `level`: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`. |
| POST | `/api/sessions` | `{exercise, reps, accuracy, durationSeconds, avgAngles?}` → `201` con la sesión y sus calorías (MET × 70 kg × horas). `avgAngles`: ≤ 20 claves de ≤ 40 caracteres, valores −360…360. |
| GET | `/api/sessions?limit=20` | Últimas sesiones (1–100). |
| GET | `/api/stats` | `{totalSessions, totalReps, totalCalories, avgAccuracy, streakDays, weekly[7], byExercise[], level}`. Días en UTC. |
| GET | `/api/routine` | `{level, restSeconds, items:[{exercise, sets, reps, note}], reason}`. Se adapta a la precisión media y a la constancia semanal. |
| GET | `/api/achievements` | `[{id, title, description, unlocked, progress}]` con progreso 0–100. |
| GET | `/api/exercises?q=` | Catálogo, filtro sin tildes por id, nombre o músculo. |
| GET | `/api/exercises/next?from=squat` | Progresiones más difíciles, con `distance`. |
| POST | `/api/coach` | `{question (≤ 500), context? (≤ 4000, se ignora)}` → `{answer, source}` con `source` = `claude` (respondió Claude a través de la IA) o `rules` (reglas de la IA o, si la IA no responde, reglas locales). El contexto que recibe la IA lo arma el servidor: nivel del usuario y su última sesión (ejercicio, precisión, reps). |

Errores: `{error, message}` sin trazas. Códigos habituales: `validation_error` (400), `bad_request` (400), `unknown_exercise` (400), `unauthorized` / `unknown_user` / `invalid_credentials` (401), `invalid_google_token` (401), `email_taken` (409), `not_found` (404), `rate_limited` (429, con `Retry-After`), `google_not_configured` / `google_unavailable` (503).

Límite de peticiones por IP y minuto: `POST /api/auth/**` 20 y `POST /api/coach` 30 (configurables).

## Contrato WebSocket (STOMP)

- Endpoint: `/ws` (WebSocket nativo, sin SockJS). CONNECT admite `Authorization: Bearer <jwt>` opcional; sin token la conexión es anónima; con token inválido se rechaza.
- Mensajes entrantes de hasta 8 KB. `session_id`: 1–100 caracteres `[A-Za-z0-9_-]` (el frontend usa UUID).
- `SEND /app/pose` con `{exercise, points:[{name,x,y,confidence}], frame_width, frame_height, session_id}`.
  - Respuesta sólo a `/topic/feedback.{session_id}`: `{type:"feedback", score, message, feedback:[...], phase, reps, state, exercise}`. Con la IA caída, `phase` es `null` y `reps` no avanza.
  - Frames con confianza baja, a menos de 150 ms del anterior de la misma sesión, o de tamaño inválido se descartan en silencio.
- `SEND /app/command` con `{session_id, name}` donde `name` ∈ `pause | resume | skip | finish | undo`.
  - Respuesta a `/topic/state.{session_id}`: `{type:"state", state, exercise}`, con `state` ∈ `WARMUP | ACTIVE_SET | REST | PAUSED | FINISHED`.
- Control de acceso: sólo se puede hacer SEND a `/app/pose` y `/app/command`, y SUBSCRIBE a `/topic/feedback.{id}` y `/topic/state.{id}` exactos (sin comodines). Una sesión creada por un usuario autenticado sólo la puede usar y observar ese usuario.
- Memoria acotada: como máximo 10 000 sesiones en vivo; las inactivas 30 minutos se eliminan junto con su estado de limitación.

## Base de datos

El esquema canónico es `FITMENTOR-BD/init.sql`. `src/main/resources/db/schema.sql` es una copia idéntica que el perfil `render` ejecuta en cada arranque (es idempotente): una base vacía queda creada y una existente se actualiza. Después Hibernate sólo valida (`ddl-auto: validate`) que las entidades coinciden con el esquema (`jsonb`, `numeric`, `uuid`, claves foráneas). Si cambias `init.sql`, copia el archivo:

```bash
cp ../FITMENTOR-BD/init.sql src/main/resources/db/schema.sql
```

En desarrollo (sin perfil) se usa H2 en memoria con `ddl-auto: update`.

## Variables de entorno

| Variable | Obligatoria | Descripción |
| --- | --- | --- |
| `PORT` | No (8080) | Puerto HTTP. |
| `SPRING_PROFILES_ACTIVE` | No | `render` para PostgreSQL. Vacío para desarrollo con H2. |
| `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD` | Sólo perfil `render` | Conexión PostgreSQL (Render las rellena desde la base). |
| `DATABASE_URL` | No | Sólo desarrollo sin perfil: URL **JDBC** (`jdbc:...`). No es la URL `postgres://` de Render. |
| `JWT_SECRET` | Sí en `prod`/`render` (≥ 32 caracteres) | Clave HS256. Tokens válidos 7 días. En desarrollo se genera una efímera y se avisa en el log. |
| `GOOGLE_CLIENT_ID` | Sí para login Google | Audiencia de los ID tokens de Google. |
| `FRONTEND_ORIGIN` | Sí en producción (por defecto `*`) | Orígenes permitidos (separados por comas) en CORS y en el handshake WebSocket. Con `*` se avisa en el log. |
| `AI_SERVICE_URL` | No | URL base de FITMENTOR-IA; sin esquema se antepone `http://` (formato `host:puerto` de Render). Local: `http://localhost:8000`. |
| `AI_SHARED_SECRET` | Sí si la IA la exige | Secreto que se envía en la cabecera `X-Internal-Token` en cada petición a `/analyze` y `/coach`. Debe ser idéntico a `AI_SHARED_SECRET` de FITMENTOR-IA. Vacío = no se envía. Nunca se registra en logs. Si la IA responde `401`, se usa el fallback local y se avisa en el log (sin mostrar el secreto). |
| `AI_COACH_TIMEOUT_MS` | No (9000) | Tiempo máximo de espera a `/coach` en milisegundos. La IA espera hasta 8 s a Claude; `/analyze` mantiene 1,5 s fijos. Cada operación tiene su propio enfriamiento tras un fallo, así que un fallo de `/coach` no pausa `/analyze`. |
| `RATE_LIMIT_AUTH_PER_MINUTE`, `RATE_LIMIT_COACH_PER_MINUTE` | No (20 / 30) | Límite de peticiones por IP. |
| `JPA_DDL_AUTO`, `SCHEMA_INIT_MODE` | No (`validate` / `always`) | Sólo perfil `render`; no cambiarlos salvo para diagnóstico. |

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

- Unitarias: calorías, stats, rutina (Template Method), logros, fábrica de ejercicios, máquina de estados, comandos con Memento, Observer y Decorator, Builder, cadena de validación, análisis local, JWT, adaptador de Google, límite de peticiones, control de acceso STOMP y límites de memoria.
- Registro y login con email y contraseña: unitarias (`AuthServiceTests`, límite BCrypt en bytes) e integración (`PasswordAuthIntegrationTests`: duplicados, credenciales erróneas, invitado y Google sin contraseña, validación).
- Integración (siempre): flujo de sesiones con MockMvc (aislamiento entre usuarios, usuario borrado), autenticación, cliente STOMP real contra el servidor embebido, respaldo local con la IA caída y cliente de IA contra un servidor HTTP de prueba (incluido el 422).
- Integración opcional contra servicios reales:

```bash
# PostgreSQL 16 real: aplica schema.sql, valida el mapeo y hace login, sesión con avgAngles (jsonb) y stats
FITMENTOR_TEST_PG_URL=jdbc:postgresql://127.0.0.1:5432/fitmentor FITMENTOR_TEST_PG_USER=fitmentor \
FITMENTOR_TEST_PG_PASSWORD=... ./mvnw test -Dtest=PostgresIntegrationTests

# FITMENTOR-IA levantado (uvicorn): sentadilla sintética por STOMP con score, fase y reps de la IA
FITMENTOR_TEST_AI_URL=http://127.0.0.1:8000 ./mvnw test -Dtest=AiServiceStompIntegrationTests
```

## Despliegue (Render)

1. Crea el servicio desde `render.yaml` (Blueprint), que también crea la base PostgreSQL `fitmentor-db`.
2. Define `GOOGLE_CLIENT_ID`, `FRONTEND_ORIGIN`, `AI_SERVICE_URL` y `AI_SHARED_SECRET` en el panel. `JWT_SECRET` se genera solo. `AI_SHARED_SECRET` debe coincidir con la del servicio de IA.
3. El `Dockerfile` es multi-etapa: compila con `./mvnw` en un JDK 21 y ejecuta el jar en un JRE 21 como usuario no root. El health check es `/actuator/health`.
4. Al arrancar se aplica `db/schema.sql` y Hibernate valida el mapeo. Si la base se creó antes con `ddl-auto: update` y las columnas no coinciden (p. ej. `accuracy` como `double precision`), el arranque falla con un error de validación: aplica `init.sql` sobre una base nueva o ajusta los tipos.
5. Render termina TLS en un proxy privado; el perfil `render` confía en sus cabeceras `X-Forwarded-*` para conocer la IP real.

CI: `.github/workflows/ci.yml` ejecuta `./mvnw verify` con Java 21 en cada push y PR a `main`.
