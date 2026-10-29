# Schema

```sql
users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

```sql
rooms (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255),
    capacity    INT          NOT NULL CHECK (capacity > 0),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

```sql
bookings (
    id            BIGSERIAL    PRIMARY KEY,
    room_id       BIGINT       NOT NULL REFERENCES rooms (id),
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    title         VARCHAR(255) NOT NULL,
    start_at      TIMESTAMPTZ  NOT NULL,
    end_at        TIMESTAMPTZ  NOT NULL,
    attendees     INT          NOT NULL CHECK (attendees > 0),
    status        VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    cancel_reason VARCHAR(500),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT bookings_time_range CHECK (end_at > start_at),
    CONSTRAINT bookings_no_overlap
        EXCLUDE USING gist (
            room_id                           WITH =,
            tstzrange(start_at, end_at, '[)') WITH &&
        ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_bookings_user_start ON bookings (user_id, start_at);
```
