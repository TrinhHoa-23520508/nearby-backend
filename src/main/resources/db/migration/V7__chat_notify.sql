-- Module: Chat + Notify

CREATE TABLE conversations
(
    id              UUID PRIMARY KEY,
    listing_id      UUID        NOT NULL, -- logical -> listings
    buyer_id        UUID        NOT NULL, -- logical -> users
    seller_id       UUID        NOT NULL, -- logical -> users
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT conversations_listing_buyer_uq UNIQUE (listing_id, buyer_id)
);
CREATE INDEX conversations_buyer_last_msg_idx ON conversations (buyer_id, last_message_at DESC);
CREATE INDEX conversations_seller_last_msg_idx ON conversations (seller_id, last_message_at DESC);

CREATE TABLE messages
(
    id              BIGSERIAL PRIMARY KEY, -- monotonically increasing sync checkpoint
    conversation_id UUID          NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       UUID          NOT NULL, -- logical -> users
    body            VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX messages_conversation_id_idx ON messages (conversation_id, id);

CREATE TABLE notifications
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL, -- logical -> users
    type       VARCHAR(30) NOT NULL,
    payload    JSONB,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX notifications_user_created_idx ON notifications (user_id, created_at DESC);

CREATE TABLE device_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL, -- logical -> users
    fcm_token  VARCHAR(300) NOT NULL UNIQUE,
    platform   VARCHAR(10)  NOT NULL DEFAULT 'ANDROID'
        CONSTRAINT device_tokens_platform_chk CHECK (platform IN ('ANDROID')),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX device_tokens_user_id_idx ON device_tokens (user_id);
