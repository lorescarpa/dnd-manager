# Modello di dominio – D&D Manager

## Obiettivo

Applicazione web per creare e gestire schede personaggio di D&D 5e (SRD 5.1)
e condividerle all'interno di campagne gestite da un Master.

## Glossario

- **Master (DM)**: utente che crea e gestisce una campagna.
- **Giocatore**: utente che partecipa a una campagna con un proprio personaggio.
- **Campagna**: sessione di gioco in cui un gruppo di giocatori (il party) interpreta gli stessi personaggi attraverso un arco narrativo che si sviluppa nel tempo

## Entità

### Utente

| Campo        | Tipo      | Vincoli               | Note                         |
| ------------ | --------- | --------------------- | ---------------------------- |
| id           | UUID      | PK                    |                              |
| email        | string    | unica, obbligatoria   | usata per il login           |
| passwordHash | string    | obbligatoria          | mai esposta nelle API        |
| displayName  | string    | 3–30 caratteri        | nome mostrato nelle campagne |
| createdAt    | timestamp | impostato dal sistema |                              |

Regole:

- Un utente può possedere più personaggi.
- Un utente può essere Master di una campagna e Giocatore in un'altra.

### Personaggio

#### Campi salvati

| Campo                    | Tipo                | Vincoli                                    | Persistenza / note                                                         |
| ------------------------ | ------------------- | ------------------------------------------ | -------------------------------------------------------------------------- |
| id                       | UUID                | PK, generato dal sistema                   |                                                                            |
| ownerId                  | UUID                | FK → Utente, obbligatorio                  | impostato dal backend dall'utente autenticato, mai dal client              |
| campaignId               | UUID                | FK → Campagna                              |                                                                            |
| name                     | string              | 1–50 caratteri, obbligatorio               |                                                                            |
| race                     | enum Race           | obbligatorio, valori SRD 5.1               | [D] enum in v1, tabella se in futuro si importa l'SRD                      |
| characterClass           | enum CharacterClass | obbligatorio, valori SRD 5.1               | [D] come race                                                              |
| subclass                 | string              | opzionale, max 50 caratteri                | testo libero in v1                                                         |
| level                    | int                 | 1–20, default 1                            |                                                                            |
| alignment                | enum Alignment      | obbligatorio (9 valori)                    |                                                                            |
| abilityScores            | 6 × int             | ciascuno 1–20                              | 6 colonne (str_score … cha_score), oggetto incorporato                     |
| savingThrowProficiencies | set AbilityKey      | 0–6 valori, nessun duplicato               | tabella figlia o colonna array                                             |
| skillProficiencies       | set SkillKey        | valori tra le 18 abilità, nessun duplicato | tabella figlia o colonna array                                             |
| armorClass               | int                 | 1–30                                       | inserita dall'utente (niente calcolo dall'armatura in v1)                  |
| speed                    | int                 | 0–120                                      | in m; default dalla razza in fase di creazione                             |
| maxHp                    | int                 | ≥ 1                                        | [D] salvato, non calcolato (i giocatori tirano i dadi)                     |
| currentHp                | int                 | 0 ≤ currentHp ≤ maxHp                      |                                                                            |
| temporaryHp              | int                 | ≥ 0                                        |                                                                            |
| version                  | int                 | gestito dal sistema                        | blocco ottimistico: aggiornamento rifiutato se la versione non corrisponde |
| createdAt / updatedAt    | timestamp           | gestiti dal sistema                        |                                                                            |

#### Inventario (entità figlia, 1 personaggio → N oggetti)

| Campo       | Tipo   | Vincoli                      | Note                              |
| ----------- | ------ | ---------------------------- | --------------------------------- |
| id          | UUID   | PK                           |                                   |
| characterId | UUID   | FK → Personaggio             | cancellato insieme al personaggio |
| name        | string | 1–100 caratteri              | testo libero, anche per le armi   |
| quantity    | int    | ≥ 1                          |                                   |
| notes       | string | opzionale, max 500 caratteri | danni, effetti, descrizione       |

