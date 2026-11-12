# D&D Manager

Applicazione web per creare e gestire schede personaggio di **Dungeons & Dragons 5e (SRD 5.1)**
e condividerle all'interno di campagne gestite da un Master (DM).

## Stato del progetto

| Parte | Stato |
| --- | --- |
| Modello di dominio e contratto API | completati (`docs/`) |
| Backend: autenticazione (registrazione, login JWT, profilo) | completato |
| Backend: personaggi e campagne | da fare |
| Frontend Angular | non ancora iniziato |

## Stack

- **Backend**: Java 21, Spring Boot 4, Spring Security (JWT HS256), Spring Data JPA, Flyway, Maven
- **Database**: PostgreSQL 17
- **Frontend**: Angular (standalone, Signals, SignalStore), client generato dal contratto API
- **API**: contract-first con OpenAPI 3.1 (`docs/api.yaml`)

## Struttura del repository

```
dnd-manager/
├── backend/            API REST Spring Boot (vedi backend/README.md)
├── frontend/           applicazione Angular (in arrivo)
├── docs/
│   ├── domain.md       entità, vincoli, regole e permessi
│   ├── api.yaml        contratto OpenAPI: fonte di verità per backend e frontend
│   └── decisions.md    decisioni di progetto
├── docker-compose.yml  PostgreSQL per lo sviluppo locale
└── CLAUDE.md           istruzioni per l'agente di sviluppo (Claude Code)
```

## Prerequisiti

- **JDK 21**
- **Docker**: per il database locale e per i test (Testcontainers)
- Maven non serve: il backend include il Maven Wrapper (`mvnw`)

## Avvio rapido

1. **Avvia il database**

   ```bash
   docker compose up -d
   ```

2. **Imposta il segreto per firmare i JWT** (almeno 32 caratteri; l'app non parte senza)

   ```powershell
   # PowerShell
   $env:JWT_SECRET = "una-stringa-casuale-lunga-almeno-32-caratteri"
   ```

   ```bash
   # bash / zsh
   export JWT_SECRET="una-stringa-casuale-lunga-almeno-32-caratteri"
   ```

3. **Avvia il backend**

   ```bash
   cd backend
   ./mvnw spring-boot:run      # su Windows: .\mvnw.cmd spring-boot:run
   ```

4. **Prova l'API**
   - Swagger UI: <http://localhost:8080/swagger-ui.html>. Fai login e usa **Authorize** con il token ricevuto.
   - Oppure le richieste pronte in [backend/api.http](backend/api.http), con l'estensione REST Client di VS Code.

Per avviare il backend senza docker-compose e senza `JWT_SECRET`, vedi
[Avvio con Testcontainers](backend/README.md#avvio-con-testcontainers).

## Test

```bash
cd backend
./mvnw verify
```

Docker deve essere attivo: i test di integrazione usano un PostgreSQL temporaneo.

## Documentazione

- [Modello di dominio](docs/domain.md): entità, vincoli, regole e tabella dei permessi
- [Contratto API](docs/api.yaml): endpoint, schemi, errori (consultabile anche da Swagger UI)
- [Decisioni di progetto](docs/decisions.md): le scelte fatte dove il dominio non era esplicito
- [Backend](backend/README.md): configurazione, struttura del codice, migrazioni, test

## Flusso di lavoro

- `develop` è il branch di integrazione, `main` si usa solo per il deploy.
- Ogni funzionalità va su un branch `feat/<nome>` creato da `develop` e riunito in `develop`.
- Commit in inglese secondo [Conventional Commits](https://www.conventionalcommits.org/);
  documentazione in italiano.

## Licenza

[CC0 1.0 Universal](LICENSE). I contenuti di gioco seguono l'SRD 5.1 di Dungeons & Dragons.
