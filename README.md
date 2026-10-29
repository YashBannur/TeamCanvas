# TeamCanvas

Collaborative project management workspace (portfolio project).
Backend: Java 21, Spring Boot 3.5.16, MySQL 8, Flyway, JWT. Frontend: React + Vite (separate, not in this archive).

## Status of the backend (this archive)

Phase 2 (auth) and Phase 3 steps 3.1 - 3.4b are included:

| Area | What exists |
|---|---|
| Auth | register, login, refresh (rotating HttpOnly cookie), logout, `GET /api/me` |
| Workspaces | create/list/get/update/delete, members, role changes, remove, leave |
| Invitations | invite by email (7-day expiry, no duplicate pending), list, accept, reject, cancel |
| Projects | create/list/get/update/archive/delete, progress calculated from tasks |
| Tasks | create/get/update/move/delete, option B permission rules, optimistic locking |
| Task queries | search, filters, sorting, pagination, Kanban board |
| Tests | JwtServiceTest, AuthServiceTest, AuthIntegrationTest (25 tests) |

Next: Step 3.5 (automated tests for workspaces, invitations, projects, tasks), then the frontend wiring.

## Restore on a new machine / after losing the project

1. Install JDK 21 and MySQL 8, and open `backend` as a Maven project in your IDE.
2. Copy `backend/.env.example` to `backend/.env` and fill in your MySQL root password and a JWT secret
   (32+ characters). `.env` is git-ignored and is NOT part of this archive.
3. **Start with fresh databases.** Flyway stores a checksum of every migration. If your MySQL still has the
   `teamcanvas` / `teamcanvas_test` databases from before, drop them first so Flyway recreates them:

   ```sql
   DROP DATABASE IF EXISTS teamcanvas;
   DROP DATABASE IF EXISTS teamcanvas_test;
   ```

   (The URL has `createDatabaseIfNotExist=true`, so they are created again on start.)
4. Run `TeamCanvasApplication` with `backend` as the working directory (so `.env` is found).
5. Swagger UI: http://localhost:8080/swagger-ui.html - health: http://localhost:8080/actuator/health
6. Tests: run `src/test/java` as JUnit (the integration test uses the `teamcanvas_test` database).

The Maven wrapper (`mvnw`) is not included; use your IDE's Maven or generate it again.
