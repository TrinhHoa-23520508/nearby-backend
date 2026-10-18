-- Module: Transaction

CREATE TABLE transactions
(
    id                  UUID PRIMARY KEY,
    listing_id          UUID           NOT NULL, -- logical -> listings
    buyer_id            UUID           NOT NULL, -- logical -> users
    seller_id           UUID           NOT NULL, -- logical -> users
    status              VARCHAR(20)    NOT NULL DEFAULT 'OFFERED'
        CONSTRAINT transactions_status_chk CHECK (status IN
                                                  ('OFFERED', 'ACCEPTED', 'COMPLETED',
                                                   'REJECTED', 'EXPIRED', 'CANCELLED')),
    offered_price       NUMERIC(12, 0) NOT NULL
        CONSTRAINT transactions_offered_price_chk CHECK (offered_price > 0),
    message             VARCHAR(300),
    expires_at          TIMESTAMPTZ,
    buyer_confirmed_at  TIMESTAMPTZ,
    seller_confirmed_at TIMESTAMPTZ,
    cancelled_by        UUID,
    cancel_reason       VARCHAR(200),
    exchange_note       VARCHAR(200),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT transactions_buyer_not_seller_chk CHECK (buyer_id <> seller_id)
);
CREATE INDEX transactions_listing_id_idx ON transactions (listing_id);
CREATE INDEX transactions_buyer_id_idx ON transactions (buyer_id);
CREATE INDEX transactions_seller_id_idx ON transactions (seller_id);
CREATE INDEX transactions_status_expires_at_idx ON transactions (status, expires_at);
-- At most one open offer per (listing, buyer)
CREATE UNIQUE INDEX transactions_open_offer_uq ON transactions (listing_id, buyer_id)
    WHERE status IN ('OFFERED', 'ACCEPTED');
