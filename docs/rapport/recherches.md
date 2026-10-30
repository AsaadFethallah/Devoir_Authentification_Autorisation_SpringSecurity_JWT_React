# Travaux de recherche

Ce document répond aux points de recherche du sujet en se limitant au code présent dans le projet. Les chemins indiqués sont relatifs à la racine du dépôt.

## 1. Relations JPA et stratégie de chargement

Les relations principales sont les suivantes :

- `Incident.createdBy` et `Incident.assignedTo` sont des `@ManyToOne(fetch = FetchType.LAZY)` vers `User`.
- `IncidentHistory.incident`, `IncidentHistory.author`, `IncidentComment.incident` et `IncidentComment.author` sont aussi des `@ManyToOne(fetch = FetchType.LAZY)`.
- `RefreshToken.user` est un `@ManyToOne(fetch = FetchType.LAZY)`.

Le code ne déclare pas de collection `@OneToMany(mappedBy = ...)` dans `User` ou `Incident`. Les relations sont donc orientées depuis les objets dépendants vers leurs parents. Les recherches de listes passent par les repositories, par exemple `findByCreatedById...` ou `findByIncidentId...`.

Le projet choisit explicitement `LAZY` pour les relations `ManyToOne`. L’entité `Incident` précise que les contrôleurs renvoient des DTO et non les entités, afin d’éviter les boucles JSON et les problèmes de chargement différé. Les services concernés sont transactionnels, ce qui permet de charger les relations nécessaires pendant la conversion en DTO. Aucun `EAGER` explicite n’est utilisé.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/model/Incident.java`, `IncidentHistory.java`, `IncidentComment.java`, `RefreshToken.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/service/DtoMapper.java`.

## 2. Claims JWT et expiration

`JwtService.generateToken` crée un JWT signé en HS256 avec :

- le sujet (`sub`) égal à l’adresse email ;
- la date d’émission (`iat`) ;
- la date d’expiration (`exp`) ;
- un claim `role` contenant le rôle de l’utilisateur.

Lors de la réception d’une requête, `JwtAuthenticationFilter` vérifie la signature et les claims via `JwtService.isValid`, puis extrait le sujet avec `extractEmail`. La bibliothèque JWT vérifie également l’expiration lors du parsing : un token expiré est donc considéré comme invalide.

Le claim `role` est ajouté au token mais n’est pas utilisé directement pour construire les autorisations. Le filtre recharge l’utilisateur en base à partir de l’email, vérifie qu’il est activé, puis crée l’autorité à partir du rôle actuellement enregistré en base. Cette décision évite de faire confiance à un rôle isolé dans le token.

La durée de l’access token est configurée par `app.jwt.expiration-ms`. La valeur de développement présente dans `application.properties` est de 300000 ms. Aucun secret JWT ni token complet n’est reproduit ici.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/security/JwtService.java`, `JwtAuthenticationFilter.java`, `incidents-api/src/main/resources/application.properties`.

## 3. `hasRole`, `hasAuthority`, `@PreAuthorize` et `@EnableMethodSecurity`

`@EnableMethodSecurity` est activé dans `SecurityConfig`. Il permet l’évaluation des annotations de sécurité placées sur les contrôleurs.

Le projet utilise réellement :

- `@PreAuthorize("hasRole('EMPLOYEE')")` pour créer et consulter les incidents d’un employé ;
- `@PreAuthorize("hasRole('TECHNICIAN')")` pour la liste des incidents affectés ;
- `@PreAuthorize("hasRole('ADMIN')")` pour les opérations d’administration et l’affectation ;
- `hasAnyRole('TECHNICIAN', 'ADMIN')` pour les changements d’état et les notes ;
- `isAuthenticated()` pour les accès nécessitant seulement une authentification.

Avec Spring Security, `hasRole('EMPLOYEE')` recherche habituellement l’autorité `ROLE_EMPLOYEE`. Le filtre construit précisément des autorités sous la forme `ROLE_` + nom du rôle.

`hasAuthority('...')` comparerait une autorité exacte, sans ajouter automatiquement le préfixe de rôle. Cette expression n’est pas utilisée dans le code actuel. Il ne faut donc pas la présenter comme une règle effectivement configurée.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/security/SecurityConfig.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/controller/IncidentController.java`, `AdminController.java`, `ProfileController.java`.

## 4. Récupération de l’utilisateur authentifié et contrôle du propriétaire

`CurrentUserService.getRequiredUser` récupère l’objet `Authentication` depuis `SecurityContextHolder`. Il vérifie que l’authentification existe, qu’elle est authentifiée et qu’elle n’est pas anonyme. Le nom de l’authentification correspond à l’email placé dans le sujet du JWT ; cet email sert à relire l’utilisateur en base.

Dans `IncidentService`, le contrôle d’accès aux détails d’un incident compare ensuite les identifiants :

- un administrateur peut lire l’incident ;
- son créateur peut le lire ;
- le technicien auquel il est affecté peut le lire.

Les opérations de modification vérifient aussi que le technicien est bien le technicien affecté. La présence d’un `Principal` dans une signature de contrôleur n’est pas utilisée dans ce projet : le choix réel est `Authentication` via `SecurityContextHolder`, puis une recherche en base.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/service/CurrentUserService.java`, `IncidentService.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/security/JwtAuthenticationFilter.java`.

