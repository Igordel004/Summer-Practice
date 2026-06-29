INSERT INTO users (phone, nickname)
VALUES ('+78005553535', 'Money')
ON CONFLICT (phone) DO NOTHING;
