-- ── Plans d'abonnement ────────────────────────────────────────────────────
CREATE TABLE plans_abonnement (
    id                   BIGSERIAL PRIMARY KEY,
    nom                  VARCHAR(100) NOT NULL UNIQUE,
    description          TEXT,
    prix                 NUMERIC(10, 2) NOT NULL,
    duree_mois           INTEGER NOT NULL,
    nombre_consultations INTEGER,            -- NULL si illimite
    illimite             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL
);

-- ── Abonnements ───────────────────────────────────────────────────────────
CREATE TABLE abonnements (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id                 BIGINT NOT NULL REFERENCES plans_abonnement(id),
    date_debut              DATE NOT NULL,
    date_fin                DATE NOT NULL,
    statut                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    consultations_restantes INTEGER,         -- NULL si plan illimite
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL
);

CREATE INDEX idx_abonnements_user_id  ON abonnements(user_id);
CREATE INDEX idx_abonnements_statut   ON abonnements(statut);
CREATE INDEX idx_abonnements_date_fin ON abonnements(date_fin);

-- ── Paiements ─────────────────────────────────────────────────────────────
CREATE TABLE paiements (
    id                   BIGSERIAL PRIMARY KEY,
    abonnement_id        BIGINT NOT NULL REFERENCES abonnements(id) ON DELETE CASCADE,
    user_id              BIGINT NOT NULL REFERENCES users(id),
    montant              NUMERIC(10, 2) NOT NULL,
    date_paiement        TIMESTAMP NOT NULL,
    methode_paiement     VARCHAR(50) NOT NULL,
    reference_transaction VARCHAR(200),
    statut_paiement      VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL
);

CREATE INDEX idx_paiements_abonnement_id ON paiements(abonnement_id);
CREATE INDEX idx_paiements_user_id       ON paiements(user_id);

-- ── Consultations ─────────────────────────────────────────────────────────
CREATE TABLE consultations (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    norme_id          BIGINT NOT NULL REFERENCES normes(id) ON DELETE CASCADE,
    date_consultation TIMESTAMP NOT NULL
);

CREATE INDEX idx_consultations_user_id  ON consultations(user_id);
CREATE INDEX idx_consultations_norme_id ON consultations(norme_id);
