# Portail Web Backend — API REST Normes Internationales

API REST complète pour la gestion de normes internationales (ISO, IEC, EN) avec authentification JWT, gestion des abonnements SaaS, import Excel, stockage PDF et contrôle d'accès par rôles.

**Stack :** Spring Boot 4.0.5 | Java 17 | PostgreSQL | JWT (JJWT 0.11.5) | Flyway | Spring Data JPA | MapStruct | Lombok | Apache POI | Springdoc OpenAPI 3 | Maven

---

## Architecture du projet

### Structure des packages

```
src/main/java/portail/web/backend/exemple/portail/web/backend/
├── abonnement/
│   ├── controller/        # PlanAbonnementController, AbonnementController
│   ├── service/           # PlanAbonnementService, AbonnementService, AbonnementGuard
│   ├── repository/        # PlanAbonnementRepository, AbonnementRepository
│   ├── entity/            # PlanAbonnement, Abonnement, StatutAbonnement
│   ├── mapper/            # AbonnementMapper (MapStruct)
│   └── dto/               # Requêtes et réponses abonnement
├── paiement/
│   ├── controller/        # PaiementController
│   ├── service/           # PaiementService
│   ├── repository/        # PaiementRepository
│   ├── entity/            # Paiement, StatutPaiement, MethodePaiement
│   ├── mapper/            # PaiementMapper (MapStruct)
│   └── dto/
├── consultation/
│   ├── service/           # ConsultationService
│   ├── repository/        # ConsultationRepository
│   ├── entity/            # Consultation
│   └── dto/
├── auth/                  # AuthController + DTOs login/register
├── user/                  # User, UserRepository, UserService, UserDetailsServiceImpl
├── controller/            # NormeController, LookupController, AdminUserController, UserController
├── service/               # NormeService, LookupService, NormeExcelImportService
│   └── support/           # NormePdfStorage, NormeRelationResolver, LookupCatalog
├── repository/            # NormeRepository + 10 LookupRepositories
├── entity/                # Norme + AbstractLookupEntity + 10 entités lookup
├── dto/                   # NormeRequest, NormeResponse, NormeImportReport, LookupRequest/Response
├── mapper/                # NormeMapper, LookupMapper
├── security/              # SecurityConfig, JwtService, JwtAuthenticationFilter
├── config/                # OpenApiConfig, AdminUserInitializer
├── exception/             # GlobalExceptionHandler + exceptions métier
└── common/                # BaseTimestampEntity
```

### Architecture en couches

- **Controller** : endpoints HTTP, validation des requêtes, `@PreAuthorize`
- **Service** : logique métier, validation, orchestration
- **Repository** : Spring Data JPA avec `@EntityGraph` pour le chargement optimisé
- **Entity** : entités JPA avec contraintes et relations
- **Security** : JWT stateless, CSRF désactivé, CORS configuré

---

## Fonctionnalités

### Authentification JWT

- `POST /api/auth/register` — Inscription
- `POST /api/auth/login` — Connexion, retourne `accessToken` + `refreshToken`
- `POST /api/auth/refresh` — Renouvellement du token
- `GET /api/auth/me` — Profil de l'utilisateur connecté
- Tokens signés HS256, access token 15 min, refresh token 14 jours
- BCrypt pour les mots de passe

