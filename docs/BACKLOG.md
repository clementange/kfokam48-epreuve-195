# Backlog — Présence & Relecture KFOKAM48

> Vue d'ensemble et **ordre de traitement assumé**. Le détail de chaque story — contexte,
> critères d'acceptation, règles de gestion concernées — vit dans l'issue GitHub correspondante,
> qui est la référence. Ce tableau existe pour que la priorisation soit lisible d'un coup d'œil
> et qu'elle reste dans l'historique Git quand elle change.

## Priorisation

| # | Story | Issue | Priorité | Exigence | Règles | Étape visée |
|---|---|---|---|---|---|---|
| 1 | Le formateur ouvre une session et obtient un code de présence | [#1](https://github.com/clementange/kfokam48-epreuve-195/issues/1) | **Must** | EF1 | RG1, RG2 | v0.1 |
| 2 | L'étudiant se désigne dans la liste de sa promotion | [#2](https://github.com/clementange/kfokam48-epreuve-195/issues/2) | **Must** | EF3 | RG25, RG24 | v0.1 |
| 3 | L'étudiant marque sa présence avec le code | [#4](https://github.com/clementange/kfokam48-epreuve-195/issues/4) | **Must** | EF2 | RG1, RG3–RG6, RG24 | v0.1 |
| 4 | Toute erreur de l'API répond `{ code, message }` | [#3](https://github.com/clementange/kfokam48-epreuve-195/issues/3) | **Must** | ENF4 | B4 | v0.1 |
| 5 | L'étudiant dépose le lien de son exercice | [#5](https://github.com/clementange/kfokam48-epreuve-195/issues/5) | **Must** | EF4 | RG9, RG10, RG11, RG12 | v0.1 |
| 6 | Le formateur clôture une session | [#6](https://github.com/clementange/kfokam48-epreuve-195/issues/6) | **Must** | EF5 | RG6, RG12, RG14 | v0.1 |
| 7 | Le système assigne un relecteur à chaque exercice déposé | [#7](https://github.com/clementange/kfokam48-epreuve-195/issues/7) | **Must** | EF6 | RG15, RG16, RG17 | v0.1 |
| 8 | Le relecteur rend sa note et son commentaire | [#8](https://github.com/clementange/kfokam48-epreuve-195/issues/8) | **Must** | EF7 | RG17, RG18, RG19 | v0.1 |
| 9 | Le formateur consulte le tableau récapitulatif de sa promotion | [#9](https://github.com/clementange/kfokam48-epreuve-195/issues/9) | **Must** | EF8 | RG21, RG22, RG23 | v0.1 |
| 10 | L'application démarre chez un tiers par une seule commande `docker compose up --build` | [#10](https://github.com/clementange/kfokam48-epreuve-195/issues/10) | **Must** | ENF6, ENF10 | — | v0.1 |
| 16 | L'interface est présentable : palette cohérente et statuts lisibles | [#17](https://github.com/clementange/kfokam48-epreuve-195/issues/17) | **Must** | ENF9 | RG8, RG16, RG21 | v1.0 |
| 11 | Le formateur ajoute une présence à la main | [#11](https://github.com/clementange/kfokam48-epreuve-195/issues/11) | Should | EF9 | RG8, RG6 | v1.0 |
| 12 | L'étudiant remplace le lien de son exercice | [#12](https://github.com/clementange/kfokam48-epreuve-195/issues/12) | Should | EF10 | RG13 | v1.0 |
| 13 | L'étudiant consulte la note et le commentaire reçus | [#13](https://github.com/clementange/kfokam48-epreuve-195/issues/13) | Should | EF11 | RG20 | v1.0 |
| 14 | Le formateur distingue les exercices en attente de relecture | [#14](https://github.com/clementange/kfokam48-epreuve-195/issues/14) | Should | EF13 | RG16, RG21 | v1.0 |
| ~~15~~ | ~~Le système bloque un étudiant après cinq codes erronés~~ | [#15](https://github.com/clementange/kfokam48-epreuve-195/issues/15) | ~~Could~~ **abandonnée** | EF12 | RG7 | **sortie du périmètre à l'étape 3** |
| 17 | Chaque exercice est relu par deux pairs différents | [#34](https://github.com/clementange/kfokam48-epreuve-195/issues/34) | **Must** | RG15 révisée | RG15, RG16, RG17 | v1.0 |
| 18 | La note est la moyenne des deux relectures, provisoire si une seule | [#35](https://github.com/clementange/kfokam48-epreuve-195/issues/35) | **Must** | RG26 | RG22, RG26 | v1.0 |

## Pourquoi cet ordre

**Les dix `Must` forment le plus petit produit qui se tient.** Ils suivent le parcours réel
d'une séance : ouvrir (1), s'identifier (2), marquer sa présence (3), déposer (5), clôturer (6),
être assigné (7), noter (8), consulter le tableau (9).

Deux stories `Must` ne viennent pas du client :

- **Story 4 — le format d'erreur.** Elle est placée avant tout le reste du parcours parce que le
  contrat l'impose sur *toutes* les opérations sans exception. La traiter après coup obligerait à
  reprendre chaque contrôleur ; la traiter d'abord fait que chaque story suivante hérite du
  comportement correct.
- **Story 10 — le démarrage chez un tiers.** Une application que le correcteur ne peut pas lancer
  ne vaut rien, et cela ne se rattrape pas le dernier quart d'heure.

**Story 6 avant story 7 :** l'assignation est un effet de la clôture (RG14, cahier des charges §7).
Sans clôture, aucune relecture n'existe et les stories 8 et 9 sont intestables. C'est la raison pour
laquelle la clôture — absente de la demande du client — est classée `Must` et non `Should`.

**Story 15 en `Could` :** le blocage de Q4 protège contre une fraude marginale sur une séance de
deux heures. C'est la première chose sacrifiée en cas de retard (cahier des charges §10).

## Ordre de sacrifice en cas de retard

EF12 → EF11 → EF13 → EF10 → EF9. Ne sont jamais sacrifiés : les migrations versionnées, les deux
tests de B6, et la mise à jour de l'analyse après l'étape 3.

**Appliqué à l'étape 3 :** le changement de besoin étant un `Must` tardif, **EF12 a été sacrifiée**,
conformément à cet ordre écrit avant l'ouverture de l'enveloppe. EF11 et EF13 étaient déjà livrées ;
le prochain sacrifice, s'il faut en faire un, serait EF9.

## Révisions

| Quand | Ce qui a changé et pourquoi |
|---|---|
| 25/09/2026 | Backlog initial, 15 stories, avant tout code. Ouvert en issues #1 à #15 sur le dépôt |
| 25/09/2026 | **Changement de besoin de l'étape 3.** Deux stories `Must` ajoutées (#34, #35) : deux relecteurs par exercice et moyenne des deux, ce qui casse RG15 issue de Q6. **Story 15 (EF12) sortie du périmètre** — elle était déjà `Could` et première de l'ordre de sacrifice inscrit au §10 avant même l'ouverture de l'enveloppe ; la menace de Q4 reste couverte par l'entropie du code et RG24. **Story 11 (EF9) maintenue mais repoussée** derrière le changement |
| 25/09/2026 | **Périmètre élargi par le commanditaire après l'étape 1.** Story 10 renforcée — le démarrage passe à une seule commande, frontend conteneurisé compris (ENF6, ENF10, issue #10 réécrite). Story 16 ajoutée en `Must` — interface présentable (ENF9, issue #17). Aucune story existante n'est déclassée : le périmètre s'élargit, il ne se déplace pas |
