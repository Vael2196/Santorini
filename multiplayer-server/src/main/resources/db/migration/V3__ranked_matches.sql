ALTER TABLE player_accounts ADD COLUMN rating INT NOT NULL DEFAULT 1200;
ALTER TABLE player_accounts ADD COLUMN ranked_wins INT NOT NULL DEFAULT 0;
ALTER TABLE player_accounts ADD COLUMN ranked_losses INT NOT NULL DEFAULT 0;
CREATE TABLE ranked_matches (
    match_id VARCHAR(36) PRIMARY KEY,
    player_a VARCHAR(36) NOT NULL,
    player_b VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL,
    winner_seat INT,
    rating_a_before INT NOT NULL,
    rating_b_before INT NOT NULL,
    rating_a_after INT,
    rating_b_after INT,
    started_at BIGINT NOT NULL,
    finished_at BIGINT,
    FOREIGN KEY (player_a) REFERENCES player_accounts(id),
    FOREIGN KEY (player_b) REFERENCES player_accounts(id)
);
CREATE INDEX ranked_match_pair ON ranked_matches(player_a, player_b, finished_at);
CREATE INDEX player_ranking ON player_accounts(rating, ranked_wins);
