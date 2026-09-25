# Soumission — Épreuve finale fullstack KFOKAM48

> Remplis ce fichier, **vérifie ton lien depuis une fenêtre de navigation privée**,
> puis téléverse-le sur la plateforme **avant 18h00**.
> Sans ce dépôt sur la plateforme, tu n'as rien rendu.

---

## Candidat

| | |
|---|---|
| Nom et prénom(s) | Kengne Ange Clément |
| Matricule | 195 |
| Centre | **Yaoundé** |
| Compte GitHub | clementange |

## Projet

| | |
|---|---|
| Dépôt (public) | https://github.com/clementange/kfokam48-epreuve-195 |
| Commit final — hash complet, 40 caractères | *relevé après le dernier `push` — voir ci-dessous* |
| Branche | `main` |

> **Sur le hash.** Le sujet est clair : « termine, pousse, puis copie son hash complet ». Un hash
> écrit **dans** le dépôt ne peut pas désigner le commit qui le contient — l'inscrire le change.
> La version téléversée sur la plateforme porte donc le hash du dernier commit poussé sur `main`,
> relevé après ce commit. Ce fichier-ci est le modèle dont elle est tirée.

## Technique

| | |
|---|---|
| Frontend utilisé | **Next.js** 16.3.6 (App Router, TypeScript) |
| Base de données | PostgreSQL 16, schéma versionné par Flyway |
| Commandes de démarrage | `docker compose up --build` puis <http://localhost:3000> |

## Ce que j'ai livré

**Ce qui fonctionne.** Les cinq opérations imposées du contrat et huit opérations ajoutées, toutes justifiées dans `api/contrat.yaml`. Les trois écrans, avec couche d'appels dédiée et états de chargement et d'erreur gérés. Le changement de besoin de l'étape 3 est livré : deux relecteurs par exercice, note moyenne, et note provisoire tant qu'une relecture manque. 184 tests, exécutables sur un poste vierge sans base locale. `docker compose up --build` vérifié depuis un clone vierge dans un dossier vide : l'application démarre peuplée.

**Ce que j'ai volontairement laissé de côté.** **EF12 — le blocage après cinq codes erronés (Q4).** Sacrifiée pour absorber le changement de besoin de l'étape 3, selon l'ordre de sacrifice inscrit au §10 du cahier des charges **avant** l'ouverture de l'enveloppe. La menace reste couverte : le code compte 32⁶ combinaisons, expire en 15 minutes, et RG24 ne confirme jamais l'existence d'un code appartenant à une autre promotion. Le raisonnement complet est dans l'issue #15, fermée en « not planned », et dans le journal.

**Ce qu'il faut savoir en corrigeant.** Le bug signalé par le client à l'étape 3 — deux étudiants simultanés, un seul enregistré — **n'a pas été reproduit**, et je l'écris plutôt que de le maquiller : testé sur H2 puis sur PostgreSQL, douze requêtes concurrentes passent. La contrainte d'unicité était en base dès la première migration. L'investigation a en revanche mis au jour un vrai défaut au même endroit — toute violation d'intégrité était traduite en « déjà présent », le message exact que le client avait vu — corrigé dans la PR #32, avec les tests en échec dans un commit antérieur à la correction.

**Hors périmètre par conception**, documenté au §3 : il n'y a aucune authentification. L'étudiant se désigne dans une liste, conformément à Q1. C'est une simplification du sujet, pas un oubli.

---

## Avant de téléverser, vérifie

- [ ] Mon dépôt est **public** et s'ouvre en navigation privée
- [ ] Le hash fait bien **40 caractères** et existe sur GitHub
- [ ] Tout mon travail est **poussé** — `git status` est propre
- [ ] Mon `README` a été testé depuis un clone vierge, dans un dossier vide
- [ ] Mon `JOURNAL.md` et mon cahier des charges sont dans `docs/`
- [ ] Les trois commits `[JALON]` sont poussés et dans le bon ordre

---

**Déclaration.** J'ai réalisé ce travail seul. Les outils d'IA étaient autorisés sans restriction et je les ai utilisés ; mon journal indique où et comment j'ai vérifié leurs réponses. Mon dépôt restera public et inchangé jusqu'à la publication des résultats.

Signature : ______________________  Date : __________
