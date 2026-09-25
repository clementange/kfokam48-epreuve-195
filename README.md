# Présence & Relecture KFOKAM48

Application de suivi de présence et de relecture par les pairs pour la formation KFOKAM48.
Le formateur ouvre une séance et obtient un code ; l'étudiant marque sa présence avec ce code,
dépose le lien de son exercice, et relit l'exercice d'un pair désigné au hasard ; le formateur
lit dans un tableau unique qui était là, qui a rendu et quelle moyenne chacun obtient.

**Épreuve finale fullstack KFOKAM48** — candidat **195**.

---

## État du dépôt

| Étape | État |
|---|---|
| 1 — Analyse, spécification, conception | **en cours** |
| 2 — Première version (`Must`) | à venir |
| 3 — Enveloppe | à venir |
| 4 — Version finale | à venir |

> L'installation et le démarrage seront documentés ici à l'étape 4, **testés depuis un clone
> vierge dans un dossier vide**. Il n'y a volontairement aucune ligne de code dans ce dépôt
> avant le commit `[JALON] analyse`.

## Structure

```
docs/     CAHIER_DES_CHARGES.md · BACKLOG.md · JOURNAL.md · diagrammes/
api/      contrat.yaml — le contrat d'API, figé avant le premier commit de code
backend/  Spring Boot (Java 21, Maven, wrapper mvnw)
frontend/ Next.js
```

## Par où commencer la lecture

| Document | Ce qu'on y trouve |
|---|---|
| [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md) | Le besoin spécifié : 13 exigences fonctionnelles, 25 règles de gestion, et **la section 7** — les contradictions du client tranchées et les trous de sa demande comblés |
| [`docs/diagrammes/D1-cas-utilisation.md`](docs/diagrammes/D1-cas-utilisation.md) | Qui fait quoi |
| [`docs/diagrammes/D2-modele-donnees.md`](docs/diagrammes/D2-modele-donnees.md) | Le modèle de données — **fait foi pour les migrations** |
| [`docs/diagrammes/D3-sequence-marquer-presence.md`](docs/diagrammes/D3-sequence-marquer-presence.md) | « Marquer sa présence », cas nominal et six cas d'erreur — **fait foi pour les codes HTTP** |
| [`docs/diagrammes/D4-etats-exercice.md`](docs/diagrammes/D4-etats-exercice.md) | Le cycle de vie d'un exercice |
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | Les 15 stories et l'ordre de traitement assumé |
| [`api/contrat.yaml`](api/contrat.yaml) | Les 5 opérations imposées, reprises à l'identique, et les 8 opérations ajoutées avec leur justification |
| [`docs/JOURNAL.md`](docs/JOURNAL.md) | Le journal de bord, une entrée par étape |

## Choix techniques

**Backend : Java 21 · Spring Boot 3 · Maven** (imposé par le sujet), PostgreSQL, schéma versionné
par **Flyway** en SQL portable — ce qui permet d'exécuter les tests d'intégration sur H2 sans
aucune base locale.

**Frontend : Next.js**, parce que son routage par fichiers donne directement les trois écrans
imposés — formateur, étudiant, relecteur — et que son découpage `app/` / `services/` impose
naturellement la couche d'appels API exigée par la contrainte F3.

## Ce que ce produit ne fait pas

Il n'y a **aucune authentification** : l'étudiant se désigne dans une liste, conformément à la
réponse Q1 du client. C'est une simplification assumée du sujet, pas un oubli — elle est
documentée au §3 du cahier des charges, avec les autres exclusions de périmètre.