#### Campi calcolati (non salvati, calcolati dal backend in lettura)

| Campo              | Formula                                                                                |
| ------------------ | -------------------------------------------------------------------------------------- |
| abilityModifier(X) | ⌊(punteggio X − 10) / 2⌋                                                               |
| proficiencyBonus   | 2 + ⌊(level − 1) / 4⌋                                                                  |
| initiative         | abilityModifier(DEX)                                                                   |
| savingThrow(X)     | abilityModifier(X) + (X ∈ savingThrowProficiencies ? proficiencyBonus : 0)             |
| skill(S)           | abilityModifier(caratteristica di S) + (S ∈ skillProficiencies ? proficiencyBonus : 0) |
| passivePerception  | 10 + skill(Percezione)                                                                 |

#### Regole

- Un personaggio ha un solo proprietario; la proprietà non è trasferibile.
- Un personaggio partecipa a [una sola campagna o più campagne].
- Cambiare il livello non modifica maxHp: l'utente lo aggiorna a mano.
- La caratteristica associata a ogni abilità è fissa (dati di riferimento, non modificabili).

#### Fuori scope (v1)

- Multiclasse, maestria (expertise), incantesimi, catalogo armi/oggetti dall'SRD,
  calcolo della CA dall'equipaggiamento, privilegi di classe e razza.

### Campagna

#### Campi salvati

| Campo                 | Tipo      | Vincoli                                 | Note                                                          |
| --------------------- | --------- | --------------------------------------- | ------------------------------------------------------------- |
| id                    | UUID      | PK, generato dal sistema                |                                                               |
| title                 | string    | 1–100 caratteri, obbligatorio           |                                                               |
| description           | string    | opzionale, max 2000 caratteri           |                                                               |
| dmId                  | UUID      | FK → Utente, obbligatorio               | impostato dal backend dall'utente autenticato, mai dal client |
| inviteCode            | string    | unico, 8 caratteri alfanumerici casuali | generato alla creazione, rigenerabile dal DM                  |
| createdAt / updatedAt | timestamp | gestiti dal sistema                     |                                                               |

#### Legame con i personaggi

- Personaggio.campaignId: UUID nullable, FK → Campagna.
- I partecipanti della campagna sono i proprietari dei personaggi collegati (dato derivato, non salvato).

#### Regole

- Un personaggio appartiene al massimo a una campagna.
- Un utente può avere al massimo un personaggio per campagna.
- Il DM non può collegare propri personaggi alla campagna che gestisce. [D]
- Un giocatore entra inserendo l'inviteCode e scegliendo uno dei propri personaggi non ancora assegnati.
- Rigenerare l'inviteCode invalida quello precedente; i partecipanti già presenti restano.
- Un giocatore può ritirare il proprio personaggio; il DM può rimuovere qualsiasi personaggio.
- Eliminare una campagna scollega i personaggi (campaignId = null), non li cancella.
- Eliminare un personaggio lo rimuove dalla campagna.

#### Fuori scope (v1)

- Stato della campagna (attiva/archiviata), numero massimo di giocatori, passaggio del ruolo di DM,
  inviti diretti via email, sessioni e note di gioco, PNG gestiti dal DM.

## Permessi

| Azione                                  | Proprietario PG | DM della campagna del PG | Altro giocatore della stessa campagna | Altro utente |
| --------------------------------------- | --------------- | ------------------------ | ------------------------------------- | ------------ |
| Leggere scheda personaggio              | sì              | sì                       | sì in sola lettura                    | no           |
| Modificare scheda personaggio           | sì              | no                       | no                                    | no           |
| Eliminare personaggio                   | sì              | no                       | no                                    | no           |
| Leggere campagna (titolo, partecipanti) | —               | sì                       | sì                                    | no           |
| Modificare/eliminare campagna           | —               | sì                       | no                                    | no           |
| Vedere/rigenerare inviteCode            | —               | sì                       | no                                    | no           |
| Rimuovere un personaggio dalla campagna | sì (il proprio) | sì                       | no                                    | no           |
