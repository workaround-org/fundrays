-- Dev/test seed data

-- Admin user: username=admin, password=admin123 (bcrypt cost 10)
INSERT INTO AdminUser (id, username, passwordhash, roles, displayname, createdat)
VALUES (gen_random_uuid(), 'admin', '$2a$10$WntIbRd5nSwh8/0pAQuRlO1La/5ym7n1AmJbf27g79ZT6zpYXAlCi', 'admin', 'Administrator', now());

-- Sample active campaign for the public donation page (/donate/demokratie-retten)
INSERT INTO Campaign (id, slug, title, description, goalamount, currency, createdat, status)
VALUES ('11111111-1111-1111-1111-111111111111', 'demokratie-retten', 'Demokratie retten',
'## Gemeinsam für die Demokratie

Hilf uns, **Aufklärungsarbeit** zu leisten – jeder Beitrag zählt!

- Workshops an Schulen
- Öffentliche Veranstaltungen
- Material und Kampagnen', 500000, 'EUR', now(), 'ACTIVE');

INSERT INTO Donation (id, campaign_id, amount, currency, paymentmethod, status, donorname, message, createdat, confirmedat)
VALUES ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 5000, 'EUR', 'PAYPAL', 'CONFIRMED', 'Anna', 'Tolle Sache, weiter so!', now() - interval '2 hours', now() - interval '2 hours');
INSERT INTO Donation (id, campaign_id, amount, currency, paymentmethod, status, donorname, message, createdat, confirmedat)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 12000, 'EUR', 'STRIPE', 'CONFIRMED', 'Bernd', 'Für unsere Zukunft.', now() - interval '1 hours', now() - interval '1 hours');
INSERT INTO Donation (id, campaign_id, amount, currency, paymentmethod, status, donorname, message, createdat, confirmedat)
VALUES ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 3000, 'EUR', 'PAYPAL', 'PENDING', 'Clara', 'Noch nicht bestätigt', now(), null);
