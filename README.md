# Gestion des Normes - Backend API

A comprehensive REST API for international standards (ISO, IEC, EN) management with role-based user authentication, advanced filtering, hierarchical ICS classification, and complete user administration system.

**Tech Stack**: Spring Boot 3.x | PostgreSQL | JWT Security | Flyway | Spring Data JPA/Hibernate | Swagger/OpenAPI | Maven

---

## 📋 Project Overview

**Gestion des Normes** is a platform for managing international standards with advanced classification, user management, and robust security. It provides CRUD operations for standards (Normes), hierarchical ICS (International Classification for Standards) levels, and lookup tables with role-based access control.

### Key Capabilities:
- 📌 **Standards Management**: Full CRUD operations with multi-language support (French, English, German)
- 👥 **User Management**: Complete user administration for admins with user CRUD, roles, and password management
- 🏆 **ICS Hierarchy**: 3-level classification system with automatic code prefixing (e.g., `77` → `77.01` → `77.01.401`)
- 🔐 **JWT Authentication**: Secure access with access tokens (15 min) and refresh tokens (14 days)
- 🛡️ **Role-Based Access**: ADMIN (full access) and USER (read-only) roles with `@PreAuthorize` validation
- 📊 **Pagination & Filtering**: Advanced filtering by reference, status, ICS levels with paginated results
- 🔄 **Lookup Tables**: Manage STATUT, DOCUMENT_TYPE, COLLECTION, BRANCH, FAMILY, SUBFAMILY, FILTER categories
- 📡 **API Documentation**: Full Swagger UI with OpenAPI 3.0 specification

---

## 🏗️ System Architecture

### Package Structure
```
src/main/java/portail/web/backend/exemple/portail/web/backend/
├── controller/          # REST API endpoints
├── service/             # Business logic layer
├── repository/          # Data access with JPA
├── entity/              # Domain models
├── dto/                 # Request/Response DTOs
├── mapper/              # Entity-DTO mappers
├── config/              # Application configuration
├── security/            # JWT & security components
├── exception/           # Global exception handling
├── auth/                # Authentication endpoints
└── user/                # User management (service, controller, DTO)
```

### Layered Architecture
- **Controller Layer**: HTTP endpoints with request validation
- **Service Layer**: Business logic, validation, data transformation
- **Repository Layer**: Spring Data JPA with @EntityGraph for lazy-loading optimization
- **Entity Layer**: JPA entities with relationships and constraints
- **Security Layer**: JWT token generation/validation, role-based access

---

## 🎯 Key Features

### Authentication & Authorization
- **JWT-based Authentication**: `/api/auth/register` and `/api/auth/login`
- **Token Management**: Access tokens (15 min expiry) + Refresh tokens (14 days)
- **Role-Based Access Control**: 
  - `ROLE_ADMIN`: Full CRUD access to standards, lookups, and users
  - `ROLE_USER`: Read-only access to standards and lookups
- **Password Encryption**: BCrypt encoding with salt
- **Auto-Admin Seeding**: Optional pre-configured admin user (disable in production)

### Standards (Norme) Management
- **Full CRUD Operations**: Create, read, update, delete standards
- **Multi-language Support**: French (titreFr, descripteurFr), English (titreEn, descripteurEn), German (titreDe)
- **Rich Metadata**: Publication date, document type, collection, print info, subscription details
- **PDF Storage**: Upload/download a PDF per norme (stored on disk, path in DB)
- **Advanced Relationships**: Links to 9 lookup tables (statut, documentType, collection, industrialBranch, productFamily, subFamily, filter1, icsLevel1/2/3)
- **Search & Filter**: By reference, status, ICS hierarchy levels, with pagination
- **Timestamps**: Automatic createdAt/updatedAt tracking

