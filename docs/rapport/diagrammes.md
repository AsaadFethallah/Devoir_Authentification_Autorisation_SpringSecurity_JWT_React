# Diagrammes du projet

Les diagrammes ci-dessous représentent uniquement les classes, relations et flux présents dans le code audité.

## 1. Diagramme des entités

```mermaid
erDiagram
    USER {
        BIGINT id PK
        VARCHAR fullName
        VARCHAR email UK
        VARCHAR password
        VARCHAR role
        BOOLEAN enabled
        TIMESTAMP createdAt
    }

    INCIDENT {
        BIGINT id PK
        VARCHAR title
        VARCHAR description
        VARCHAR category
        VARCHAR priority
        VARCHAR status
        TIMESTAMP createdAt
        TIMESTAMP updatedAt
        BIGINT created_by_id FK
        BIGINT assigned_to_id FK
    }

    INCIDENT_HISTORY {
        BIGINT id PK
        BIGINT incident_id FK
        TIMESTAMP eventDate
        VARCHAR actionText
        BIGINT author_id FK
    }

    INCIDENT_COMMENT {
        BIGINT id PK
        BIGINT incident_id FK
        BIGINT author_id FK
        VARCHAR content
        TIMESTAMP createdAt
    }

    REFRESH_TOKEN {
        BIGINT id PK
        VARCHAR tokenHash
        BIGINT user_id FK
        TIMESTAMP createdAt
        TIMESTAMP expiresAt
        TIMESTAMP revokedAt
    }

    USER ||--o{ INCIDENT : "createdBy"
    USER ||--o{ INCIDENT : "assignedTo"
    INCIDENT ||--o{ INCIDENT_HISTORY : "has"
    USER ||--o{ INCIDENT_HISTORY : "authors"
    INCIDENT ||--o{ INCIDENT_COMMENT : "has"
    USER ||--o{ INCIDENT_COMMENT : "authors"
    USER ||--o{ REFRESH_TOKEN : "owns"
```

Dans le code Java, ces liens sont portés par des `@ManyToOne(fetch = FetchType.LAZY)` depuis `Incident`, `IncidentHistory`, `IncidentComment` et `RefreshToken`. Les collections inverses `@OneToMany` ne sont pas déclarées.

## 2. Diagramme d’architecture et flux d’une requête

```mermaid
flowchart LR
    Browser[React Browser]
    Axios[Axios API client\nrequest/response interceptors]
    CORS[CORS\nlocalhost:5173]
    Filter[JwtAuthenticationFilter]
    Security[Spring Security\nSTATELESS + method security]
    Auth[AuthController\n/auth/**]
    Profile[ProfileController\n/api/profile/**]
    Incidents[IncidentController\n/api/incidents/**]
    Admin[AdminController\n/api/admin/**]
    Services[Services\nAuth / Profile / Incident / Admin]
    Current[CurrentUserService\nSecurityContextHolder]
    Repositories[Spring Data JPA repositories]
    DB[(H2 dev / MySQL profile)]
    JWT[JwtService\naccess JWT]
    Refresh[RefreshTokenService\nrotation et révocation]

    Browser --> Axios
    Axios --> CORS
    CORS --> Filter
    Filter --> Security
    Security --> Auth
    Security --> Profile
    Security --> Incidents
    Security --> Admin
    Auth --> Services
    Profile --> Services
    Incidents --> Services
    Admin --> Services
    Services --> Current
    Services --> Repositories
    Services --> JWT
    Services --> Refresh
    Repositories --> DB
    Refresh --> Repositories
    Services --> Axios
    Auth --> Axios
```

Le filtre valide le JWT présent dans `Authorization: Bearer ...`, recharge l’utilisateur par son email et place une authentification dans le `SecurityContext`. Les contrôleurs délèguent aux services. Les services appliquent les règles métier, puis utilisent les repositories pour accéder à la base. Les réponses sont des DTO JSON.

## 3. Flux de renouvellement d’une session

```mermaid
sequenceDiagram
    participant R as React/Axios
    participant A as API
    participant F as JwtAuthenticationFilter
    participant T as AuthService/RefreshTokenService
    participant B as Base de données

    R->>A: Requête avec access token
    A->>F: Vérification du JWT
    F-->>A: JWT expiré ou invalide
    A-->>R: HTTP 401
    R->>A: POST /auth/refresh avec refresh token
    A->>T: Rotation du refresh token
    T->>B: Vérifier hash, expiration et révocation
    T-->>A: Nouvel access token + nouveau refresh token
    A-->>R: Réponse de session
    R->>A: Rejoue la requête initiale
```
