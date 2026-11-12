-- Schema iniziale di D&D Manager (vedi docs/domain.md).
-- Gestiti dal backend, non dal database:
--   - updated_at e version (JPA @UpdateTimestamp / @Version);
--   - il DM non può collegare propri personaggi alla campagna che gestisce;
--   - l'abbinamento abilità → caratteristica (dato di riferimento nel codice).

-- ---------------------------------------------------------------------------
-- Utenti
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            uuid         NOT NULL DEFAULT gen_random_uuid(),
    email         varchar(254) NOT NULL,
    password_hash varchar(255) NOT NULL,
    display_name  varchar(30)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_display_name_length CHECK (char_length(display_name) BETWEEN 3 AND 30)
);

-- Email unica senza distinguere maiuscole e minuscole.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- ---------------------------------------------------------------------------
-- Campagne
-- ---------------------------------------------------------------------------
CREATE TABLE campaigns (
    id          uuid          NOT NULL DEFAULT gen_random_uuid(),
    title       varchar(100)  NOT NULL,
    description varchar(2000),
    dm_id       uuid          NOT NULL,
    invite_code char(8)       NOT NULL,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT pk_campaigns PRIMARY KEY (id),
    CONSTRAINT fk_campaigns_dm FOREIGN KEY (dm_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_campaigns_invite_code UNIQUE (invite_code),
    CONSTRAINT ck_campaigns_title_length CHECK (char_length(title) BETWEEN 1 AND 100),
    CONSTRAINT ck_campaigns_invite_code_format CHECK (invite_code ~ '^[A-Za-z0-9]{8}$')
);

CREATE INDEX ix_campaigns_dm_id ON campaigns (dm_id);

-- ---------------------------------------------------------------------------
-- Personaggi
-- ---------------------------------------------------------------------------
CREATE TABLE characters (
    id              uuid        NOT NULL DEFAULT gen_random_uuid(),
    owner_id        uuid        NOT NULL,
    campaign_id     uuid,
    name            varchar(50) NOT NULL,
    race            varchar(20) NOT NULL,
    character_class varchar(20) NOT NULL,
    subclass        varchar(50),
    level           integer     NOT NULL DEFAULT 1,
    alignment       varchar(20) NOT NULL,
    str_score       integer     NOT NULL,
    dex_score       integer     NOT NULL,
    con_score       integer     NOT NULL,
    int_score       integer     NOT NULL,
    wis_score       integer     NOT NULL,
    cha_score       integer     NOT NULL,
    armor_class     integer     NOT NULL,
    speed           integer     NOT NULL,           -- in piedi (ft)
    max_hp          integer     NOT NULL,
    current_hp      integer     NOT NULL,
    temporary_hp    integer     NOT NULL DEFAULT 0,
    version         bigint      NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_characters PRIMARY KEY (id),
    CONSTRAINT fk_characters_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT,
    -- Eliminare una campagna scollega i personaggi, non li cancella.
    CONSTRAINT fk_characters_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE SET NULL,
    -- Al massimo un personaggio per utente in ogni campagna (i NULL non entrano in conflitto).
    CONSTRAINT uq_characters_owner_campaign UNIQUE (owner_id, campaign_id),

    CONSTRAINT ck_characters_name_length CHECK (char_length(name) BETWEEN 1 AND 50),
    CONSTRAINT ck_characters_race CHECK (race IN (
        'DRAGONBORN', 'DWARF', 'ELF', 'GNOME', 'HALF_ELF',
        'HALFLING', 'HALF_ORC', 'HUMAN', 'TIEFLING')),
    CONSTRAINT ck_characters_class CHECK (character_class IN (
        'BARBARIAN', 'BARD', 'CLERIC', 'DRUID', 'FIGHTER', 'MONK',
        'PALADIN', 'RANGER', 'ROGUE', 'SORCERER', 'WARLOCK', 'WIZARD')),
    CONSTRAINT ck_characters_alignment CHECK (alignment IN (
        'LAWFUL_GOOD', 'NEUTRAL_GOOD', 'CHAOTIC_GOOD',
        'LAWFUL_NEUTRAL', 'NEUTRAL', 'CHAOTIC_NEUTRAL',
        'LAWFUL_EVIL', 'NEUTRAL_EVIL', 'CHAOTIC_EVIL')),
    CONSTRAINT ck_characters_level CHECK (level BETWEEN 1 AND 20),
    CONSTRAINT ck_characters_ability_scores CHECK (
        str_score BETWEEN 1 AND 20 AND dex_score BETWEEN 1 AND 20 AND
        con_score BETWEEN 1 AND 20 AND int_score BETWEEN 1 AND 20 AND
        wis_score BETWEEN 1 AND 20 AND cha_score BETWEEN 1 AND 20),
    CONSTRAINT ck_characters_armor_class CHECK (armor_class BETWEEN 1 AND 30),
    CONSTRAINT ck_characters_speed CHECK (speed BETWEEN 0 AND 120),
    CONSTRAINT ck_characters_max_hp CHECK (max_hp >= 1),
    CONSTRAINT ck_characters_current_hp CHECK (current_hp BETWEEN 0 AND max_hp),
    CONSTRAINT ck_characters_temporary_hp CHECK (temporary_hp >= 0)
);

CREATE INDEX ix_characters_campaign_id ON characters (campaign_id);

-- ---------------------------------------------------------------------------
-- Competenze (tabelle figlie: la PK composta impedisce i duplicati)
-- ---------------------------------------------------------------------------
CREATE TABLE character_saving_throw_proficiencies (
    character_id uuid       NOT NULL,
    ability      varchar(3) NOT NULL,

    CONSTRAINT pk_character_saving_throw_proficiencies PRIMARY KEY (character_id, ability),
    CONSTRAINT fk_character_saving_throw_proficiencies_character
        FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE,
    CONSTRAINT ck_character_saving_throw_proficiencies_ability
        CHECK (ability IN ('STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA'))
);

CREATE TABLE character_skill_proficiencies (
    character_id uuid        NOT NULL,
    skill        varchar(20) NOT NULL,

    CONSTRAINT pk_character_skill_proficiencies PRIMARY KEY (character_id, skill),
    CONSTRAINT fk_character_skill_proficiencies_character
        FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE,
    CONSTRAINT ck_character_skill_proficiencies_skill CHECK (skill IN (
        'ACROBATICS', 'ANIMAL_HANDLING', 'ARCANA', 'ATHLETICS', 'DECEPTION', 'HISTORY',
        'INSIGHT', 'INTIMIDATION', 'INVESTIGATION', 'MEDICINE', 'NATURE', 'PERCEPTION',
        'PERFORMANCE', 'PERSUASION', 'RELIGION', 'SLEIGHT_OF_HAND', 'STEALTH', 'SURVIVAL'))
);

-- ---------------------------------------------------------------------------
-- Inventario
-- ---------------------------------------------------------------------------
CREATE TABLE inventory_items (
    id           uuid         NOT NULL DEFAULT gen_random_uuid(),
    character_id uuid         NOT NULL,
    name         varchar(100) NOT NULL,
    quantity     integer      NOT NULL,
    notes        varchar(500),

    CONSTRAINT pk_inventory_items PRIMARY KEY (id),
    CONSTRAINT fk_inventory_items_character
        FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE,
    CONSTRAINT ck_inventory_items_name_length CHECK (char_length(name) BETWEEN 1 AND 100),
    CONSTRAINT ck_inventory_items_quantity CHECK (quantity >= 1)
);

CREATE INDEX ix_inventory_items_character_id ON inventory_items (character_id);
