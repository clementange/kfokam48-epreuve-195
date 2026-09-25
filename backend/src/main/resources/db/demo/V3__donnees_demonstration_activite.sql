-- =============================================================================
-- V3 — Donnees de demonstration : l'activite
--
-- Le sujet est explicite : "Prevois quelques donnees de demonstration chargees
-- au demarrage, sinon le correcteur ouvre une application vide et ne peut rien
-- verifier."
--
-- Ce jeu est construit pour qu'on puisse voir CHAQUE statut a l'ecran sans rien
-- faire. En particulier NON_ASSIGNE (RG16) et EN_ATTENTE_RELECTURE (RG21), les
-- deux cas que le client n'avait pas prevus et qui sont les plus faciles a
-- oublier de verifier :
--
--   Seance 1 (CLOTUREE)  6 presents dont 1 ajoute par le formateur (RG8)
--                        4 exercices deposes
--                        2 relus (notes 15 et 12), 2 en attente de relecture
--   Seance 2 (CLOTUREE)  1 seul present, 1 exercice -> NON_ASSIGNE (RG16)
--   Seance 3 (OUVERTE)   3 presents, 1 exercice deja depose
--                        code volontairement expire : le depot reste possible
--                        (Q12), le marquage de presence non (RG5)
--
-- Les horodatages sont relatifs a l'instant du demarrage, pour que la
-- demonstration reste coherente quel que soit le jour ou on la lance.
-- =============================================================================

-- --- Seance 1 : le cas complet ---------------------------------------------
INSERT INTO session (id, titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at) VALUES
    (1, 'Séance 10 — Les bases de Spring Boot', 1, 'D3M0A1',
     CURRENT_TIMESTAMP - INTERVAL '7' DAY,
     CURRENT_TIMESTAMP - INTERVAL '7' DAY + INTERVAL '15' MINUTE,
     'CLOTUREE', CURRENT_TIMESTAMP - INTERVAL '6' DAY);

INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES
    (1, 1, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (1, 2, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (1, 3, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (1, 4, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (1, 5, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    -- Q14, RG8 : Fouda avait un souci de telephone. La trace doit se voir.
    (1, 6, 'FORMATEUR', CURRENT_TIMESTAMP - INTERVAL '7' DAY + INTERVAL '20' MINUTE);

INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (1, 1, 1, 'https://github.com/awono/tp10',    'RELU',                 CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (2, 1, 2, 'https://github.com/bello/tp10',    'RELU',                 CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (3, 1, 3, 'https://github.com/chendjou/tp10', 'EN_ATTENTE_RELECTURE', CURRENT_TIMESTAMP - INTERVAL '7' DAY),
    (4, 1, 4, 'https://github.com/djeukam/tp10',  'EN_ATTENTE_RELECTURE', CURRENT_TIMESTAMP - INTERVAL '7' DAY);

INSERT INTO relecture (id, exercice_id, relecteur_id, statut, note, commentaire, assignee_at, rendue_at) VALUES
    (1, 1, 3, 'RENDUE', 15, 'Code clair et bien découpé. Il manque la gestion des cas d''erreur sur le formulaire.',
     CURRENT_TIMESTAMP - INTERVAL '6' DAY, CURRENT_TIMESTAMP - INTERVAL '5' DAY),
    (2, 2, 5, 'RENDUE', 12, 'Ça fonctionne, mais tout est dans une seule méthode. À découper.',
     CURRENT_TIMESTAMP - INTERVAL '6' DAY, CURRENT_TIMESTAMP - INTERVAL '4' DAY),
    -- Q11, RG21 : deux relecteurs n'ont jamais rendu. Le formateur doit le voir.
    (3, 3, 2, 'EN_ATTENTE', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '6' DAY, NULL),
    (4, 4, 1, 'EN_ATTENTE', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '6' DAY, NULL);

-- --- Seance 2 : RG16, aucun relecteur eligible ------------------------------
INSERT INTO session (id, titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at) VALUES
    (2, 'Séance 11 — Rattrapage (un seul présent)', 1, 'D3M0A2',
     CURRENT_TIMESTAMP - INTERVAL '3' DAY,
     CURRENT_TIMESTAMP - INTERVAL '3' DAY + INTERVAL '15' MINUTE,
     'CLOTUREE', CURRENT_TIMESTAMP - INTERVAL '2' DAY);

INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES
    (2, 7, 'ETUDIANT', CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- Ganava etait seule presente : personne ne pouvait la relire (RG16).
INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (5, 2, 7, 'https://github.com/ganava/tp11', 'NON_ASSIGNE', CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- --- Seance 3 : ouverte, code expire ----------------------------------------
-- Q12 : le depot reste possible apres l'expiration du code, jusqu'a la cloture.
-- RG5 : le marquage de presence, lui, ne l'est plus. C'est la distinction que
-- la demande du client ne faisait pas.
INSERT INTO session (id, titre, promotion_id, code, ouverture_at, expiration_at, statut) VALUES
    (3, 'Séance 12 — Spring Data JPA', 1, 'D3M0A3',
     CURRENT_TIMESTAMP - INTERVAL '2' HOUR,
     CURRENT_TIMESTAMP - INTERVAL '2' HOUR + INTERVAL '15' MINUTE,
     'OUVERTE');

INSERT INTO presence (session_id, etudiant_id, source, marquee_at) VALUES
    (3, 1, 'ETUDIANT', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
    (3, 2, 'ETUDIANT', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
    (3, 8, 'ETUDIANT', CURRENT_TIMESTAMP - INTERVAL '2' HOUR);

INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (6, 3, 1, 'https://github.com/awono/tp12', 'DEPOSE', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE);

-- Les identifiants ont ete poses a la main : on repositionne les sequences.
ALTER TABLE session   ALTER COLUMN id RESTART WITH 4;
ALTER TABLE exercice  ALTER COLUMN id RESTART WITH 7;
ALTER TABLE relecture ALTER COLUMN id RESTART WITH 5;
