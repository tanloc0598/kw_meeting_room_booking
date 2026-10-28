CREATE TABLE rooms (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255),
    capacity    INT          NOT NULL CHECK (capacity > 0),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO rooms (code, name, location, capacity, active) VALUES
    ('R-101', 'Focus Room',     'Floor 1', 4,  TRUE),
    ('R-102', 'Huddle Room',    'Floor 1', 6,  TRUE),
    ('R-201', 'Meeting Room A', 'Floor 2', 10, TRUE),
    ('R-202', 'Meeting Room B', 'Floor 2', 10, TRUE),
    ('R-301', 'Board Room',     'Floor 3', 20, TRUE),
    ('R-302', 'Training Room',  'Floor 3', 30, FALSE);
