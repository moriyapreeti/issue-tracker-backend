# Issue Tracker – Backend (Spring Boot)

Jira-like issue tracker API. **Java 17 · Spring Boot 3 · Spring Security + JWT · JPA/Hibernate · PostgreSQL (or MySQL) · Swagger · WebSocket · Multipart upload.** No Lombok – plain getters/setters and Java records.

## Features
| Use case | Endpoint(s) |
|---|---|
| JWT auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/me` |
| Projects & team | `GET/POST /api/projects`, `PUT/DELETE /api/projects/{id}`, `POST/DELETE /api/projects/{id}/members/{userId}` |
| Tickets CRUD | `POST /api/tickets`, `GET /api/tickets/{id}`, `PUT /api/tickets/{id}`, `DELETE /api/tickets/{id}` |
| Assign | `PATCH /api/tickets/{id}/assign` `{ "assigneeId": 3 }` (null = unassign) |
| Kanban | `PATCH /api/tickets/{id}/status` `{ "status": "IN_PROGRESS" }` (TODO → IN_PROGRESS → DONE) |
| Filter & search | `GET /api/tickets?projectId=&status=&priority=&type=&assigneeId=&reporterId=&mine=&q=&page=&size=&sort=` |
| Comments | `GET/POST /api/tickets/{id}/comments`, `DELETE /api/comments/{id}` |
| Screenshot upload | `POST /api/tickets/{id}/attachment` (multipart field `file`), `GET /api/files/{name}` |
| User roles | `GET /api/users`, `PATCH /api/users/{id}/role` (admin only) |
| Real-time (optional) | SockJS/STOMP endpoint `/ws`, subscribe to `/topic/projects/{id}` |
| API docs | `/swagger-ui.html` |

## Role-based permissions
| Action | Admin | Manager | Developer |
|---|---|---|---|
| Create project | ✅ | ✅ | ❌ |
| Edit/delete project, manage members | ✅ (any) | ✅ (own projects) | ❌ |
| See project & tickets | all | member projects | member projects |
| Create ticket / comment / move on board | ✅ | ✅ (member) | ✅ (member) |
| Edit ticket | ✅ | owner, reporter, assignee | reporter, assignee |
| Delete ticket | ✅ | owner, reporter | reporter |
| Change user roles | ✅ | ❌ | ❌ |

Public sign-up always creates a `DEVELOPER`; an admin promotes users. All rules live in `AccessService`.

## Run locally
```bash
# Option A – PostgreSQL
docker compose up -d db
./mvnw spring-boot:run        # or: mvn spring-boot:run

# Option B – zero setup (in-memory H2)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Swagger: http://localhost:8080/swagger-ui.html

Seeded demo users (first start only):
- `admin@tracker.com` / `Admin@123`
- `manager@tracker.com` / `Manager@123`
- `dev@tracker.com` / `Dev@1234`

Run tests: `mvn test` (uses the H2 profile).

## Configuration (environment variables)
| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/issue_tracker` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | |
| `JWT_SECRET` | dev value | **Set a random 32+ char string in production** |
| `JWT_EXPIRATION_MS` | `86400000` | 24h |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma separated; add your deployed frontend URL |
| `UPLOAD_DIR` | `uploads` | Screenshot folder |
| `PORT` | `8080` | Provided by Render/Railway |

**MySQL instead of PostgreSQL:** replace the `postgresql` dependency in `pom.xml` with `com.mysql:mysql-connector-j` and set `DB_URL=jdbc:mysql://host:3306/issue_tracker`.

## Deploy (Render / Railway) – Day 12
1. Push this folder to GitHub.
2. Create a **PostgreSQL** database on the platform.
3. Create a **Web Service** from the repo, runtime **Docker** (uses the included `Dockerfile`).
4. Set env vars: `DB_URL` (`jdbc:postgresql://<host>:5432/<db>`), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS` (your frontend URL, e.g. `https://my-tracker.vercel.app`).
   - Render shows a `postgres://user:pass@host/db` URL – convert it to the JDBC form above and use the user/password in separate vars.
5. Attach a persistent disk mounted at `/app/uploads` (or the uploaded screenshots vanish on redeploy) and set `UPLOAD_DIR=/app/uploads`.
6. Open `https://<your-service>/swagger-ui.html` to verify.

## Project structure
```
src/main/java/com/tracker
├── config/       SecurityConfig, OpenApiConfig, WebSocketConfig, DataInitializer
├── security/     JwtUtil, JwtAuthFilter, CustomUserDetailsService
├── entity/       User, Project, Ticket, Comment + enums
├── repository/   Spring Data JPA repositories (entity graphs avoid N+1)
├── dto/          Java records for requests/responses
├── service/      Business logic, AccessService (permissions), TicketSpecifications (search)
├── controller/   REST controllers
└── exception/    Global error handling (consistent JSON errors)
```

Query optimisation notes: indexes on `tickets(project_id, status, assignee_id)`, `@EntityGraph` fetch joins on the list query, `default_batch_fetch_size`, server-side pagination with a whitelist of sortable fields.
