CREATE TABLE equipment
(
    id            UUID PRIMARY KEY,
    manufacturer  VARCHAR(255) NOT NULL,
    model_number  VARCHAR(255) NOT NULL,
    serial_number VARCHAR(255),
    asset_tag     VARCHAR(255),
    location      VARCHAR(255),
    status        VARCHAR(50)  NOT NULL,
    description   TEXT,
    purchase_date DATE
);

CREATE TABLE procedure
(
    id             UUID PRIMARY KEY,
    equipment_id   UUID         NOT NULL REFERENCES equipment (id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    steps          TEXT,
    required_tools TEXT,
    interval_days  INT          NOT NULL
);

CREATE TABLE perform
(
    id           UUID PRIMARY KEY,
    procedure_id UUID NOT NULL REFERENCES procedure (id) ON DELETE CASCADE,
    date         DATE NOT NULL,
    notes        TEXT
);