### User Management System
- **Admin-only Operations**: User CRUD at `/api/users`
- **User CRUD**: Create, list, retrieve by ID, update, delete users
- **Role Management**: Assign ROLE_ADMIN or ROLE_USER with normalization
- **Password Management**: Optional password update (empty = no change)
- **Duplicate Prevention**: Validates unique usernames
- **List Management**: Paginated user list with optional filtering by username/role

### ICS Hierarchy (International Classification for Standards)
- **3-Level Hierarchy**: Level1 → Level2 → Level3
- **Auto-Code Prefixing**:
  - Level1: Base code (e.g., `77`)
  - Level2: Auto-prefixed (e.g., `01` becomes `77.01`)
  - Level3: Auto-prefixed (e.g., `401` becomes `77.01.401`)
- **Lazy Loading Optimization**: Uses `@EntityGraph` for eager loading to prevent "no session" errors
- **Hierarchical Filtering**: Filter normes by any ICS level

### Lookup Tables
- **STATUT**: Status/publication state (PUBLISHED, DRAFT, etc.)
- **DOCUMENT_TYPE**: Type classification (STANDARD, TECHNICAL_REPORT, etc.)
- **COLLECTION**: Collection grouping (ISO, IEC, EN, etc.)
- **INDUSTRIAL_BRANCH**: Industry categorization (MANUFACTURING, IT, etc.)
- **PRODUCT_FAMILY**: Product family classification
- **SUB_FAMILY**: Sub-family details
- **FILTER**: Additional filter category
- **All with CRUD**: Full create, read, update, delete via `/api/lookups/{type}`

### Data Quality & Validation
- **Bean Validation**: Jakarta Validation annotations (@NotBlank, @Size, @Email, etc.)
- **Global Exception Handling**: Standardized error responses with proper HTTP status codes
- **Field Constraints**: Min/max lengths, required fields, format validation
- **Business Rules**: Duplicate username prevention, role validation, password strength (optional)

---

## 🗄️ Database Schema

### Core Tables
- **user**: User accounts with encrypted passwords and roles
- **norme**: Standards catalog with multi-language support
- **statut**: Publication status lookup
- **document_type**: Document type lookup
- **collection**: Collection lookup (ISO, IEC, EN)
- **industrial_branch**: Industry branch lookup
- **product_family**: Product family lookup
- **sub_family**: Sub-family lookup
- **filter**: Additional filter lookup
- **ics_level1**, **ics_level2**, **ics_level3**: ICS hierarchy lookups

### Flyway Migrations
- **V1__initial_schema.sql**: User and core schema
- **V2__add_categories_and_standards.sql**: Standard lookups and norme table
- **V3__create_normes_and_lookup_tables.sql**: ICS hierarchy tables

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.8 or higher

### Setup & Installation

1. **Clone repository**
   ```powershell
   git clone <repository-url>
   cd portail-web-backend
   ```

2. **Configure database** (edit `src/main/resources/application.properties`)
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_normes
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```

3. **Run the application**
   ```powershell
   mvn clean spring-boot:run
   ```

4. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Build JAR
```powershell
mvn clean package
java -jar target/portail-web-backend-0.0.1-SNAPSHOT.jar
```

---

## 📡 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT tokens
- `POST /api/auth/refresh` - Refresh access token

### Standards (Normes)
- `GET /api/normes?page=0&size=20` - List standards with pagination
- `GET /api/normes/{id}` - Get standard by ID
- `POST /api/normes` - Create new standard (ADMIN only)
- `PUT /api/normes/{id}` - Update standard (ADMIN only)
- `DELETE /api/normes/{id}` - Delete standard (ADMIN only)
- `POST /api/normes/{id}/pdf` - Upload norme PDF (ADMIN only, multipart `file`)
- `GET /api/normes/{id}/pdf` - Download norme PDF (ADMIN/USER)
- **Filters**: `?reference=ISO&statutId=1&icsLevel1Id=1&icsLevel2Id=12&icsLevel3Id=123`

### Lookup Tables
- `GET /api/lookups/{type}?page=0&size=20` - List lookups (STATUT, DOCUMENT_TYPE, COLLECTION, etc.)
- `GET /api/lookups/{type}/{id}` - Get single lookup
- `POST /api/lookups/{type}` - Create lookup (ADMIN only)
- `PUT /api/lookups/{type}/{id}` - Update lookup (ADMIN only)
- `DELETE /api/lookups/{type}/{id}` - Delete lookup (ADMIN only)

### User Management (Admin Only)
- `GET /api/users?page=0&size=20` - List all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- **Filters**: `?username=john&role=ROLE_USER`

---

## 📋 Configuration

### Application Properties (`application.properties`)

```properties
# Server
server.port=8080
server.servlet.context-path=/

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_normes
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway (Database Migrations)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT Configuration
app.jwt.secret=your-secret-key-min-256-bits
app.jwt.access-token-expiry=900000  # 15 minutes
app.jwt.refresh-token-expiry=1209600000  # 14 days