### Gestion des normes (publique + admin)

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/normes` | Public | Liste paginée avec filtres |
| `GET /api/normes/{id}` | Public | Détail d'une norme |
| `POST /api/normes` | ADMIN | Créer une norme |
| `PUT /api/normes/{id}` | ADMIN | Modifier une norme |
| `DELETE /api/normes/{id}` | ADMIN | Supprimer une norme |
| `POST /api/normes/{id}/pdf` | ADMIN | Uploader un PDF |
| `GET /api/normes/{id}/pdf` | USER/ADMIN | Télécharger le PDF (contrôle abonnement) |
| `POST /api/normes/import/excel` | ADMIN | Import en masse depuis .xlsx |

**Filtres disponibles :** `?search=iso&reference=ISO-001&statutId=1&icsLevel1Id=1&icsLevel2Id=12&icsLevel3Id=123`

### Import Excel des normes

Endpoint : `POST /api/normes/import/excel` (multipart/form-data)

Paramètres :
- `file` : fichier `.xlsx` obligatoire
- `onDuplicate` : `SKIP` (défaut) ou `UPDATE`

Colonnes du fichier Excel (en-tête non sensible à la casse) :

| Colonne | Obligatoire | Format |
|---|---|---|
| reference | Oui | texte |
| publicationDate | Non | yyyy-MM-dd |
| titreFr / titreEn / titreDe | Non | texte |
| descripteurFr / descripteurEn | Non | texte |
| documentIdentifier | Non | texte |
| includedInSubscription | Non | true/false/oui/non/1/0 |
| mandatory | Non | true/false/oui/non/1/0 |
| afnorIndex / printNumber / printDate | Non | texte / yyyy-MM-dd |
| regulationSpecifique | Non | texte |
| statutCode / documentTypeCode / collectionCode | Non | code entité référence |
| industrialBranchCode / productFamilyCode / subFamilyCode | Non | code entité référence |
| filter1Code / icsLevel1Code / icsLevel2Code / icsLevel3Code | Non | code entité référence |

Réponse :
```json
{
  "totalRows": 10,
  "created": 7,
  "updated": 1,
  "skipped": 1,
  "errors": 1,
  "rowErrors": [
    { "row": 5, "reference": "ISO-1234", "message": "Code introuvable: 'X99'" }
  ]
}
```

### Gestion des abonnements SaaS

#### Plans d'abonnement

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/admin/plans` | Public | Lister tous les plans |
| `POST /api/admin/plans` | ADMIN | Créer un plan |
| `PUT /api/admin/plans/{id}` | ADMIN | Modifier un plan |
| `DELETE /api/admin/plans/{id}` | ADMIN | Supprimer un plan |

Champs d'un plan :
- `nom` (unique), `description`, `prix`, `dureeMois`
- `illimite` (boolean) — si `true`, `nombreConsultations` doit être `null`
- `nombreConsultations` — obligatoire si `illimite = false`

#### Abonnements

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/admin/abonnements` | ADMIN | Tous les abonnements |
| `POST /api/admin/abonnements` | ADMIN | Créer un abonnement |
| `PUT /api/admin/abonnements/{id}` | ADMIN | Modifier un abonnement |
| `DELETE /api/admin/abonnements/{id}` | ADMIN | Annuler un abonnement |
| `GET /api/abonnements/me` | USER | Abonnement actif de l'utilisateur connecté |
| `GET /api/abonnements/me/consultations` | USER | Historique de consultations |

Statuts d'un abonnement : `PENDING` → `ACTIVE` → `EXPIRED` / `CANCELLED`

#### Paiements

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/admin/paiements` | ADMIN | Tous les paiements |
| `POST /api/admin/paiements` | ADMIN | Enregistrer un paiement |
| `GET /api/admin/paiements/abonnement/{id}` | ADMIN | Paiements d'un abonnement |

Règle automatique : un paiement avec `statutPaiement = COMPLETE` sur un abonnement `PENDING` le passe automatiquement à `ACTIVE`.

Méthodes de paiement : `CARTE_BANCAIRE`, `VIREMENT`, `CHEQUE`, `ESPECES`, `PAYPAL`

Statuts de paiement : `EN_ATTENTE`, `COMPLETE`, `ECHOUE`, `REMBOURSE`

### Contrôle d'accès aux PDF (AbonnementGuard)

Le composant `AbonnementGuard` intercepte chaque téléchargement de PDF :

