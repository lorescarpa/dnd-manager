# D&D Manager

Applicazione full-stack per gestire schede personaggio D&D 5e (SRD 5.1) e campagne.
Progetto personale per consolidare lo sviluppo backend; lo sviluppatore è esperto di Angular, conosce i fondamenti di Java e sta imparando Spring: spiega le scelte backend non ovvie.

## Documentazione di riferimento (fonte di verità)

- docs/domain.md: entità, vincoli, regole, permessi
- docs/api.yaml: contratto API (contract-first)
- docs/decisions.md: decisioni architetturali
  Non modificare questi file senza chiedere. Se il codice e i documenti divergono, fermati e segnalalo.

## Stack

- backend/: Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway, Maven
- frontend/: Angular (standalone, Signals, SignalStore), client generato da api.yaml
- docker-compose.yml: PostgreSQL per lo sviluppo locale

## Comandi

- Backend: `cd backend && ./mvnw verify`
- Frontend: `cd frontend && npm test`
- Database: `docker compose up -d`

## Convenzioni backend

- Package per funzionalità: auth, user, character, campaign, common
- Struttura: controller → service → repository. I controller implementano le interfacce generate da api.yaml
- Le entità JPA restano nel livello service: i service restituiscono DTO e la conversione avviene dentro la transazione.
- I calcoli D&D stanno in una classe di dominio pura, senza dipendenze Spring
- Schema del database solo tramite migrazioni Flyway; mai ddl-auto=update
- Errori con ProblemDetail e un @RestControllerAdvice centralizzato

## Regole di sicurezza (non negoziabili)

- ownerId e dmId vengono sempre dall'utente autenticato, mai dalla richiesta
- Una risorsa non accessibile restituisce 404, non 403
- Le password solo con hash BCrypt; non loggare mai token o password

## Test

- JUnit 5. Calcoli di dominio: test unitari puri. Controller: @WebMvcTest. Repository e integrazione: Testcontainers con PostgreSQL
- Ogni riga della tabella dei permessi in domain.md deve avere un test
- I test sulle formule li scrive lo sviluppatore: non modificarli per farli passare

## Modo di lavorare

- Un passo alla volta: prima proponi un piano breve, aspetta conferma, poi implementa
- Modifiche piccole e revisionabili; alla fine di ogni passo esegui i test
- Non aggiungere dipendenze senza chiedere
- Codice, nomi e commit in inglese (Conventional Commits); documentazione in italiano
