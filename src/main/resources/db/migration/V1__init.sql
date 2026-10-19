CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    owner VARCHAR(60) NOT NULL,
    balance NUMERIC(19,4) NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_owner ON accounts (owner);

CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    requested_by VARCHAR(60) NOT NULL,
    idempotency_key VARCHAR(60) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    from_account BIGINT NOT NULL REFERENCES accounts(id),
    to_account BIGINT NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19,4) NOT NULL CHECK(amount> 0),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_transfer_idempotency UNIQUE (requested_by, idempotency_key),
    CONSTRAINT chk_different_accounts CHECK (from_account <> to_account)
);

CREATE TABLE ledger_entries(
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT REFERENCES transfers(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19,4) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    created_at NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_accounts ON ledger_entries (account_id, id DESC);

CREATE TABLE audit_log(
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(60) NOT NULL,
    action VARCHAR(40) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    created_at TIMESTAMPZ NOT NULL DEFAULT now()
);

CREATE FUNCTION forbid_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_append_only BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION forbid_change();
CREATE TRIGGER audit_append_only BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION forbid_change()

