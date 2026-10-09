CREATE TABLE player_accounts (
    id VARCHAR(36) PRIMARY KEY,
    name_key VARCHAR(96) NOT NULL UNIQUE,
    display_name VARCHAR(96) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE account_sessions (
    token_hash VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    expires_at BIGINT NOT NULL,
    FOREIGN KEY (account_id) REFERENCES player_accounts(id) ON DELETE CASCADE
);
CREATE INDEX account_sessions_account ON account_sessions(account_id);
CREATE INDEX account_sessions_expiry ON account_sessions(expires_at);