1. Si l'utilisateur est ADMIN → accès direct sans vérification
2. Si la norme n'est pas `includedInSubscription` → accès libre
3. Sinon :
   - Vérifie qu'un abonnement `ACTIVE` existe avec `dateFin >= aujourd'hui`
   - Si plan non illimité : vérifie `consultationsRestantes > 0`, décrémente et sauvegarde
   - Enregistre la consultation dans la table `consultations`
   - Lance `SubscriptionRequiredException` (403) ou `ConsultationLimitExceededException` (403)

### Tables de référence (Lookups)

| Endpoint | Accès | Types disponibles |
|---|---|---|
| `GET /api/lookups/{type}` | Public | STATUT, DOCUMENT_TYPE, COLLECTION, INDUSTRIAL_BRANCH, PRODUCT_FAMILY, SUB_FAMILY, FILTER, ICS_LEVEL1, ICS_LEVEL2, ICS_LEVEL3 |
| `POST/PUT/DELETE /api/lookups/{type}` | ADMIN | Gestion CRUD |

### Gestion des utilisateurs (Admin)

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/users` | ADMIN | Liste paginée avec filtres username/role |
| `GET /api/users/{id}` | ADMIN | Détail utilisateur |
| `POST /api/users` | ADMIN | Créer utilisateur |
| `PUT /api/users/{id}` | ADMIN | Modifier utilisateur (mot de passe vide = inchangé) |
| `DELETE /api/users/{id}` | ADMIN | Supprimer utilisateur |

---

## Base de données

### Migrations Flyway

| Fichier | Contenu |
|---|---|
| V1__initial_schema.sql | Table `users` |
| V2__add_categories_and_standards.sql | Tables initiales (no-op sur DB fraîche) |
| V3__create_normes_and_lookup_tables.sql | Table `normes` + 10 tables lookup ICS |
| V4__add_norme_pdf_fields.sql | Colonnes PDF sur `normes` |
| V5__create_subscription_tables.sql | `plans_abonnement`, `abonnements`, `paiements`, `consultations` |
| V6__drop_orphan_tables.sql | Suppression tables orphelines `standards` et `categories` |

### Tables actives

- `users` — comptes utilisateurs
- `normes` — catalogue des normes avec métadonnées multilingues + PDF
- `statut`, `document_type`, `collection`, `industrial_branch`, `product_family`, `sub_family`, `filter1` — tables de référence
- `ics_level1`, `ics_level2`, `ics_level3` — hiérarchie ICS 3 niveaux
- `plans_abonnement` — plans tarifaires
- `abonnements` — abonnements utilisateurs
- `paiements` — historique des paiements
- `consultations` — historique des téléchargements PDF

---

## Tests

### Vue d'ensemble

| # | Type | Technologie | Portée | Nb tests |
|---|---|---|---|---|
| 1 | Tests unitaires | JUnit 5 + Mockito | Services, entités, sécurité | 69 |
| 2 | Tests contrôleurs | MockMvc + `@WebMvcTest` | Couche HTTP, codes de retour, JSON | 49 |
| 3 | Tests migration | Testcontainers + PostgreSQL 15 | Migrations Flyway, schéma réel | 11 |
| 4 | Analyse qualité | SonarQube 9.9 | Bugs, vulnérabilités, code smells, couverture | — |
| | **Total** | | | **129** |

---

### 1. Tests unitaires — JUnit 5 + Mockito

**Package :** `junit_test/`  
**Technologie :** JUnit 5 (`@ExtendWith(MockitoExtension.class)`), Mockito pour les dépendances simulées.  
**Portée :** logique métier pure, sans démarrage du contexte Spring.

