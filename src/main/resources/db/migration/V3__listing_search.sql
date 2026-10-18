-- Module: Listing + Search

CREATE TABLE categories
(
    id     SERIAL PRIMARY KEY,
    code   VARCHAR(30) NOT NULL UNIQUE,
    name   VARCHAR(60) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE listings
(
    id              UUID PRIMARY KEY,
    seller_id       UUID                   NOT NULL, -- logical -> users
    category_id     INT                    NOT NULL REFERENCES categories (id),
    title           VARCHAR(120)           NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 0)         NOT NULL
        CONSTRAINT listings_price_chk CHECK (price >= 0),
    condition       VARCHAR(15)            NOT NULL
        CONSTRAINT listings_condition_chk CHECK (condition IN ('NEW', 'LIKE_NEW', 'USED', 'WORN')),
    status          VARCHAR(10)            NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT listings_status_chk CHECK (status IN ('ACTIVE', 'HIDDEN', 'SOLD', 'DELETED', 'REMOVED')),
    exact_location  geography(Point, 4326) NOT NULL, -- never returned to clients
    public_location geography(Point, 4326) NOT NULL, -- blurred, used for search
    search_vector   TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(description, '')), 'B')
        ) STORED,
    seller_score    NUMERIC(5, 2),                   -- snapshot synced via ReputationUpdated
    created_at      TIMESTAMPTZ            NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ            NOT NULL DEFAULT now(),
    version         BIGINT                 NOT NULL DEFAULT 0
);
CREATE INDEX listings_public_location_gix ON listings USING gist (public_location);
CREATE INDEX listings_search_vector_gin ON listings USING gin (search_vector);
CREATE INDEX listings_status_category_created_idx ON listings (status, category_id, created_at);
CREATE INDEX listings_seller_id_idx ON listings (seller_id);

CREATE TABLE listing_images
(
    id         UUID PRIMARY KEY,
    listing_id UUID         NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    object_key VARCHAR(200) NOT NULL,
    position   SMALLINT     NOT NULL
        CONSTRAINT listing_images_position_chk CHECK (position BETWEEN 1 AND 8),
    CONSTRAINT listing_images_listing_position_uq UNIQUE (listing_id, position)
);

CREATE TABLE favorites
(
    user_id    UUID        NOT NULL, -- logical -> users
    listing_id UUID        NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, listing_id)
);
CREATE INDEX favorites_listing_id_idx ON favorites (listing_id);