# Admin User Seeding (disable in production)
app.admin.seed.enabled=false
app.admin.username=admin
app.admin.password=admin@123

# PDF storage (normes)
app.storage.norme-pdf-dir=storage/normes

# CORS
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

---

## 🔐 Authentication Flow

### 1. Register New User
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "securePassword@123"
}
```

### 2. Login to Get Tokens
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "securePassword@123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900000
}
```

### 3. Use Access Token in Headers
```bash
GET /api/normes
Authorization: Bearer {accessToken}
```

### 4. Refresh Token When Expired
```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{refreshToken}"
}
```

---

## 👥 User Management Examples

### Create User (Admin Only)
```bash
POST /api/users
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "username": "new_user",
  "password": "password@123",
  "role": "ROLE_USER"
}
```

### List Users with Pagination
```bash
GET /api/users?page=0&size=10&username=john
Authorization: Bearer {adminToken}
```

### Update User (Admin Only)
```bash
PUT /api/users/1
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "username": "updated_user",
  "password": "",  # Empty password = don't change
  "role": "ROLE_ADMIN"
}
```

### Delete User (Admin Only)
```bash
DELETE /api/users/1
Authorization: Bearer {adminToken}
```

---

## 📌 Standard (Norme) Examples

### Create Standard (Admin Only)
```bash
POST /api/normes
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "reference": "ISO-9001:2015",
  "publicationDate": "2015-09-23",
  "titreFr": "Systèmes de management de la qualité",
  "titreEn": "Quality management systems",
  "documentIdentifier": "DOC-ISO-9001",
  "includedInSubscription": true,
  "mandatory": false,
  "statutId": 1,
  "documentTypeId": 2,
  "collectionId": 1,
  "industrialBranchId": 3,
  "productFamilyId": 7,
  "subFamilyId": 11,
  "icsLevel1Id": 1,
  "icsLevel2Id": 12,
  "icsLevel3Id": 123
}
```

### List Standards with Filters
```bash
GET /api/normes?page=0&size=20&reference=ISO&statutId=1&icsLevel1Id=1&icsLevel2Id=12
Authorization: Bearer {token}
```

### Get Standard by ID
```bash
GET /api/normes/42
Authorization: Bearer {token}
```

**Example Response:**
```json
{
  "id": 42,
  "reference": "ISO-9001:2015",
  "publicationDate": "2015-09-23",
  "titreFr": "Systèmes de management de la qualité",
  "titreEn": "Quality management systems",
  "titreDe": "Qualitatsmanagementsysteme",
  "descripteurFr": "Management de la qualite",
  "descripteurEn": "Quality management",
  "documentIdentifier": "DOC-ISO-9001",
  "includedInSubscription": true,
  "afnorIndex": "X50-131",
  "printNumber": "3",
  "printDate": "2024-01-10",
  "mandatory": false,
  "statutId": 1,
  "statutCode": "PUBLISHED",
  "documentTypeId": 2,
  "documentTypeCode": "STANDARD",
  "collectionId": 1,
  "collectionCode": "ISO",
  "industrialBranchId": 3,
  "industrialBranchCode": "MANUFACTURING",
  "productFamilyId": 7,
  "productFamilyCode": "QUALITY",
  "subFamilyId": 11,
  "subFamilyCode": "QMS",
  "icsLevel1Id": 1,
  "icsLevel1Code": "01",
  "icsLevel2Id": 12,
  "icsLevel2Code": "01.040",
  "icsLevel3Id": 123,
  "icsLevel3Code": "01.040.03",
  "createdAt": "2026-05-14T10:00:00",
  "updatedAt": "2026-05-14T10:00:00"
}
```

