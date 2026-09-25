-- =============================================================================
-- V5 — Donnees de demonstration : le passage a deux relecteurs
--
-- La migration V3 avait ete ecrite quand un exercice n'avait qu'un relecteur.
-- Elle n'est PAS modifiee — une migration publiee ne se retouche jamais, son
-- empreinte est enregistree. On ajoute donc celle-ci, qui complete le jeu pour
-- que les trois situations nees du changement de besoin soient visibles a
-- l'ecran sans rien faire :
--
--   exercice 1  deux relectures RENDUES     -> note = moyenne (15 + 17) / 2 = 16
--                                              DEFINITIVE
--   exercice 2  une rendue, une EN_ATTENTE  -> note provisoire 12
--                                              PARTIELLEMENT_RELU
--   exercice 3  deux EN_ATTENTE             -> aucune note
--   exercice 4  une seule relecture, heritee de V3, jamais rendue
--   exercice 5  NON_ASSIGNE (RG16), inchange
--
-- Les relectures heritees de V3 gardent leur unique relecteur : la V4 ne leur
-- en a pas invente un second, et cette migration non plus. On ajoute des
-- relectures la ou le scenario de demonstration en a besoin, c'est tout.
-- =============================================================================

-- Exercice 1 (Awono) : Chendjou avait deja rendu 15. Fouda complete avec 17.
INSERT INTO relecture (exercice_id, relecteur_id, statut, note, commentaire, assignee_at, rendue_at)
VALUES (1, 6, 'RENDUE', 17,
        'Bonne lisibilité, les noms sont parlants. J''aurais découpé la méthode principale.',
        CURRENT_TIMESTAMP - INTERVAL '6' DAY, CURRENT_TIMESTAMP - INTERVAL '4' DAY);

UPDATE exercice SET statut = 'RELU' WHERE id = 1;

-- Exercice 2 (Bello) : Essomba avait rendu 12. Hamadou est assigne mais n'a pas
-- rendu -> la note 12 doit s'afficher comme PROVISOIRE (RG26).
INSERT INTO relecture (exercice_id, relecteur_id, statut, assignee_at)
VALUES (2, 8, 'EN_ATTENTE', CURRENT_TIMESTAMP - INTERVAL '6' DAY);

UPDATE exercice SET statut = 'PARTIELLEMENT_RELU' WHERE id = 2;

-- Exercice 3 (Chendjou) : un second relecteur, aucun des deux n'a rendu.
INSERT INTO relecture (exercice_id, relecteur_id, statut, assignee_at)
VALUES (3, 4, 'EN_ATTENTE', CURRENT_TIMESTAMP - INTERVAL '6' DAY);
