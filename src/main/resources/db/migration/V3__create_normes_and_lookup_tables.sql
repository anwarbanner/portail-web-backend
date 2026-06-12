CREATE TABLE IF NOT EXISTS statuts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS document_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS collections (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS industrial_branches (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS product_families (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS sub_families (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS filters1 (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS ics_level1 (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS ics_level2 (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    ics_level1_id BIGINT NOT NULL REFERENCES ics_level1(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ics_level3 (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    ics_level2_id BIGINT NOT NULL REFERENCES ics_level2(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS normes (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(120) NOT NULL UNIQUE,
    publication_date DATE,
    titre_fr VARCHAR(255),
    titre_en VARCHAR(255),
    titre_de VARCHAR(255),
    descripteur_fr TEXT,
    descripteur_en TEXT,
    document_identifier VARCHAR(120),
    included_in_subscription BOOLEAN NOT NULL DEFAULT FALSE,
    afnor_index VARCHAR(80),
    print_number VARCHAR(80),
    print_date DATE,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    regulation_specifique TEXT,
    statut_id BIGINT REFERENCES statuts(id),
    document_type_id BIGINT REFERENCES document_types(id),
    collection_id BIGINT REFERENCES collections(id),
    industrial_branch_id BIGINT REFERENCES industrial_branches(id),
    product_family_id BIGINT REFERENCES product_families(id),
    sub_family_id BIGINT REFERENCES sub_families(id),
    filter1_id BIGINT REFERENCES filters1(id),
    ics_level1_id BIGINT REFERENCES ics_level1(id),
    ics_level2_id BIGINT REFERENCES ics_level2(id),
    ics_level3_id BIGINT REFERENCES ics_level3(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_normes_reference ON normes(reference);
CREATE INDEX IF NOT EXISTS idx_normes_statut ON normes(statut_id);
CREATE INDEX IF NOT EXISTS idx_normes_ics1 ON normes(ics_level1_id);
CREATE INDEX IF NOT EXISTS idx_normes_ics2 ON normes(ics_level2_id);
CREATE INDEX IF NOT EXISTS idx_normes_ics3 ON normes(ics_level3_id);

