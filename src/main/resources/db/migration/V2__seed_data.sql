-- Equipment
INSERT INTO equipment (id, manufacturer, model_number, serial_number, asset_tag, location, status, description,
                       purchase_date)
VALUES ('a1b2c3d4-0001-0001-0001-000000000001', 'Dell', 'G15 5515', '', '', 'Dad''s Bedroom', 'ACTIVE', 'Gaming Laptop',
        '2020-12-28'),
       ('a1b2c3d4-0002-0002-0002-000000000002', 'Honeywell', 'Heater / AC', '', '', 'Heater Closet', 'ACTIVE', 'HVAC',
        '2005-12-28'),
       ('a1b2c3d4-0003-0003-0003-000000000003', 'Narwal', 'Freo-Z', '', '', 'Kitchen', 'ACTIVE', 'Robot Vac',
        '2024-06-28'),
       ('a1b2c3d4-0004-0004-0004-000000000004', 'Bambu Labs', 'X1C', '', '', 'Hobby Room', 'ACTIVE', '3D Printer',
        '2023-06-28'),
       ('a1b2c3d4-0005-0005-0005-000000000005', 'aa', 'bb', 'cc', 'dd', 'ee', 'IN_USE', 'ff', '2026-03-31');

-- Procedures for Dell G15 5515
INSERT INTO procedure (id, equipment_id, name, description, steps, required_tools, interval_days)
VALUES ('b1b2c3d4-0001-0001-0001-000000000001', 'a1b2c3d4-0001-0001-0001-000000000001',
        'Clean the Fans', 'Improves airflow and lowers temperatures.',
        E'1. Open the bottom.\n2. Use compressed air to blow out fans.\n3. Close the bottom.',
        NULL, 90);

-- Procedures for Honeywell Heater / AC
INSERT INTO procedure (id, equipment_id, name, description, steps, required_tools, interval_days)
VALUES ('b1b2c3d4-0002-0002-0002-000000000001', 'a1b2c3d4-0002-0002-0002-000000000002',
        'Change the Filter', 'Filtrete 20x20x4 MPR 1550',
        E'1. Open The furnace\n2. Remove the old filter\n3. Add a new filter\n4. Close the furnace\n5. ',
        '* tool 111', 90),
       ('b1b2c3d4-0002-0002-0002-000000000002', 'a1b2c3d4-0002-0002-0002-000000000002',
        'Clean the condenser coils', '',
        '1. Spay them down',
        NULL, 180);

-- Procedures for Narwal Freo-Z
INSERT INTO procedure (id, equipment_id, name, description, steps, required_tools, interval_days)
VALUES ('b1b2c3d4-0003-0003-0003-000000000001', 'a1b2c3d4-0003-0003-0003-000000000003',
        'Clean Filter', '',
        E'1. step 1\n2. step 2\n3. step 3\n4. step 4',
        E'here are the tools\n\n* tool 1\n* tool 2\n* ', 7),
       ('b1b2c3d4-0003-0003-0003-000000000002', 'a1b2c3d4-0003-0003-0003-000000000003',
        'Replace Filter', '',
        'blah...',
        NULL, 90);

-- Perform history for Dell G15 - Clean the Fans
INSERT INTO perform (id, procedure_id, date, notes)
VALUES ('c1b2c3d4-0001-0001-0001-000000000001', 'b1b2c3d4-0001-0001-0001-000000000001', '2025-12-28', ''),
       ('c1b2c3d4-0001-0001-0001-000000000002', 'b1b2c3d4-0001-0001-0001-000000000001', '2026-03-30', '');

-- Perform history for Honeywell - Clean the condenser coils
INSERT INTO perform (id, procedure_id, date, notes)
VALUES ('c1b2c3d4-0002-0002-0002-000000000001', 'b1b2c3d4-0002-0002-0002-000000000002', '2025-01-28', ''),
       ('c1b2c3d4-0002-0002-0002-000000000002', 'b1b2c3d4-0002-0002-0002-000000000002', '2025-12-28', '');

-- Perform history for Narwal - Clean Filter
INSERT INTO perform (id, procedure_id, date, notes)
VALUES ('c1b2c3d4-0003-0003-0003-000000000001', 'b1b2c3d4-0003-0003-0003-000000000001', '2026-01-05', ''),
       ('c1b2c3d4-0003-0003-0003-000000000002', 'b1b2c3d4-0003-0003-0003-000000000001', '2026-03-17', ''),
       ('c1b2c3d4-0003-0003-0003-000000000003', 'b1b2c3d4-0003-0003-0003-000000000001', '2026-03-17', ''),
       ('c1b2c3d4-0003-0003-0003-000000000004', 'b1b2c3d4-0003-0003-0003-000000000001', '2026-03-30', '');

-- Perform history for Narwal - Replace Filter
INSERT INTO perform (id, procedure_id, date, notes)
VALUES ('c1b2c3d4-0004-0004-0004-000000000001', 'b1b2c3d4-0003-0003-0003-000000000002', '2025-12-28', ''),
       ('c1b2c3d4-0004-0004-0004-000000000002', 'b1b2c3d4-0003-0003-0003-000000000002', '2026-03-30', '');
