# Spring Boot Security Project (Pro24 Group 4)

## Overview
This application is a stateless REST API built on **Spring Boot 4.1.1** and **Java 21** that provides user authentication (JWT), dynamic role- and permission-based authorization, soft deletion, and entity auditing backed by **PostgreSQL** and managed with **Liquibase**.

### Key Architectural Concepts
- **Identity-Only JWT**: JWT tokens only hold the subject username. All authorization checks dynamically load the user's current roles and permissions from the database on every request.
- **Two-Phase Filter Pipeline**:
  - `JwtGuardFilter` (Order `-101`): Intercepts requests before the security chain. Public auth endpoints (`/api/auth/register`, `/login`, `/refresh`) are passed with the `Authorization` header concealed. Protected requests require a valid Bearer token and check the account status (`enabled`, `accountNonLocked`, `accountNonExpired`, `credentialsNonExpired`, `deletedAt == null`). Missing or invalid credentials immediately return HTTP `401 Unauthorized`.
  - `AuthorityEnrichmentFilter` (Order `-99`): Runs after Spring Security authentication and before controllers, replacing the authentication object with fresh authorities queried from PostgreSQL via `AuthorityLoader`. Role assignments and revocations take effect immediately on the next request using the same token.
- **Method Security**: Enforced using `@EnableMethodSecurity` and `@PreAuthorize` on all management controller methods based on fine-grained permissions.

---

## Prerequisites
- **Java**: OpenJDK / Eclipse Adoptium Java 21+
- **Database**: PostgreSQL 16+ (running on `localhost:5432`)
- **Required Databases**:
  ```sql
  CREATE DATABASE pro24_group4;
  CREATE DATABASE pro24_group4_test;
  ```

---

## Environment Variables

| Variable | Description | Default / Example |
|---|---|---|
| `JWT_SECRET` | 32+ character HMAC-SHA secret key | `mysecretkeythatisatleast32characterslong123456` |
| `APP_SEED_ADMIN_PASSWORD` | Password for default `admin` user | `AdminPassword123!` |
| `APP_SEED_DARA_PASSWORD` | Password for default `dara` user | `DaraPassword123!` |
| `DB_TEST_URL` | Test database JDBC URL | `jdbc:postgresql://localhost:5432/pro24_group4_test` |
| `DB_TEST_USER` | Test database user | `postgres` |
| `DB_TEST_PASSWORD` | Test database password | `11111111` |

---

## Running & Testing

### Run Tests
```powershell
./mvnw clean test
```
The test suite runs against the dedicated `pro24_group4_test` database and verifies authentication, authorization rules, and CRUD endpoints.

### Run Application
Set the required environment variables and start the server:
```powershell
$env:DB_PASSWORD = "11111111"
$env:DB_TEST_PASSWORD = "11111111"
$env:JWT_SECRET = "mysecretkeythatisatleast32characterslong123456"
$env:JWT_TEST_SECRET = "mytestsecretkeythatisatleast32characterslong123456"
$env:APP_SEED_ADMIN_PASSWORD = "AdminPassword123!"
$env:APP_SEED_DARA_PASSWORD = "DaraPassword123!"
mvn spring-boot:run
```
The server starts on port `8081` (or configured port).

---

## Liquibase Workflow & Seeding
1. Liquibase executes changesets on application startup from `classpath:db/changelog/db.changelog-master.yaml`.
2. Tables created: `users`, `roles`, `permissions`, `role_user`, `role_permission`.
3. Default seed: `ROLE_ADMIN`, `ROLE_USER`, Permission records and Initial role-permission relationships.
4. `DataInitializer`:
   - Automatically realigns PostgreSQL identity sequences (`roles_id_seq`, `permissions_id_seq`, `users_id_seq`) to prevent duplicate key conflicts.
   - Links `ROLE_ADMIN` to all 9 permissions and `ROLE_USER` to `USER_READ`.
   - Seeds `admin` (`ROLE_ADMIN`) and `dara` (`ROLE_USER`) accounts if present in environment variables.

---

## Default Accounts

| Username | Role | Assigned Permissions | Password Source |
|---|---|---|---|
| `admin` | `ROLE_ADMIN` | All 9 permissions (`USER_*`, `ROLE_*`, `PERMISSION_*`) | `$env:APP_SEED_ADMIN_PASSWORD` |
| `dara` | `ROLE_USER` | `USER_READ` | `$env:APP_SEED_DARA_PASSWORD` |

