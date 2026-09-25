-- =============================================================================
-- V2 — Donnees de demonstration : le referentiel
--
-- Le seed est une migration, et non un script a lancer a la main (ENF6) : les
-- donnees arrivent avec le schema, au premier demarrage, sans aucune action du
-- correcteur. Elles sont donc versionnees et reproductibles comme le schema.
--
-- La gestion des promotions, formateurs et etudiants est hors perimetre
-- (cahier des charges §3) : ces lignes sont la seule facon d'en obtenir.
--
-- Les identifiants sont fixes explicitement pour que les migrations suivantes
-- (V3, activite de demonstration) puissent y faire reference sans deviner des
-- valeurs generees.
-- =============================================================================

INSERT INTO formateur (id, nom) VALUES
    (1, 'Mme Ndongo Clarisse'),
    (2, 'M. Fotso Bertrand');

INSERT INTO promotion (id, nom, formateur_id) VALUES
    (1, 'KFOKAM48 — Promotion Yaoundé 2026', 1),
    (2, 'KFOKAM48 — Promotion Douala 2026', 2);

-- Douze etudiants sur la promotion 1 : assez pour que le tirage aleatoire du
-- relecteur (RG15) ait un sens et que le tableau du formateur soit lisible.
INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1,  'Awono Marie',        1),
    (2,  'Bello Idriss',       1),
    (3,  'Chendjou Laure',     1),
    (4,  'Djeukam Patrick',    1),
    (5,  'Essomba Nadège',     1),
    (6,  'Fouda Armand',       1),
    (7,  'Ganava Solange',     1),
    (8,  'Hamadou Ousmane',    1),
    (9,  'Ibrahim Aïcha',      1),
    (10, 'Jiofack Cédric',     1),
    (11, 'Kamga Berthe',       1),
    (12, 'Lontsi Rodrigue',   1),
    -- La promotion 2 sert a verifier RG24 : un etudiant ne marque sa presence
    -- que sur une seance de sa propre promotion.
    (13, 'Mbarga Sylvie',      2),
    (14, 'Ndam Aboubakar',     2),
    (15, 'Owona Christelle',   2);

-- Les identifiants ont ete poses a la main : on repositionne les sequences,
-- sinon la premiere insertion applicative reessaierait a partir de 1 et
-- violerait la cle primaire.
ALTER TABLE formateur ALTER COLUMN id RESTART WITH 3;
ALTER TABLE promotion ALTER COLUMN id RESTART WITH 3;
ALTER TABLE etudiant  ALTER COLUMN id RESTART WITH 16;
