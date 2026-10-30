# Incident Desk

Application de déclaration et de suivi d’incidents informatiques, réalisée avec Spring Boot, Spring Security, JWT, JPA, H2 et React.

Le projet contient deux applications indépendantes :

- `incidents-api` : API REST Spring Boot sur le port `8080` ;
- `incidents-front` : interface React/Vite sur le port `5173`.

Les comptes et mots de passe indiqués ci-dessous sont exclusivement des comptes de démonstration locaux.

## Prérequis

- Java 17 ou supérieur ;
- Node.js et npm ;
- une connexion Internet au premier lancement pour télécharger les dépendances Maven/npm.

Le backend contient `mvnw.cmd`. Maven n’a donc pas besoin d’être installé globalement.

## Lancer le backend

Dans PowerShell :

```powershell
cd incidents-api
.\mvnw.cmd spring-boot:run
```

L’API est disponible sur `http://localhost:8080`.

Pour construire et lancer le JAR :

```powershell
.\mvnw.cmd clean package
java -jar target\incidents-api-0.0.1-SNAPSHOT.jar
```

La configuration par défaut utilise une base H2 en mémoire et le profil `dev`. Les données de démonstration sont recréées à chaque démarrage et l’initialisation est idempotente.

Console H2 de développement : `http://localhost:8080/h2-console`

- JDBC URL : `jdbc:h2:mem:incidentsdb`
- utilisateur : `sa`
- mot de passe : vide

La console H2 est destinée au développement local uniquement.

## Lancer le frontend

Dans un autre terminal PowerShell :

```powershell
cd incidents-front
npm install
npm run dev
```

L’interface est disponible sur `http://localhost:5173`.

Le frontend n’autorise l’API qu’à l’origine `http://localhost:5173`. Le backend doit donc rester sur le port `8080` et le frontend sur le port `5173` pour utiliser la configuration par défaut.

## Comptes de test

| Compte | Email | Mot de passe | Rôle |
|---|---|---|---|
| Administrateur | `admin@example.com` | `Admin123!` | ADMIN |
| Technicien 1 | `tech1@example.com` | `Tech123!` | TECHNICIAN |
| Technicien 2 | `tech2@example.com` | `Tech123!` | TECHNICIAN |
| Employé 1 | `employee1@example.com` | `Employee123!` | EMPLOYEE |
| Employé 2 | `employee2@example.com` | `Employee123!` | EMPLOYEE |

Une inscription publique crée toujours un compte `EMPLOYEE`. Le champ `role` éventuellement envoyé par le client est ignoré par le backend.

## Fonctionnalités

- authentification JWT HS256 avec access token à expiration par défaut de 5 minutes ;
- refresh token opaque stocké côté serveur sous forme de hash, avec rotation et révocation ;
- stockage BCrypt des mots de passe ;
- API stateless et CORS limité à `http://localhost:5173` ;
- contrôle des routes par rôle avec `@PreAuthorize` ;
- refus d’un token valide appartenant à un compte désactivé ;
- déclaration et consultation des incidents ;
- contrôle backend de la propriété et de l’affectation ;
- affectation réservée à l’ADMIN ;
- changements de statut contrôlés et historisés ;
- notes de traitement et historique ;
- gestion du profil et changement de mot de passe ;
- activation/désactivation des utilisateurs ;
- statistiques globales pour l’ADMIN ;
- messages d’erreur JSON homogènes avec validation des champs ;
- routes React protégées par authentification et par rôle ;
- interceptor Axios pour envoyer le JWT et gérer l’expiration de session.

## Endpoints principaux

### Authentification

| Méthode | Endpoint | Accès |
|---|---|---|
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| POST | `/auth/refresh` | Public avec refresh token |
| POST | `/auth/logout` | Public avec refresh token |

### Profil

| Méthode | Endpoint | Accès |
|---|---|---|
| GET | `/api/profile` | Authentifié |
| PUT | `/api/profile` | Authentifié |
| PUT | `/api/profile/password` | Authentifié |

Si l’email du profil change, `PUT /api/profile` renvoie un nouveau token car l’email est le subject du JWT.

### Incidents