---

## 🏷️ Lookup Table Examples

### List All Statuses
```bash
GET /api/lookups/STATUT?page=0&size=50
Authorization: Bearer {token}
```

### Available Lookup Types
- `STATUT` - Publication status
- `DOCUMENT_TYPE` - Document type
- `COLLECTION` - Collection (ISO, IEC, EN)
- `INDUSTRIAL_BRANCH` - Industry branch
- `PRODUCT_FAMILY` - Product family
- `SUB_FAMILY` - Sub-family
- `FILTER` - Additional filter

### Create Lookup Item (Admin Only)
```bash
POST /api/lookups/STATUT
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "code": "PUBLISHED",
  "label": "Published"
}
```

---

## 🧪 Testing

### Run Tests
```powershell
mvn clean test
```

### Test Coverage
- Authentication and token management
- User CRUD operations
- Standards CRUD operations
- Lookup table management
- Pagination and filtering
- Role-based access control
- Duplicate validation
- Error handling

---

## 🔧 Development

### IDE Setup
- **IntelliJ IDEA**: Import as Maven project
- **VS Code**: Install Java extensions

### Code Style
- Use Java 17+ features (records, text blocks where appropriate)
- Follow Spring Boot best practices
- Use DTOs for API contracts
- Use @PreAuthorize for role-based access

### Add New Feature
1. Create entity in `entity/`
2. Create repository in `repository/` with @EntityGraph if needed
3. Create DTO in `dto/`
4. Create service in `service/`
5. Create controller in `controller/`
6. Add Flyway migration in `src/main/resources/db/migration/`
7. Write tests

---

## 🐛 Common Issues

### Issue: "Could not initialize proxy - no session"
**Cause**: Lazy loading of relationships after session closed  
**Solution**: Add `@EntityGraph` to repository methods for eager loading

### Issue: "JWT token expired"
**Solution**: Use the refresh token endpoint to get new access token

### Issue: "Access denied" on API calls
**Cause**: Missing or invalid JWT token, or insufficient role permissions  
**Solution**: Ensure token is in `Authorization: Bearer {token}` header and user has required role

---

## 📝 Database Migrations

Managed by Flyway. New migrations should follow naming: `V{N}__{description}.sql`

- **V1**: Initial schema with user table
- **V2**: Lookup tables (statut, document_type, etc.)
- **V3**: ICS hierarchy tables and norme table
- **V4**: PDF fields for normes

---

## 🎯 Frontend Integration

This backend is designed to work with React/Vue frontend at endpoints:
- Standards list and management: `/gestion/normes`
- User management: `/gestion/utilisateurs` (Admin only)
- Authentication: `/auth/login`, `/auth/register`

Configure CORS in `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:5173
```

---

## 📚 Dependencies

- **Spring Boot**: Web, Data JPA, Security
- **JWT**: java-jwt, jjwt
- **Database**: PostgreSQL driver, Flyway
- **Validation**: Jakarta Validation API
- **Documentation**: Springdoc OpenAPI
- **Mapper**: MapStruct (if used)
- **Lombok** (optional): Reduce boilerplate

---

## 📄 License

This project is proprietary and confidential.

---

## 👨‍💻 Team

- Backend: Spring Boot/Java
- Frontend: React/Vue.js
- Database: PostgreSQL
