CREATE TABLE event (
    id SERIAL PRIMARY KEY,
    created numeric,
    event_type text,
    raw text
);