```
junit_test/
├── AbonnementIsActifTest.java        (6 tests)  — isActif() : ACTIVE + dateFin >= today
├── JwtServiceTest.java               (7 tests)  — génération, validation, expiration, extraction JWT
├── AbonnementGuardTest.java          (9 tests)  — accès PDF : admin, norme libre, quota, abonnement actif
├── PlanAbonnementServiceTest.java   (10 tests)  — CRUD plans, règle illimité/nombreConsultations
├── AbonnementServiceTest.java        (8 tests)  — création, statut, calcul dateFin
├── PaiementServiceTest.java          (7 tests)  — paiement COMPLETE → abonnement ACTIVE
├── NormeServiceTest.java             (9 tests)  — CRUD norme, unicité référence, nettoyage PDF
└── NormeExcelImportServiceTest.java (12 tests)  — parsing xlsx, doublons, erreurs par ligne
```

**Caractéristiques :**
- Aucune base de données requise — toutes les dépendances sont mockées (`@Mock`, `@InjectMocks`)
- Cas nominaux + cas d'erreur couverts (exceptions métier, valeurs limites)
- Exécution rapide (< 5 secondes)

---

### 2. Tests contrôleurs — MockMvc + `@WebMvcTest`

**Package :** `mockmvc_test/`  
**Technologie :** `@WebMvcTest` (contexte Spring MVC partiel), `MockMvc` pour simuler les requêtes HTTP, `@MockitoBean` pour les services.  
**Portée :** couche HTTP uniquement — routing, codes de retour, sérialisation JSON, sécurité Spring.

```
mockmvc_test/
├── AuthControllerMvcTest.java           (6 tests)
│   ├── register : username disponible → 201 / username pris → 400
│   ├── login : identifiants valides → 200 + tokens / invalides → 401
│   └── GET /me : sans auth → 401 / avec auth → 200 + profil
│
├── NormeControllerMvcTest.java         (13 tests)
│   ├── GET liste paginée → 200, GET par id → 200 / 404
│   ├── POST créer → 201, référence vide → 400, référence absente → 400
│   ├── PUT modifier → 200 / 404, DELETE → 204
│   ├── GET PDF → 200 / 402 (sans abonnement)
│   └── POST import Excel → 200 + rapport
│
├── PlanAbonnementControllerMvcTest.java (11 tests)
│   ├── GET liste → 200, GET par id → 200 / 404
│   ├── POST créer → 201, nom vide → 400, illimite + consultations → 400
│   ├── PUT modifier → 200 / 404
│   └── DELETE → 204 / 404
│
├── AbonnementControllerMvcTest.java    (10 tests)
│   ├── GET liste admin → 200, GET par id → 200 / 404
│   ├── POST créer → 201, champ obligatoire manquant → 400
│   ├── PUT modifier → 200, DELETE → 204
│   └── GET /me → 200 (actif) / 204 (aucun), GET /me/consultations → 200
│
└── PaiementControllerMvcTest.java       (9 tests)
    ├── GET liste → 200, liste vide → 200
    ├── GET par id → 200 / 404
    ├── GET par abonnement → 200 / 200 vide
    └── POST enregistrer → 201 / 400 (montant nul) / 404 (abonnement inexistant)
```

**Caractéristiques :**
- Contexte Spring MVC partiel : pas de base de données, pas de Flyway
- Les services sont mockés — tests ciblés sur la couche contrôleur
- Vérifie les codes HTTP, la structure JSON, et les règles de sécurité (`@PreAuthorize`)

---

### 3. Tests migration Flyway — Testcontainers + PostgreSQL

**Package :** `testcontainers_test/`  
**Technologie :** `docker-java` avec API 1.47, PostgreSQL 15 dans un conteneur Docker isolé, `@SpringBootTest` complet avec `@DynamicPropertySource`.  
**Portée :** validation des 6 migrations Flyway sur une vraie base PostgreSQL vierge.