## 5. Validation des données

Les DTO utilisent Jakarta Validation :

- `@NotBlank` vérifie qu’une chaîne existe et ne contient pas seulement des espaces ;
- `@Email` vérifie le format de l’adresse ;
- `@Size` limite la longueur du mot de passe, du titre, de la description et des notes ;
- `@NotNull` vérifie les valeurs obligatoires des énumérations d’incident.

Les contrôleurs placent `@Valid` sur les corps de requête. Une erreur de validation produit une réponse HTTP 400 contenant le message général et la liste des champs invalides.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/dto/RegisterRequest.java`, `LoginRequest.java`, `IncidentRequest.java`, `CommentRequest.java`, `PasswordChangeRequest.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/controller/AuthController.java`, `IncidentController.java`, `ProfileController.java`.

## 6. `@RestControllerAdvice` et gestion des exceptions

Le projet utilise `@RestControllerAdvice`, spécialisation adaptée aux réponses JSON de `@ControllerAdvice`. `GlobalExceptionHandler` centralise notamment :

- les `ApiException` portant déjà un code HTTP ;
- les erreurs de validation `MethodArgumentNotValidException` ;
- les corps JSON illisibles ;
- les conflits d’intégrité en base ;
- les accès refusés et les erreurs imprévues.

Les réponses utilisent la structure `ErrorResponse` : `status`, `message` et, lorsque nécessaire, `errors` avec le nom et le message de chaque champ.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/exception/GlobalExceptionHandler.java`, `ErrorResponse.java`, `FieldErrorResponse.java`.

## 7. Différences entre 400, 401, 403 et 404 dans ce projet

| Code | Signification dans l’API | Exemples réels |
|---|---|---|
| 400 | Requête reçue mais données ou transition invalides | validation DTO, JSON illisible, transition d’état interdite, modification d’un incident `CLOSED` |
| 401 | Authentification absente ou non valide | JWT absent, invalide ou expiré ; identifiants incorrects ; utilisateur désactivé |
| 403 | Utilisateur authentifié mais sans permission | rôle EMPLOYEE sur une route ADMIN ; technicien non affecté |
| 404 | Ressource demandée inexistante | incident ou technicien recherché absent |

Les réponses 401 et 403 de la couche Spring Security sont produites respectivement par `RestAuthenticationEntryPoint` et `RestAccessDeniedHandler`. Les exceptions métier `UnauthorizedException`, `ForbiddenException`, `BadRequestException` et `NotFoundException` sont traitées par `GlobalExceptionHandler` via leur classe mère `ApiException`.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/security/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/exception/*.java`.

## 8. Intercepteur Axios

`incidents-front/src/services/api.js` configure deux intercepteurs :

- l’intercepteur de requête lit l’access token dans `localStorage` et ajoute `Authorization: Bearer ...` ;
- l’intercepteur de réponse intercepte un 401 sur une route non liée à l’authentification, appelle `/auth/refresh` une seule fois, enregistre la nouvelle session et rejoue la requête initiale.

`refreshPromise` évite plusieurs renouvellements concurrents. Si le renouvellement échoue, la session est supprimée, un message est placé en `sessionStorage` et l’utilisateur est redirigé vers `/login`. Les routes d’authentification sont exclues de la tentative automatique de renouvellement.

## 9. Access token et refresh token

L’access token est le JWT court utilisé dans l’en-tête `Authorization` pour accéder aux routes protégées. Le refresh token sert uniquement à obtenir une nouvelle paire de tokens lorsque l’access token expire.

Le refresh token est bien implémenté dans ce projet. `RefreshTokenService` stocke un hash du refresh token, avec ses dates de création, d’expiration et de révocation. L’endpoint `/auth/refresh` fait tourner le token et invalide l’ancien. L’endpoint `/auth/logout` le révoque. Les tests automatisés vérifient la rotation, le rejet de l’ancien token, la révocation après logout et le refus pour un utilisateur désactivé.

Le frontend conserve actuellement les deux tokens dans `localStorage`. Ce choix simplifie l’application, mais augmente l’impact potentiel d’une faille XSS ; il est signalé comme limite dans le rapport final.

Sources : `incidents-api/src/main/java/ma/formation/incidentsapi/service/RefreshTokenService.java`, `AuthService.java`, `AuthController.java`, `incidents-api/src/main/java/ma/formation/incidentsapi/model/RefreshToken.java`, `incidents-front/src/services/api.js`, `authService.js`.