| Méthode | Endpoint | Accès |
|---|---|---|
| POST | `/api/incidents` | EMPLOYEE |
| GET | `/api/incidents/my` | EMPLOYEE |
| GET | `/api/incidents/{id}` | Propriétaire, technicien affecté ou ADMIN |
| GET | `/api/incidents/assigned` | TECHNICIAN |
| GET | `/api/incidents` | ADMIN |
| PUT | `/api/incidents/{id}/status` | TECHNICIAN affecté ou ADMIN |
| PUT | `/api/incidents/{id}/assign/{technicianId}` | ADMIN |
| POST | `/api/incidents/{id}/comments` | TECHNICIAN affecté ou ADMIN |
| GET | `/api/incidents/{id}/comments` | Droits de lecture du détail |
| GET | `/api/incidents/{id}/history` | Droits de lecture du détail |

Transitions prises en charge :

- `OPEN -> ASSIGNED` par affectation ADMIN ;
- `ASSIGNED -> IN_PROGRESS` ;
- `OPEN -> IN_PROGRESS` par ADMIN ;
- `IN_PROGRESS -> RESOLVED` ;
- `RESOLVED -> CLOSED` ;
- `RESOLVED -> IN_PROGRESS` pour réouverture.

Un incident `CLOSED` ne peut plus être modifié.

### Administration

| Méthode | Endpoint | Accès |
|---|---|---|
| GET | `/api/admin/users` | ADMIN |
| PUT | `/api/admin/users/{id}/enable` | ADMIN |
| PUT | `/api/admin/users/{id}/disable` | ADMIN |
| GET | `/api/admin/statistics` | ADMIN |

Toutes les requêtes `/api/**` protégées doivent contenir :

```text
Authorization: Bearer <JWT>
```

Les réponses d’erreur ont notamment la forme suivante :

```json
{
  "status": 403,
  "message": "Accès interdit",
  "errors": []
}
```

Une erreur de validation `400` ajoute la liste `errors` avec les champs concernés.

## Tests automatisés

Les tests d’intégration MockMvc couvrent les 9 scénarios de sécurité demandés et plusieurs règles métier.

```powershell
cd incidents-api
.\mvnw.cmd test
```

## Scénario HTTP complet

Le scénario fonctionnel des 17 étapes est automatisable avec :

```powershell
.\scripts\phase4-scenario.ps1
```

Le script crée deux employés temporaires avec un email horodaté. Il ne montre jamais de JWT complet.

Pour exécuter automatiquement les tests HTTP de sécurité et du refresh token :

```powershell
.\scripts\phase7-automated-verification.ps1
```

Ce script vérifie les 9 scénarios de sécurité, la rotation, la révocation et le rejeu avec un nouvel access token. Les tokens ne sont jamais affichés.

## Profil MySQL optionnel

Le profil `mysql` est fourni dans `src/main/resources/application-mysql.properties`.

Exemple PowerShell :

```powershell
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/incidentsdb?useSSL=false&serverTimezone=UTC'
$env:MYSQL_USERNAME = 'root'
$env:MYSQL_PASSWORD = 'mot-de-passe-local'
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql
```

Ne jamais utiliser un vrai secret dans le dépôt. Pour remplacer le secret JWT de développement :

```powershell
$env:JWT_SECRET = 'une-valeur-secrete-locale-de-32-caracteres-minimum'
$env:JWT_EXPIRATION_MS = '300000'
$env:JWT_REFRESH_EXPIRATION_MS = '604800000'
```

Le secret JWT n’est pas affiché par l’application et ne doit pas être ajouté aux captures ou à la collection Postman.

## Postman

Les fichiers sont dans `postman/` :

- `incidents.postman_collection.json` : collection importable ;
- `incidents.postman_environment.json` : environnement avec `baseUrl`, comptes de test, tokens et identifiants de préparation.

Importer les deux fichiers dans Postman, sélectionner l’environnement `Incidents API - local`, puis exécuter d’abord le dossier `00 - Préparation`. Les requêtes de connexion enregistrent automatiquement les tokens dans l’environnement Postman.

## Refresh token

`POST /auth/login` renvoie un access token court et un refresh token opaque. Le refresh token n’est jamais stocké en clair en base : seul son hash SHA-256 est conservé.

`POST /auth/refresh` vérifie l’expiration, la révocation et l’état du compte, puis effectue une rotation : l’ancien refresh token devient inutilisable et un nouveau couple access/refresh est renvoyé.

`POST /auth/logout` révoque le refresh token courant. Le frontend Axios tente automatiquement un renouvellement sur un `401` d’une route protégée, puis rejoue la requête initiale une seule fois. Si le renouvellement échoue, la session locale est supprimée et l’utilisateur est redirigé vers `/login`.