```
testcontainers_test/
└── FlywayMigrationTest.java (11 tests)
    ├── allSixMigrationsShouldBeAppliedWithSuccess   — 6 migrations SUCCESS
    ├── migrationVersionsShouldFollowSequence        — ordre V1→V6
    ├── v1_usersTableShouldExistWithRequiredColumns  — colonnes users
    ├── v3_normesAndAllLookupTablesShouldExist       — 11 tables (normes + lookups)
    ├── v3_normeIndexesShouldExist                  — 5 index (reference, statut, ics1/2/3)
    ├── v4_pdfColumnsShouldBeAddedToNormes          — 4 colonnes PDF
    ├── v5_subscriptionTablesShouldExist             — 4 tables abonnement
    ├── v5_subscriptionIndexesShouldExist            — 7 index abonnements/paiements/consultations
    ├── v6_standardsAndCategoriesTablesShouldBeDropped — tables supprimées après V6
    ├── fkConstraint_abonnementShouldReferencePlanAndUser — FK abonnements → users/plans
    └── fkConstraint_consultationShouldReferenceNormeAndUser — FK consultations → normes/users
```

**Caractéristiques :**
- Un conteneur PostgreSQL 15 est créé automatiquement via docker-java avant le démarrage du contexte Spring
- Flyway migrate depuis un schéma vide — aucune donnée résiduelle possible
- Le conteneur est détruit à la fin de la JVM (shutdown hook)
- **Prérequis :** Docker Desktop doit être démarré avec le port TCP 2375 activé (`Settings → General → Expose daemon on tcp://localhost:2375 without TLS`)

---

### 4. Analyse qualité — SonarQube

**Technologie :** SonarQube 9.9 LTS Community Edition, via `sonar-maven-plugin 4.0.0`.  
**Portée :** analyse statique du code source + import de la couverture JaCoCo.

#### Ce que SonarQube mesure

| Catégorie | Description |
|---|---|
| **Bugs** | Erreurs potentielles à l'exécution (null pointer, mauvaise comparaison…) |
| **Vulnérabilités** | Failles de sécurité (injection, exposition de données…) |
| **Code Smells** | Mauvaises pratiques, code difficile à maintenir |
| **Couverture** | % de lignes couvertes par les tests (importé depuis JaCoCo) |
| **Duplications** | Blocs de code dupliqués (CPD) |
| **Quality Gate** | Seuil global : passe si 0 bug, 0 vuln, couverture ≥ 80 % |

#### Prérequis — Démarrer SonarQube avec Docker

```powershell
docker run -d `
  --name sonarqube `
  -p 9000:9000 `
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true `
  sonarqube:lts-community
```

Attendre ~90 secondes, puis ouvrir **http://localhost:9000**  
Identifiants par défaut : `admin` / `admin`

#### Créer le projet et générer un token

1. **My Projects → Create project → Manually**
2. Project key : `portail-web-backend`
3. **Locally → Generate a token** (copier le token affiché)

#### Lancer l'analyse

```powershell
# Analyse complète (tests + sonar)
mvn clean verify sonar:sonar "-Dsonar.login=<TOKEN>"

# Sonar seul (si les tests ont déjà tourné)
mvn sonar:sonar "-Dsonar.login=<TOKEN>"
```

> **Sécurité :** ne jamais mettre le token dans `pom.xml`. Toujours le passer en argument `-Dsonar.login=...`.

#### Tableau de bord

```
http://localhost:9000/dashboard?id=portail-web-backend
```

#### Gérer le conteneur SonarQube

```powershell
docker stop sonarqube    # Arrêter
docker start sonarqube   # Relancer (conserve les données)
```

#### Configuration pom.xml (déjà en place)

Les propriétés suivantes sont préconfigurées dans `pom.xml` :

```xml
<sonar.projectKey>portail-web-backend</sonar.projectKey>
<sonar.projectName>Portail Web Backend</sonar.projectName>
<sonar.host.url>http://localhost:9000</sonar.host.url>
<sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
<sonar.exclusions>**/dto/**,**/entity/**,**/config/**,**/exception/**,**/common/**,**/*Application.java</sonar.exclusions>
```

---

### Lancer les tests

