# Authorization Matrix

## 1. Roles and Default Permissions

| Role | Description | Assigned Permissions |
|---|---|---|
| `ROLE_ADMIN` | Administrator with full privileges | `USER_READ`, `USER_WRITE`, `USER_DELETE`, `ROLE_READ`, `ROLE_WRITE`, `ROLE_DELETE`, `PERMISSION_READ`, `PERMISSION_WRITE`, `PERMISSION_DELETE` |
| `ROLE_USER` | Standard application user | `USER_READ` |

---

## 2. API Endpoint Authorization Matrix

### Authentication Endpoints (`/api/auth/**` & `/api/test`)

| Method | Endpoint | Required Authority / Rule | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | **Public** (No Auth Header parsed) | Register a new user |
| `POST` | `/api/auth/login` | **Public** (No Auth Header parsed) | Authenticate and obtain JWT token |
| `POST` | `/api/auth/refresh` | **Public** (No Auth Header parsed) | Refresh expired/active JWT token |
| `GET` | `/api/auth/me` | **Authenticated** (Valid JWT, active account) | Get current user identity |
| `GET` | `/api/test` | **Authenticated** (Valid JWT, active account) | Test protected endpoint |

### User Management Endpoints (`/api/users/**`)

| Method | Endpoint | Required Authority / Rule | Description |
|---|---|---|---|
| `GET` | `/api/users` | `hasAuthority('USER_READ')` | List all active users |
| `GET` | `/api/users/{id}` | `hasAuthority('USER_READ')` | Get user by ID |
| `POST` | `/api/users` | `hasAuthority('USER_WRITE')` | Create new user |
| `PUT` | `/api/users/{id}` | `hasAuthority('USER_WRITE')` | Update user details |
| `DELETE` | `/api/users/{id}` | `hasAuthority('USER_DELETE')` | Soft delete user |
| `POST` | `/api/users/{userId}/roles/{roleId}` | `hasRole('ADMIN')` | Assign role to user |
| `DELETE` | `/api/users/{userId}/roles/{roleId}` | `hasRole('ADMIN')` | Revoke role from user |
| `GET` | `/api/users/{id}/permissions` | `hasAuthority('USER_READ')` | Get effective permissions of user |

### Role Management Endpoints (`/api/roles/**`)

| Method | Endpoint | Required Authority / Rule | Description |
|---|---|---|---|
| `GET` | `/api/roles` | `hasAuthority('ROLE_READ')` | List all active roles |
| `GET` | `/api/roles/{id}` | `hasAuthority('ROLE_READ')` | Get role by ID |
| `POST` | `/api/roles` | `hasAuthority('ROLE_WRITE')` | Create new role |
| `PUT` | `/api/roles/{id}` | `hasAuthority('ROLE_WRITE')` | Update role details |
| `DELETE` | `/api/roles/{id}` | `hasAuthority('ROLE_DELETE')` | Soft delete role (non-reserved) |
| `POST` | `/api/roles/{roleId}/permissions/{permissionId}` | `hasRole('ADMIN')` | Assign permission to role |
| `DELETE` | `/api/roles/{roleId}/permissions/{permissionId}` | `hasRole('ADMIN')` | Revoke permission from role |

### Permission Management Endpoints (`/api/permissions/**`)

| Method | Endpoint | Required Authority / Rule | Description |
|---|---|---|---|
| `GET` | `/api/permissions` | `hasAuthority('PERMISSION_READ')` | List all active permissions |
| `GET` | `/api/permissions/{id}` | `hasAuthority('PERMISSION_READ')` | Get permission by ID |
| `POST` | `/api/permissions` | `hasAuthority('PERMISSION_WRITE')` | Create new permission |
| `PUT` | `/api/permissions/{id}` | `hasAuthority('PERMISSION_WRITE')` | Update permission details |
| `DELETE` | `/api/permissions/{id}` | `hasAuthority('PERMISSION_DELETE')` | Soft delete permission |

---

## 3. Token & Authority Architecture

1. **Identity-Only Token**: The JWT token contains solely the subject (`username`) and validity timestamps.
2. **Dynamic Authority Resolution**:
   - `JwtGuardFilter` (Order `-101`): Validates token and checks user status in PostgreSQL (`AuthorityLoader.find(username)`). Rejects inactive, locked, or soft-deleted users with HTTP `401`.
   - `SecurityFilterChain` (Order `-100`): Runs Spring Security authentication filter.
   - `AuthorityEnrichmentFilter` (Order `-99`): Queries database through `AuthorityLoader` on each request and attaches the user's current roles and permissions to the Spring SecurityContext. Role or permission changes take effect on the very next request using the exact same JWT.
