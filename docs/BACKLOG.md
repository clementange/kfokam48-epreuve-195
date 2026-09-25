# Backlog — Présence & Relecture KFOKAM48

> Vue d'ensemble et **ordre de traitement assumé**. Le détail de chaque story — contexte,
> critères d'acceptation, règles de gestion concernées — vit dans l'issue GitHub correspondante,
> qui est la référence. Ce tableau existe pour que la priorisation soit lisible d'un coup d'œil
> et qu'elle reste dans l'historique Git quand elle change.

## Priorisation

| # | Story | Priorité | Exigence | Règles | Étape visée |
|---|---|---|---|---|---|
| 1 | Le formateur ouvre une session et obtient un code de présence | **Must** | EF1 | RG1, RG2 | v0.1 |
| 2 | L'étudiant se désigne dans la liste de sa promotion | **Must** | EF3 | RG25, RG24 | v0.1 |
| 3 | L'étudiant marque sa présence avec le code | **Must** | EF2 | RG1, RG3–RG6, RG24 | v0.1 |
| 4 | Toute erreur de l'API répond `{ code, message }` | **Must** | ENF4 | B4 | v0.1 |
| 5 | L'étudiant dépose le lien de son exercice | **Must** | EF4 | RG9, RG10, RG11, RG12 | v0.1 |
| 6 | Le formateur clôture une session | **Must** | EF5 | RG6, RG12, RG14 | v0.1 |
| 7 | Le système assigne un relecteur à chaque exercice déposé | **Must** | EF6 | RG15, RG16, RG17 | v0.1 |
| 8 | Le relecteur rend sa note et son commentaire | **Must** | EF7 | RG17, RG18, RG19 | v0.1 |
| 9 | Le formateur consulte le tableau récapitulatif de sa promotion | **Must** | EF8 | RG21, RG22, RG23 | v0.1 |
| 10 | L'application démarre chez un tiers depuis un clone vierge | **Must** | ENF6 | — | v0.1 |
| 11 | Le formateur ajoute une présence à la main | Should | EF9 | RG8, RG6 | v1.0 |
| 12 | L'étudiant remplace le lien de son exercice | Should | EF10 | RG13 | v1.0 |
| 13 | L'étudiant consulte la note et le commentaire reçus | Should | EF11 | RG20 | v1.0 |
| 14 | Le formateur distingue les exercices en attente de relecture | Should | EF13 | RG16, RG21 | v1.0 |
| 15 | Le système bloque un étudiant après cinq codes erronés | Could | EF12 | RG7 | v1.0 si le temps le permet |

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

## Révisions

| Quand | Ce qui a changé et pourquoi |
|---|---|
| 25/09/2026 | Backlog initial, 15 stories, avant tout code |
