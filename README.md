# RED-SOCIAL

Monorepo **100% Java** como base para una red social, listo para expandir. Consta de un
**backend REST (Spring Boot)** con autenticación JWT y chat en tiempo real por WebSocket, y un
**frontend web (Vaadin)** que consume esa API. Ambos módulos comparten los mismos DTOs.

## Arquitectura

```
RED-SOCIAL/
├── pom.xml                  # POM padre (agregador + dependencyManagement)
├── mvnw / mvnw.cmd          # Maven Wrapper (no necesitás Maven instalado)
├── docker-compose.yml       # PostgreSQL (+ pgAdmin opcional)
├── red-social-common/       # DTOs/contratos compartidos (records Java)
├── red-social-api/          # Backend REST: Spring Boot + JPA + Security JWT + WebSocket/STOMP
│   └── src/main/java/com/redsocial/api/
│       ├── config/          # WebSocket, OpenAPI, Seeder
│       ├── controller/      # Auth, Users, Posts, Feed, Chat (REST)
│       ├── domain/          # Entidades JPA
│       ├── mapper/          # Entidad -> DTO
│       ├── repository/      # Spring Data JPA
│       ├── security/        # JWT + Spring Security
│       ├── service/         # Lógica de negocio
│       └── websocket/       # STOMP: ChatStompController + auth
└── red-social-ui/           # Frontend: Vaadin 24 (Spring Boot)
    └── src/main/java/com/redsocial/ui/
        ├── client/          # ApiService (RestClient contra el backend)
        ├── component/       # PostCard, CommentDialog, ProfileContent
        ├── session/         # UserSession (JWT por sesión de Vaadin)
        ├── util/            # TimeFormat
        └── view/            # Login, Register, Feed, Explore, Chat, Profile
```

### Funcionalidades incluidas
- **Usuarios**: registro, login con JWT, perfiles (bio, avatar), edición de perfil.
- **Social**: seguir / dejar de seguir, contadores de seguidores y seguidos, búsqueda de usuarios.
- **Publicaciones**: crear, feed (propio + de seguidos), likes, comentarios, eliminar.
- **Chat**: conversaciones 1 a 1, mensajes, contador de no leídos y marcado como leído.
- **Tiempo real**: WebSocket/STOMP en el backend; la UI de Vaadin refresca por sondeo corto (2 s).

## Requisitos
- **JDK 21** (probado con Temurin 21).
- **Docker** (para PostgreSQL) — alternativa sin Docker: perfil `dev` con H2 en memoria.
- Node **no** es necesario instalarlo: Vaadin lo descarga automáticamente la primera vez.

## Cómo ejecutar

### 1. Base de datos (PostgreSQL)
```bash
cp .env.example .env          # opcional: ajustar credenciales
docker compose up -d postgres
```
Opcional (interfaz web de la base): `docker compose --profile tools up -d` → http://localhost:5050

> El contenedor se publica en el puerto **55432** del host (y no en el 5432) para no chocar con una
> instalación local de PostgreSQL. Cambiable con `DB_PORT` en `.env`.

### 2. Backend (API REST) — puerto **8080**
```bash
# con PostgreSQL
./mvnw -pl red-social-api -am spring-boot:run

# sin Docker (H2 en memoria, con datos de ejemplo)
./mvnw -pl red-social-api -am spring-boot:run -Dspring-boot.run.profiles=dev
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- Consola H2 (solo perfil dev): http://localhost:8080/h2-console

### 3. Frontend (Vaadin) — puerto **8081**
```bash
./mvnw -pl red-social-ui -am spring-boot:run
```
Abrir http://localhost:8081

> En Windows usá `mvnw.cmd` en lugar de `./mvnw`.

### Usuarios de ejemplo (perfil `dev` / `app.seed.enabled=true`)
| usuario | contraseña    |
|---------|---------------|
| alice   | password123   |
| bob     | password123   |
| carol   | password123   |

## Configuración

Variables de entorno principales (ver `application.yml` de cada módulo):

| Variable | Módulo | Default | Descripción |
|---|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | api | 127.0.0.1 / 55432 / redsocial / redsocial / redsocial | Conexión PostgreSQL |
| `JWT_SECRET` | api | *(valor de desarrollo)* | **Cambiar en producción** (mín. 32 caracteres) |
| `JWT_EXPIRATION_MS` | api | 86400000 | Vigencia del token (24 h) |
| `CORS_ORIGINS` | api | http://localhost:8081 | Orígenes permitidos |
| `API_BASE_URL` | ui | http://localhost:8080/api | URL del backend |
| `API_PORT` / `UI_PORT` | api / ui | 8080 / 8081 | Puertos |

Perfiles del backend: `dev` (H2 + seed) o por defecto (PostgreSQL).

## API REST (resumen)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registro |
| POST | `/api/auth/login` | Login (devuelve JWT) |
| GET | `/api/users/me` · PUT `/api/users/me` | Perfil propio / editar |
| GET | `/api/users/{username}` | Perfil público |
| GET | `/api/users/search?q=` | Buscar usuarios |
| POST/DELETE | `/api/users/{username}/follow` | Seguir / dejar de seguir |
| GET | `/api/users/{username}/followers` · `/following` | Listas |
| GET | `/api/feed` | Feed del usuario |
| POST/GET/DELETE | `/api/posts` · `/api/posts/{id}` | Crear / obtener / eliminar |
| POST/DELETE | `/api/posts/{id}/like` | Dar / quitar like |
| GET/POST | `/api/posts/{id}/comments` | Comentarios |
| GET | `/api/conversations` | Conversaciones del usuario |
| POST | `/api/conversations` | Iniciar conversación |
| GET/POST | `/api/conversations/{id}/messages` | Mensajes |
| POST | `/api/conversations/{id}/read` | Marcar leído |

### WebSocket (STOMP)
- Endpoint: `ws://localhost:8080/ws` (o SockJS `/ws`)
- Autenticación: header `Authorization: Bearer <token>` en el frame `CONNECT`
- Enviar mensaje: `SEND /app/chat.send` con `{ "conversationId": 1, "content": "hola" }`
- Suscribirse: `SUBSCRIBE /topic/conversations.{id}`

## Compilar y testear
```bash
./mvnw clean verify          # compila todos los módulos + tests
./mvnw -pl red-social-api -am test
```

## Próximos pasos sugeridos (expansión)
- Migraciones versionadas con Flyway/Liquibase (hoy `ddl-auto=update`).
- Refresh tokens y roles/permisos más granulares (ADMIN).
- Subida de archivos (avatar/imágenes de posts) a almacenamiento local/S3.
- Notificaciones (nuevo seguidor, like, comentario, mensaje).
- Grupos/“comunidades” y chat grupal (el modelo de conversación ya lo soporta).
- Paginación por cursor e índices para escalar el feed.
- Push nativo en la UI de Vaadin (`@Push`) o cliente STOMP para chat 100% en tiempo real.
- Dockerfiles + reverse proxy (Nginx) y CI/CD.