```bash
# Tous les tests
mvn test

# Un type spécifique
mvn test -Dtest="*JwtServiceTest,*AbonnementGuardTest"   # JUnit
mvn test -Dtest="*MvcTest"                               # MockMvc
mvn test -Dtest=FlywayMigrationTest                      # Testcontainers

# Tests + analyse SonarQube en une commande
mvn clean verify sonar:sonar "-Dsonar.login=<TOKEN>"
```

### Rapport de couverture JaCoCo

Généré automatiquement après `mvn test` :

```
target/site/jacoco/index.html   ← rapport HTML
target/site/jacoco/jacoco.xml   ← XML importé par SonarQube
```

Le rapport affiche par classe et par package :
- % de lignes couvertes
- % de branches couvertes
- % de méthodes couvertes

### Rapport Surefire détaillé

```bash
mvn surefire-report:report
```

Rapport HTML disponible dans :
```
target/site/surefire-report.html
```

---

## CI/CD — Jenkins + SonarQube + JMeter

### Architecture du pipeline

```
GitHub Push
    │
    ▼
┌─────────────────────────────────────────────────────┐
│                  Jenkins Pipeline                    │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Build   │→ │  Tests   │→ │  Code Coverage   │  │
│  │          │  │ 191 tests│  │  JaCoCo Report   │  │
│  └──────────┘  └──────────┘  └──────────────────┘  │
│                                        │             │
│                              ┌─────────▼──────────┐ │
│                              │  SonarQube Analysis│ │
│                              │  Quality Gate ✅   │ │
│                              └─────────┬──────────┘ │
│                                        │             │
│                              ┌─────────▼──────────┐ │
│                              │  Load Test JMeter  │ │
│                              │  500 users / 5000  │ │
│                              │  requêtes / 0 err  │ │
│                              └────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### Infrastructure

| Outil | Mode | Accès |
|---|---|---|
| **Jenkins** | Docker (`jenkis`) | `http://localhost:9090` |
| **SonarQube** | Docker (`sonarqube`) | `http://localhost:9000` |
| **JMeter** | Installé dans le conteneur Jenkins (`/jmeter`) | — |
| **PostgreSQL** | Local Windows | `localhost:5432` |
| **Spring Boot** | Local Windows | `localhost:8080` |

> Jenkins utilise `host.docker.internal` pour atteindre les services sur Windows depuis les conteneurs Docker.

### Démarrer Jenkins

```powershell
# Lancer le conteneur Jenkins (port 9090 → 8080)
docker start jenkis

# Accéder à Jenkins
http://localhost:9090
```

### Démarrer SonarQube

```powershell
docker start sonarqube
# Attendre ~90s puis ouvrir http://localhost:9000
```

### Jenkinsfile — Stages du pipeline

```
Stage 1 : Build          → mvn clean package -DskipTests
Stage 2 : Tests          → mvn test -P ci  (191 tests JUnit + MockMvc)
Stage 3 : Code Coverage  → mvn verify -P ci  (JaCoCo exec + rapport XML)
Stage 4 : SonarQube      → mvn sonar:sonar  (token via Jenkins Credentials)
Stage 5 : Load Test      → JMeter 500 threads × 5 loops = 5 000 requêtes
```

### Profil Maven `ci`

Le profil `-P ci` exclut les tests d'infrastructure (qui nécessitent PostgreSQL ou Docker-in-Docker) :

