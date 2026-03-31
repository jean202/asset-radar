CREATE TABLE IF NOT EXISTS asset_price_history (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    quote_currency VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    price NUMERIC(20, 8) NOT NULL,
    signed_change_rate NUMERIC(20, 8) NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_asset_price_history_lookup
    ON asset_price_history (source, quote_currency, symbol, collected_at DESC);

CREATE TABLE IF NOT EXISTS asset_analysis_history (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    quote_currency VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    current_price NUMERIC(20, 8) NOT NULL,
    previous_price NUMERIC(20, 8),
    price_change NUMERIC(20, 8) NOT NULL,
    change_rate NUMERIC(20, 8) NOT NULL,
    movement VARCHAR(16) NOT NULL,
    analyzed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_asset_analysis_history_lookup
    ON asset_analysis_history (source, quote_currency, symbol, analyzed_at DESC);

CREATE TABLE IF NOT EXISTS asset_alert_history (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    quote_currency VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    movement VARCHAR(16) NOT NULL,
    current_price NUMERIC(20, 8) NOT NULL,
    previous_price NUMERIC(20, 8),
    price_change NUMERIC(20, 8) NOT NULL,
    change_rate NUMERIC(20, 8) NOT NULL,
    threshold_rate NUMERIC(20, 8) NOT NULL,
    baseline_at TIMESTAMPTZ NOT NULL,
    window_seconds BIGINT NOT NULL,
    message TEXT NOT NULL,
    alerted_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE IF EXISTS asset_alert_history
    ADD COLUMN IF NOT EXISTS baseline_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS asset_alert_history
    ADD COLUMN IF NOT EXISTS window_seconds BIGINT;

CREATE INDEX IF NOT EXISTS idx_asset_alert_history_lookup
    ON asset_alert_history (source, quote_currency, symbol, alerted_at DESC);
