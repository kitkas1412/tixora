-- Schema for data scraped from ticketbox.vn.
-- Prices are integer VND. tb_event.id is the ticketbox event id (not generated).
-- Children (tb_showing, tb_ticket_type) are replaced per event by the scraper.

CREATE TABLE IF NOT EXISTS tb_event (
    id          bigint PRIMARY KEY,
    name        text NOT NULL,
    url         text NOT NULL,
    image_url   text,
    venue       text,
    address     text,
    city        text,
    organizer   text,
    categories  text[] NOT NULL DEFAULT '{}',
    start_time  timestamptz,
    end_time    timestamptz,
    status      text,
    min_price   bigint,
    is_free     boolean,
    raw         jsonb NOT NULL,
    scraped_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tb_showing (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    bigint NOT NULL REFERENCES tb_event(id) ON DELETE CASCADE,
    source_id   bigint,
    start_time  timestamptz,
    end_time    timestamptz,
    status      text,
    raw         jsonb NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_ticket_type (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    showing_id      bigint NOT NULL REFERENCES tb_showing(id) ON DELETE CASCADE,
    source_id       bigint,
    name            text NOT NULL,
    price           bigint,
    original_price  bigint,
    sale_start      timestamptz,
    sale_end        timestamptz,
    status          text,
    is_free         boolean,
    raw             jsonb NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tb_event_start_time ON tb_event (start_time);
CREATE INDEX IF NOT EXISTS idx_tb_event_city ON tb_event (city);
CREATE INDEX IF NOT EXISTS idx_tb_showing_event_id ON tb_showing (event_id);
CREATE INDEX IF NOT EXISTS idx_tb_ticket_type_showing_id ON tb_ticket_type (showing_id);