```xml
<profile>
    <id>ci</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>**/PortailWebBackendApplicationTests.java</exclude>
                        <exclude>**/FlywayMigrationTest.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Credentials Jenkins requis

| Credential ID | Type | Description |
|---|---|---|
| `sonar-token` | Secret text | Token SonarQube généré dans `My Account → Security` |

### Test de charge — JMeter (`load-test.jmx`)

| Paramètre | Valeur |
|---|---|
| Threads (utilisateurs) | 500 |
| Ramp-up | 10 secondes |
| Boucles par thread | 5 |
| Total requêtes | 5 000 |
| Requête 1 | `POST /api/auth/login` → extraction JWT |
| Requête 2 | `GET /api/normes` avec `Bearer {jwt_token}` |

---

### Résultats du dernier build (Build #16 — 2026-06-15)

#### Tests

| Classe de test | Tests | Résultat |
|---|---|---|
| JwtAuthenticationFilterTest | 6 | PASS |
| NormeServiceTest | 9 | PASS |
| ConsultationServiceTest | 4 | PASS |
| JwtServiceTest | 7 | PASS |
| LookupServiceTest | 8 | PASS |
| AbonnementGuardTest | 9 | PASS |
| GlobalExceptionHandlerTest | 6 | PASS |
| NormeExcelImportServiceTest | 12 | PASS |
| UserServiceTest | 15 | PASS |
| UserAdminServiceTest | 16 | PASS |
| PlanAbonnementServiceTest | 10 | PASS |
| AbonnementServiceTest | 8 | PASS |
| PaiementServiceTest | 7 | PASS |
| AbonnementIsActifTest | 6 | PASS |
| NormeControllerMvcTest | 13 | PASS |
| PlanAbonnementControllerMvcTest | 11 | PASS |
| AdminUserControllerMvcTest | 11 | PASS |
| LookupControllerMvcTest | 8 | PASS |
| AuthControllerMvcTest | 6 | PASS |
| PaiementControllerMvcTest | 9 | PASS |
| AbonnementControllerMvcTest | 10 | PASS |
| **TOTAL** | **191** | **0 failures, 0 errors** |

#### SonarQube Quality Gate

| Métrique | Résultat |
|---|---|
| Quality Gate | Passed |
| Coverage on New Code | ≥ 80% |
| Bugs | 0 |
| Vulnerabilities | 0 |

#### JMeter Load Test (500 utilisateurs simultanés)

| Métrique | Valeur |
|---|---|
| Total requêtes | 5 000 |
| Durée | 21 secondes |
| Débit | 237.7 req/s |
| Temps de réponse moyen | 1 176 ms |
| Temps min | 9 ms |
| Temps max | 4 344 ms |
| Taux d'erreur | **0.00%** |

#### Statut global

```
Build    ✅  SUCCESS
Tests    ✅  191/191 PASS
Coverage ✅  Quality Gate PASSED
SonarQube✅  Analyse uploadée
JMeter   ✅  5000 req — 0 erreurs
```

---

## Démarrage

### Prérequis

- Java 17+
- PostgreSQL 12+
- Maven 3.8+

### Configuration (`src/main/resources/application.properties`)

```properties
# Base de données
spring.datasource.url=jdbc:postgresql://localhost:5432/portail_web_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate

# JWT
jwt.secret=your-secret-key-minimum-32-characters-long
jwt.access-token-expiration-ms=900000
jwt.refresh-token-expiration-ms=1209600000

# Stockage PDF des normes
app.storage.norme-pdf-dir=storage/normes

# CORS (origines autorisées du frontend)
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173

# Admin auto-créé au démarrage (désactiver en production)
app.admin.seed.enabled=true
app.admin.username=admin
app.admin.password=admin123
```

### Lancement

```bash
mvn clean spring-boot:run
```

Swagger UI disponible sur :
```
http://localhost:8080/swagger-ui.html
```

### Build JAR

```bash
mvn clean package
java -jar target/portail-web-backend-0.0.1-SNAPSHOT.jar
```

---

## Exemples d'utilisation

### Connexion et utilisation du token

```bash
# Connexion
POST /api/auth/login
{ "username": "admin", "password": "admin123" }

# Utiliser le token dans les requêtes suivantes
Authorization: Bearer {accessToken}
```

### Créer un plan et un abonnement

```bash
# 1. Créer un plan
POST /api/admin/plans
{
  "nom": "Plan Standard",
  "description": "50 consultations / an",
  "prix": 99.99,
  "dureeMois": 12,
  "nombreConsultations": 50,
  "illimite": false
}

