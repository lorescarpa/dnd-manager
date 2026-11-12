# Backend – D&D Manager API

API REST Spring Boot che implementa il contratto [docs/api.yaml](../docs/api.yaml).
Per il quadro generale del progetto vedi il [README principale](../README.md).

## Comandi

Su Windows usa `.\mvnw.cmd` al posto di `./mvnw`.

| Comando | Cosa fa |
| --- | --- |
| `./mvnw verify` | compila, genera il codice da `api.yaml` ed esegue tutti i test (serve Docker) |
| `./mvnw spring-boot:run` | avvia l'app su <http://localhost:8080> con il database di docker-compose |
| `./mvnw spring-boot:test-run` | avvia l'app con un PostgreSQL temporaneo (vedi sotto) |
| `./mvnw compile` | rigenera le interfacce e i modelli dal contratto, senza eseguire i test |

### Avvio con Testcontainers

```bash
./mvnw spring-boot:test-run
```

Avvia `TestDndApiApplication`: un PostgreSQL 17 in un container e il segreto JWT di test.
Non servono né docker-compose né `JWT_SECRET`. Il database riparte vuoto a ogni avvio.

## Configurazione

### Variabili d'ambiente

| Variabile | Obbligatoria | Default | Descrizione |
| --- | --- | --- | --- |
| `JWT_SECRET` | sì | – | chiave HMAC per firmare i token; almeno 32 byte, altrimenti l'app non parte |
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/dnd` | URL JDBC del database |
| `DB_USERNAME` | no | `dnd` | utente del database |
| `DB_PASSWORD` | no | `dnd` | password del database |

I default corrispondono a [docker-compose.yml](../docker-compose.yml).

### Altre proprietà ([application.yaml](src/main/resources/application.yaml))

| Proprietà | Valore | Note |
| --- | --- | --- |
| `app.jwt.expiration` | `PT3H` | durata dei token, formato ISO-8601 (`PT30M` = 30 minuti) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate controlla le entità ma non modifica mai lo schema |
| `spring.jpa.open-in-view` | `false` | le entità restano nel livello service |
| `spring.jackson.deserialization.fail-on-unknown-properties` | `true` | i campi fuori contratto (es. `ownerId`) danno `400` |
| `spring.web.locale` | `en` (fisso) | messaggi di errore sempre in inglese; le traduzioni le fa il frontend |

## Contract-first

Il contratto [docs/api.yaml](../docs/api.yaml) è la fonte di verità. A ogni build
`openapi-generator-maven-plugin` genera in `target/generated-sources/openapi`:

- le **interfacce** degli endpoint (`it.lorenzoscarpa.dnd.api`, es. `AuthApi`);
- i **modelli** di richiesta e risposta (`it.lorenzoscarpa.dnd.api.model`, es. `RegisterRequest`).

I controller implementano le interfacce generate. Il codice generato non va modificato:
per cambiare un endpoint si modifica `api.yaml` e si ricompila.

La build copia anche `api.yaml` in `static/openapi/`: la Swagger UI
(<http://localhost:8080/swagger-ui.html>) mostra il contratto così com'è, non una versione
ricavata dal codice.

## Struttura del codice

Package per funzionalità sotto `it.lorenzoscarpa.dnd`, con flusso controller → service → repository:

| Package | Contenuto |
| --- | --- |
| `auth` | registrazione e login (`AuthController`, `AuthService`, caricamento utente per email) |
| `user` | entità `UserEntity`, repository, `/users/me` |
| `common.security` | catena di sicurezza, JWT (`JwtConfig`, `TokenService`), utente corrente (`CurrentUser`) |
| `common.error` | `ApiExceptionHandler`: tutti gli errori come Problem Details (RFC 9457) |

Le regole di sicurezza (`ownerId` e `dmId` solo dal token, `404` per le risorse non accessibili,
BCrypt) sono in [CLAUDE.md](../CLAUDE.md#regole-di-sicurezza-non-negoziabili).

## Database e migrazioni

Lo schema è gestito solo da **Flyway**, in `src/main/resources/db/migration`.

- Nuove modifiche allo schema vanno in un nuovo file `V<n>__<descrizione>.sql`
  (es. `V2__add_character_notes.sql`).
- Una migrazione già applicata non si modifica: Flyway confronta il checksum e blocca l'avvio.
- All'avvio Hibernate verifica (`ddl-auto: validate`) che le entità corrispondano allo schema.

## Test

| Tipo | Strumento | Esempio |
| --- | --- | --- |
| Unitari | JUnit 5, senza Spring | `TokenServiceTest`, `JwtPropertiesTest` |
| Livello web | `@WebMvcTest` con la sicurezza reale (`SecurityTestConfig`) e service mockati | `AuthControllerTest` |
| Repository | `@DataJpaTest` + Testcontainers | `UserRepositoryTest` |
| Integrazione | `@SpringBootTest` + MockMvc + Testcontainers | `AuthIntegrationTest` |

Le proprietà solo per i test (es. il segreto JWT) sono in
`src/test/resources/config/application.yaml`, che si aggiunge a quello principale senza sostituirlo.

## Provare l'API

- **Swagger UI**: <http://localhost:8080/swagger-ui.html>. Esegui `POST /auth/login`, poi clicca
  **Authorize** e incolla l'`accessToken`.
- **REST Client** (VS Code): [api.http](api.http) contiene un percorso completo con due utenti (DM e
  giocatore). Esegui le richieste in ordine: token e id vengono salvati automaticamente.
