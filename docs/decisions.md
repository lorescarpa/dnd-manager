# Decisioni di progetto

Scelte prese durante la stesura di `api.yaml` dove `domain.md` non era esplicito.

## API v1 (2026-11-11)

- **Campagne per personaggio**: al massimo una (risolto il segnaposto in `domain.md`).
- **campaignId**: non inviabile dal client; cambia solo con `POST /campaigns/join` e
  `DELETE /campaigns/{id}/characters/{characterId}`.
- **speed in piedi** (int 0–120), come nell'SRD. Default dalla razza: 25 ft per nano,
  gnomo e halfling; 30 ft per le altre.
- **Default in creazione**: `level` 1, `currentHp` = `maxHp`, `temporaryHp` 0,
  competenze e inventario vuoti.
- **Login**: restituisce solo `{ accessToken, tokenType: "Bearer" }`, senza `expiresIn`.
- **Registrazione**: `201` con il profilo utente, senza token.
- **Password**: 8–72 caratteri (72 è il limite di bcrypt).
- **Inventario**: incorporato nella scheda; ogni PUT sostituisce l'intera lista. Gli `id`
  degli oggetti sono solo in risposta.
- **PUT personaggio**: sostituzione completa, con `version` obbligatorio (`409` se non coincide).
- **PUT campagna**: senza `version`, vale l'ultima scrittura.
- **Liste**: `CharacterSummary` e `CampaignSummary` invece degli oggetti completi.
- **Campagna in risposta**: `dm { id, displayName }` e `participants`
  `[{ userId, displayName, characterId, characterName }]`; `inviteCode` solo per il DM.
- **Codici enum**: inglese in maiuscolo con underscore; razze base SRD senza sottorazze.
- **Errori**: Problem Details. `404` se l'utente non può leggere la risorsa, `403` se può
  leggerla ma non eseguire l'azione. Join: `404` codice inesistente, `409` personaggio già
  assegnato o utente già presente, `422` personaggio non proprio o utente DM.
