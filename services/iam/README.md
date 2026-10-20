# IAM Service

Secure authentication and authorization service for the HMIS system.

## Purpose
Provides secure user identity management and role-based access control for the HMIS system. Integrates with Keycloak for enterprise identity federation.

## Features
- User authentication via Keycloak
- Role-based access control (patient, staff, admin)
- Permission management and validation
- Integration with master data services
- Secure token management

## Architecture
- Keycloak as identity provider
- Centralized role hierarchy
- Fine-grained permission mapping
- RESTful API for client applications

## Configuration
- Config file: `config/keycloak.yaml`
- Environment variables: `KEYCLOAK_CLIENT_SECRET`
- Default port: 8080

## API Endpoints
- POST /auth/login - Authenticate user
- GET /users/{userId} - Retrieve user details
- POST /roles - Manage roles
- GET /permissions - List available permissions

## Development
1. Configure `config/keycloak.yaml` with your Keycloak connection details
2. Set `KEYCLOAK_CLIENT_SECRET` environment variable
3. Run: `./mvnw spring-boot:run`

## Security
- Default-deny policy enforced
- All endpoints require authentication
- Role-based access control implemented
- Audit logging for security events
