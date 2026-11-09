# OUR VISION Backend

Spring Boot 3.3.5 / Java 21 / PostgreSQL / JWT / Flyway / Swagger starter.

## Run from Eclipse (Aiven PostgreSQL)

1. Import this folder into Eclipse as an existing project. Ensure Eclipse has Maven Integration (m2e) installed and the project has been imported as a Maven project.
2. Open `Run` -> `Run Configurations...` -> `Java Application` -> `OurVisionApplication` -> `Environment`.
3. Add these environment variables:
   - `DB_URL` = `jdbc:postgresql://pg-24659454-starsecretsuper99-6024.e.aivencloud.com:18724/defaultdb?sslmode=require`
   - `DB_USERNAME` = `avnadmin` (or the username shown in your Aiven service's connection details)
   - `DB_PASSWORD` = your current Aiven database password
   - `JWT_SECRET` = a random secret of at least 32 bytes
4. Click `Apply`, then `Run`. Keep credentials out of source control and screenshots.
5. Expected successful startup includes `Started OurVisionApplication`. If it fails, inspect the LAST `Caused by:` message:
   - `UnknownHostException`: verify the hostname in the runtime `DB_URL` (the host must contain `.e.aivencloud.com`).
   - `password authentication failed`: verify the Aiven username/password.
   - timeout/refused: check Aiven service status, access rules, and port `18724`.
6. Health endpoint: http://localhost:8080/api/health
7. Swagger UI: http://localhost:8080/swagger-ui.html

**Security:** A database password was previously exposed in chat. Rotate it in Aiven before continuing, then set the new value only in Eclipse's Environment tab. Do not place it in this file.

## Run with Docker Compose (local PostgreSQL)

The Compose configuration starts a local PostgreSQL database and passes `DB_URL=jdbc:postgresql://postgres:5432/ourvision_db` to the backend container. This hostname is correct inside the Compose network; do not replace it with the Aiven hostname for this mode.

Run from a terminal with Docker installed:

```powershell
docker compose up --build
```

## Maven notes

This ZIP does not include `mvnw.cmd`. If Eclipse Maven Integration is installed, use `Maven` -> `Update Project...`, then run `OurVisionApplication.java` as a Java Application. Alternatively install Maven 3.9+ and run `mvn spring-boot:run` from the project directory.

## Dev seed account

`admin@ourvision.org` / `Admin@123` is created by `DataSeeder` if the user does not already exist. Change this before any shared/public deployment.

## API endpoints in this starter

- `POST /api/auth/login` (public)
- `GET /api/users`, `GET /api/users/{id}`, `POST /api/users`, `DELETE /api/users/{id}`
- `GET /api/contributions`, `POST /api/contributions`
- `GET /api/meetings`, `POST /api/meetings`, `PUT /api/meetings/{id}`
- `GET /api/events`, `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}`
- `GET /api/health` (public)

Use `Authorization: Bearer <accessToken>` for protected endpoints. This is a functional starter, not the full 130+ class production implementation: profile, attendance, post moderation, notifications, file uploads, and the remaining modules are represented in the database schema but need their own controllers/services and authorization rules.
