# Journal des modifications

Toutes les évolutions notables de **Présence & Relecture KFOKAM48**.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le versionnage
[SemVer](https://semver.org/lang/fr/). Chaque entrée renvoie à son issue et à sa pull request :
l'historique Git est la source, ce fichier n'en est que la lecture.

---

## [1.0.0] — 25/09/2026

Première version complète. Elle couvre les cinq opérations imposées par le contrat, les trois
écrans, et le changement de besoin reçu à l'étape 3.

### Ajouté

- **Ouverture d'une séance et code de présence** — code de 6 caractères tiré par `SecureRandom`
  dans un alphabet sans `I`, `O`, `0` ni `1`, valable 15 minutes (EF1, RG1, ENF5) · #1, PR #20
- **Marquage de présence** avec ses six issues de sortie, dans l'ordre du diagramme D3
  (EF2) · #4, PR #22
- **Dépôt et remplacement du lien d'un exercice**, possible après l'expiration du code et
  jusqu'à la clôture (EF4, EF10, RG12, RG13) · #5, #12, PR #23
- **Clôture d'une séance**, qui déclenche l'assignation des relecteurs (EF5, EF6, RG14) · #6,
  #7, PR #24
- **Relecture par les pairs** : note entière de 0 à 20 et commentaire (EF7, RG18) · #8, PR #25
- **Consultation par l'auteur** de la note reçue, sans jamais l'identité du relecteur
  (EF11, RG20) · #13, PR #25
- **Tableau récapitulatif du formateur**, calculé en une seule requête (EF8, Q16) · #9, #14,
  PR #26
- **Liste des promotions et des étudiants** — il n'y a pas d'authentification, cette liste en
  tient lieu (EF3, Q1) · #2, PR #21
- **Ajout manuel d'une présence par le formateur**, tracé par `source = FORMATEUR` et possible
  même quand le code a expiré (EF9, RG8, Q14) · #11, PR #38
- **Format d'erreur unique** `{ code, message }` sur toutes les réponses, avec filet de sécurité
  sur `Exception` (ENF4, B4) · #3, PR #19
- **Trois écrans** — formateur, étudiant, relecteur — avec couche d'appels dédiée et états de
  chargement et d'erreur gérés (F2, F3) · #27, PR #28
- **Interface présentable** : palette en variables CSS, chaque statut identifiable à sa couleur
  **et** à son libellé (ENF9) · #17, PR #28
- **Démarrage en une commande** `docker compose up --build`, avec données de démonstration
  chargées par migration (ENF6, ENF10) · #10, PR #29

### Modifié — changement de besoin de l'étape 3

- **Deux relecteurs par exercice** au lieu d'un. Le client revient sur Q6 : « quand il ne rend
  rien, l'étudiant n'a aucune note ». **RG15 est réécrite**, Q6 devient caduque · #34, PR #37
- **La note d'un exercice est la moyenne de ses relectures rendues**, et elle est **provisoire**
  tant qu'une relecture assignée manque (RG26). Nouveau statut `PARTIELLEMENT_RELU` · #35, PR #37
- **Contrat d'API en 2.0** : ajout de `moyenneProvisoire`, `provisoire`, `relecturesRendues`,
  `relecturesAttendues` et `exercicesUnSeulRelecteur`. `note` devient décimale, puisqu'elle est
  une moyenne. Les 5 opérations imposées restent **intactes**, vérifié par script
- **`RG16` étendue** : un exercice n'ayant trouvé qu'un seul pair éligible reçoit une unique
  relecture, et le formateur en est informé à la clôture

### Corrigé

- **Toute violation d'intégrité ne signifie pas « déjà présent »** (#31, PR #32). Une clé
  étrangère cassée répondait `409 DEJA_PRESENT` à un étudiant qui ne l'était pas — le message
  exact que le client avait signalé. Seule la violation de `uq_presence_session_etudiant` le dit
  désormais ; les autres remontent en `500`, avec la trace côté serveur.
  > Le symptôme décrit par le client — deux étudiants simultanés, un seul enregistré — **n'a pas
  > été reproduit** : vérifié par un test de concurrence sur H2 **puis sur PostgreSQL**. La
  > contrainte d'unicité était en base depuis la première migration.
- **Les notes décimales étaient tronquées silencieusement.** `15.5` devenait `15` : le relecteur
  croyait mettre 15,5, l'étudiant recevait 15. Q9 impose des entiers — la note est désormais
  reçue en `BigDecimal` et **refusée, pas arrondie** · PR #25
- **CORS absent** : le navigateur bloquait tous les appels du frontend, alors que `curl`
  répondait `200` et que les tests passaient. Devenu **ENF11**, avec sa méthode de
  vérification · PR #29
- **D3 ne se parsait pas** — un `;` dans une note termine une instruction Mermaid. GitHub aurait
  affiché une erreur à la place du diagramme · PR #16
- **Surefire n'exécutait pas les tests d'intégration** : le suffixe `IT` n'est pas dans ses
  `includes` par défaut. Ils compilaient sans jamais tourner · PR #20

### Migrations

| Version | Contenu |
|---|---|
| `V1` | Schéma initial — 7 tables, contraintes et index |
| `V2` | Référentiel de démonstration — promotions, formateurs, étudiants |
| `V3` | Activité de démonstration — séances, présences, exercices, relectures |
| `V4` | **Deux relecteurs par exercice** — table `relecture` recréée, données préservées |
| `V5` | Démonstration du changement — moyenne définitive, note provisoire |

> `V4` est écrite en Java : la contrainte de `V1` était déclarée en ligne, donc nommée par le
> moteur, et ce nom diffère entre H2 et PostgreSQL. Aucune migration existante n'a été modifiée.

### Sorti du périmètre

- **EF12 — blocage après cinq codes erronés** (#15). Sacrifié pour absorber le changement de
  besoin, conformément à l'ordre inscrit au §10 du cahier des charges **avant** l'ouverture de
  l'enveloppe. La menace de Q4 reste couverte par l'entropie du code (32⁶), sa durée de vie de
  15 minutes, et RG24 qui ne confirme jamais l'existence d'un code.

---

## [0.1.0] — 25/09/2026

Première version fonctionnelle : les dix stories `Must`. Jalon `[JALON] v0.1`.

## [0.0.1] — 25/09/2026

Analyse, spécification et conception, avant toute ligne de code. Cahier des charges en dix
sections, quatre diagrammes Mermaid, backlog en issues, contrat d'API figé.
Jalon `[JALON] analyse`.
