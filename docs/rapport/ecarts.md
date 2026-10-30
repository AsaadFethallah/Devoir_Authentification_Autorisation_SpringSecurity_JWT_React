# Écarts constatés et résultats de vérification

## 1. Périmètre de l’audit

L’audit a été réalisé le 30 octobre 2026 à partir du code présent dans le projet et du sujet `devoir_jwt_spring_security_react.pdf`. Le code n’a pas été modifié.

La commande exécutée est la suivante, depuis `incidents-api` :

```text
.\mvnw.cmd test
```

Résultat réel :

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Les 15 tests sont regroupés dans `ma.formation.incidentsapi.SecurityIntegrationTest`.

## 2. Couverture automatisée des tests obligatoires du §25

| Test demandé au §25 | Test automatisé correspondant | Résultat réel |
|---|---|---|
| 1. Route protégée sans JWT | `protectedRouteWithoutJwtReturns401` | PASS — HTTP 401 |
| 2. JWT invalide | `invalidJwtReturns401` | PASS — HTTP 401 |
| 3. JWT expiré | `expiredJwtReturns401` | PASS — HTTP 401 |
| 4. EMPLOYEE sur une route ADMIN | `employeeCannotAccessAdminRoute` | PASS — HTTP 403 |
| 5. EMPLOYEE sur l’incident d’un autre utilisateur | `employeeCannotReadAnotherEmployeesIncident` | PASS — HTTP 403 |
| 6. TECHNICIAN sur un incident qui ne lui est pas affecté | `technicianCannotModifyUnassignedIncident` | PASS — HTTP 403 |
| 7. Utilisateur désactivé | `disabledUserCannotLoginOrUseExistingToken` | PASS — connexion et jeton existant refusés en HTTP 401 |
| 8. Email déjà utilisé | `duplicateEmailReturns409` | PASS — HTTP 409 |
| 9. Création d’un incident invalide | `invalidIncidentReturns400WithFieldErrors` | PASS — HTTP 400 avec erreurs de validation |

Les neuf tests obligatoires du §25 ont donc chacun un équivalent automatisé. Aucun test obligatoire ne reste sans équivalent automatisé dans la suite exécutée.

## 3. Tests automatisés complémentaires

Les six tests suivants portent sur des règles supplémentaires présentes dans le code :

| Test automatisé | Résultat réel |
|---|---|
| `closedIncidentCannotChangeStatus` | PASS |
| `validStatusTransitionsAndReopeningAreAccepted` | PASS |
| `registrationAlwaysCreatesEmployeeEvenWhenAdminIsSent` | PASS |
| `refreshRotatesTheRefreshTokenAndReturnsANewAccessToken` | PASS |
| `logoutRevokesTheRefreshToken` | PASS |
| `disabledUserCannotRefreshAnExistingRefreshToken` | PASS |

## 4. Écarts ou points à signaler par rapport au sujet

### Captures d’écran

Le dossier `docs/captures/` contient maintenant trois fichiers : `Screen (1).png`, `Screen (2).png` et `Screen (3).png`. Aucun quatrième fichier n’a été trouvé. Les trois captures présentes sont intégrées au rapport sous forme de copies référencées, avec masquage des tokens visibles dans la première image. La quatrième étape est documentée par le scénario automatisé Phase 4 ; aucune image supplémentaire n’est inventée.

### Relations JPA inverses

Le code implémente les relations vers les parents avec `@ManyToOne(fetch = FetchType.LAZY)` : un incident possède un créateur et peut avoir un technicien affecté ; un historique et un commentaire possèdent un incident et un auteur. Les entités ne déclarent pas de collections `@OneToMany(mappedBy = ...)` inverses dans `User` ou `Incident`.

Il s’agit d’un choix de modélisation réel, compatible avec l’utilisation de DTO et de repositories, mais différent d’un modèle bidirectionnel illustrant explicitement les deux annotations demandées dans la partie recherche du sujet.

### Transition d’état décrite au §24

Dans le scénario du §24, le texte indique une transition `OPEN -> IN_PROGRESS` après l’affectation. Le code réalise d’abord l’affectation avec l’état `ASSIGNED`, puis autorise la transition `ASSIGNED -> IN_PROGRESS` par le technicien. Le code suit ainsi une séquence intermédiaire plus détaillée, mais elle ne correspond pas littéralement à la transition écrite dans le scénario du PDF.

### Console H2

La route `/h2-console/**` est autorisée dans `SecurityConfig` sans condition explicite liée au profil actif. La configuration MySQL désactive H2, mais le code de sécurité ne limite pas lui-même cette autorisation à un profil de développement. Pour un déploiement réel, cette exposition devrait être supprimée ou conditionnée à l’environnement de développement.

### Configuration CORS

La propriété `app.cors.allowed-origin` existe dans la configuration, mais l’origine autorisée est actuellement écrite directement dans `SecurityConfig` (`http://localhost:5173`). La fonctionnalité attendue pour le frontend local est présente, mais la propriété n’est pas utilisée comme source de configuration effective.

## 5. Conclusion de l’étape

La suite Maven est verte : 15 tests sur 15 réussissent. Les neuf contrôles obligatoires du §25 sont couverts automatiquement. Les écarts documentés ci-dessus devront être signalés dans le rapport final et ne doivent pas être masqués par des captures ou des affirmations non vérifiées.