*Note: If password environment variables are omitted or blank, user creation is skipped and logged.*

---

## API Endpoints Summary

### Authentication (`/api/auth`)
- `POST /api/auth/register` (Public) - Register new user
- `POST /api/auth/login` (Public) - Login with username and password
- `POST /api/auth/refresh` (Public) - Refresh JWT token
- `GET /api/auth/me` (Authenticated) - Get current user profile
- `GET /api/test` (Authenticated) - Protected test endpoint

### User Management (`/api/users`)
- `GET /api/users` - `hasAuthority('USER_READ')`
- `GET /api/users/{id}` - `hasAuthority('USER_READ')`
- `POST /api/users` - `hasAuthority('USER_WRITE')`
- `PUT /api/users/{id}` - `hasAuthority('USER_WRITE')`
- `DELETE /api/users/{id}` - `hasAuthority('USER_DELETE')` (Soft delete)
- `POST /api/users/{userId}/roles/{roleId}` - `hasRole('ADMIN')`
- `DELETE /api/users/{userId}/roles/{roleId}` - `hasRole('ADMIN')`
- `GET /api/users/{id}/permissions` - `hasAuthority('USER_READ')`

### Role Management (`/api/roles`)
- `GET /api/roles` - `hasAuthority('ROLE_READ')`
- `GET /api/roles/{id}` - `hasAuthority('ROLE_READ')`
- `POST /api/roles` - `hasAuthority('ROLE_WRITE')`
- `PUT /api/roles/{id}` - `hasAuthority('ROLE_WRITE')`
- `DELETE /api/roles/{id}` - `hasAuthority('ROLE_DELETE')` (Reserved roles cannot be deleted)
- `POST /api/roles/{roleId}/permissions/{permissionId}` - `hasRole('ADMIN')`
- `DELETE /api/roles/{roleId}/permissions/{permissionId}` - `hasRole('ADMIN')`

### Permission Management (`/api/permissions`)
- `GET /api/permissions` - `hasAuthority('PERMISSION_READ')`
- `GET /api/permissions/{id}` - `hasAuthority('PERMISSION_READ')`
- `POST /api/permissions` - `hasAuthority('PERMISSION_WRITE')`
- `PUT /api/permissions/{id}` - `hasAuthority('PERMISSION_WRITE')`
- `DELETE /api/permissions/{id}` - `hasAuthority('PERMISSION_DELETE')`

For the complete authorization rules matrix, see [Authorization Matrix](docs/authorization-matrix.md).

---

## Acceptance Scenarios (401, 403, 200)

1. **401 Unauthorized**:
   - Accessing any protected endpoint without an Authorization header returns HTTP `401`.
   - Accessing with an expired or malformed token returns HTTP `401`.
   - Logging in with an account having `account_non_locked = false` or `enabled = false` returns HTTP `401`.
   - Token of a soft-deleted user immediately returns HTTP `401`.
2. **403 Forbidden**:
   - User `dara` (who possesses only `USER_READ`) calling `DELETE /api/users/2` returns HTTP `403`.
   - Non-admin calling role/permission assignment endpoints returns HTTP `403`.
3. **200 OK / 201 Created / 204 No Content**:
   - Admin calling `POST /api/users/{userId}/roles/{roleId}` returns HTTP `200`.
   - Creating resources returns HTTP `201`.
   - Calling `DELETE /api/users/{id}` sets `deleted_at`, the row remains in PostgreSQL, and responds with HTTP `204`.
   - No response body ever contains a password or passwordHash.

---

## Postman Collection & Submission Evidence
- **Postman Collection**: [docs/postman/pro24_group4.postman_collection.json](docs/postman/pro24_group4.postman_collection.json)


---

## Known Limitations
1. **Duplicate Username on Register**: The frozen `AuthService.register()` method throws `IllegalArgumentException` when a duplicate username is registered. Consequently, the `GlobalExceptionHandler` returns HTTP `400 Bad Request` rather than `409 Conflict`.
2. **Frozen Entity Field Defaults**: In the frozen `User.java` entity, `@Builder` does not include default boolean flags. Our services and snapshots explicitly handle null values as `true`.
