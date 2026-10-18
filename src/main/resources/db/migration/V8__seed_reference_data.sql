-- Reference data: system parameters, report reasons, categories

INSERT INTO app_parameters (key, value, description)
VALUES ('area.max_count', '2', 'Maximum number of verified areas per user'),
       ('area.accuracy_threshold_m', '100', 'Maximum GPS accuracy (meters) accepted for area verification'),
       ('area.verification_validity_days', '180', 'Days an area verification stays valid before renewal'),
       ('area.retention_days', '90', 'Days precise verification locations are kept before purge'),
       ('offer.ttl_hours', '48', 'Hours an offer stays open before expiring'),
       ('reputation.baseline_score', '50', 'Prior (baseline) reputation score'),
       ('reputation.prior_weight_c', '3', 'Weight C of the Bayesian prior'),
       ('reputation.half_life_days', '90', 'Half-life (days) of review weight for time decay'),
       ('reputation.gps_confirmed_weight', '1.2', 'Weight multiplier for GPS-confirmed transactions'),
       ('reputation.same_pair_weight', '0.5', 'Weight multiplier for repeated reviews between the same pair'),
       ('reputation.same_pair_window_days', '30', 'Window (days) for the same-pair weight reduction'),
       ('location.blur_meters', '300', 'Radius (meters) used to blur public listing locations');

INSERT INTO report_reasons (code, label)
VALUES ('SCAM', 'Scam or fraud'),
       ('PROHIBITED_ITEM', 'Prohibited item'),
       ('FAKE_LOCATION', 'Fake location'),
       ('HARASSMENT', 'Harassment'),
       ('OTHER', 'Other');

INSERT INTO categories (code, name)
VALUES ('ELECTRONICS', 'Electronics'),
       ('HOUSEHOLD', 'Household goods'),
       ('BOOKS', 'Books'),
       ('FASHION', 'Fashion'),
       ('VEHICLES', 'Vehicles'),
       ('OTHER', 'Other');