# 2. Créer un abonnement pour un user
POST /api/admin/abonnements
{ "userId": 5, "planId": 1, "dateDebut": "2026-01-01" }

# 3. Enregistrer le paiement → active automatiquement l'abonnement
POST /api/admin/paiements
{
  "abonnementId": 1,
  "montant": 99.99,
  "methodePaiement": "CARTE_BANCAIRE",
  "referenceTransaction": "TXN-2026-001",
  "statutPaiement": "COMPLETE"
}
```

### Import Excel de normes

```bash
POST /api/normes/import/excel
Authorization: Bearer {adminToken}
Content-Type: multipart/form-data

file=normes.xlsx
onDuplicate=SKIP
```

---

## Sécurité

### Règles d'accès

| Route | Règle |
|---|---|
| `GET /api/normes`, `GET /api/normes/{id}` | Public |
| `GET /api/lookups/**` | Public |
| `GET /api/admin/plans` | Public |
| `GET /api/normes/{id}/pdf` | USER ou ADMIN + abonnement actif |
| `POST/PUT/DELETE /api/normes/**` | ADMIN uniquement |
| `POST /api/normes/import/excel` | ADMIN uniquement |
| `POST/PUT/DELETE /api/admin/plans` | ADMIN uniquement |
| `GET/POST/PUT/DELETE /api/admin/abonnements` | ADMIN uniquement |
| `GET/POST /api/admin/paiements` | ADMIN uniquement |
| `GET /api/abonnements/me` | USER connecté |

### Exceptions métier

| Exception | Code HTTP | Cause |
|---|---|---|
| `SubscriptionRequiredException` | 403 | PDF protégé sans abonnement actif |
| `ConsultationLimitExceededException` | 403 | Quota de consultations épuisé |
| `ResourceNotFoundException` | 404 | Entité introuvable |
| `BadRequestException` | 400 | Données invalides |

---

## Dépendances clés

| Dépendance | Version | Rôle |
|---|---|---|
| spring-boot-starter-web | 4.0.5 | API REST |
| spring-boot-starter-data-jpa | 4.0.5 | ORM JPA/Hibernate |
| spring-boot-starter-security | 4.0.5 | Sécurité |
| spring-boot-starter-validation | 4.0.5 | Validation des DTOs |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.11.5 | Tokens JWT |
| flyway-database-postgresql | géré par parent | Migrations SQL |
| postgresql | géré par parent | Driver JDBC |
| springdoc-openapi-starter-webmvc-ui | 3.0.2 | Swagger UI |
| mapstruct | 1.5.5.Final | Mapping entité ↔ DTO |
| lombok | géré par parent | Réduction boilerplate |
| poi-ooxml | 5.3.0 | Lecture fichiers Excel |
| jacoco-maven-plugin | 0.8.12 | Rapport de couverture tests |

---

## Problèmes courants

**"No session" / lazy loading**
Ajouter `@EntityGraph` sur les méthodes du repository concernées.

**JWT expiré**
Appeler `POST /api/auth/refresh` avec le `refreshToken` pour obtenir un nouveau `accessToken`.

**403 sur téléchargement PDF**
L'utilisateur n'a pas d'abonnement actif (`ACTIVE` + `dateFin >= aujourd'hui`) ou son quota de consultations est épuisé.

**Erreur Flyway au démarrage**
Vérifier que toutes les migrations V1→V6 sont présentes dans `src/main/resources/db/migration/`. Ne jamais modifier un fichier de migration existant.

**Import Excel échoue avec erreur 400**
- Le fichier doit être `.xlsx` (pas `.xls` ni `.csv`)
- La colonne `reference` est obligatoire dans l'en-tête
- Les codes de référence (statutCode, icsLevel1Code, etc.) doivent exister en base
