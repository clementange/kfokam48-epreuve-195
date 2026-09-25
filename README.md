# Présence & Relecture KFOKAM48

Application de suivi de présence et de relecture par les pairs pour la formation KFOKAM48.
Le formateur ouvre une séance et obtient un code ; l'étudiant marque sa présence avec ce code,
dépose le lien de son exercice, et relit l'exercice d'un pair désigné au hasard ; le formateur
lit dans un tableau unique qui était là, qui a rendu et quelle moyenne chacun obtient.

**Épreuve finale fullstack KFOKAM48** — candidat **195**.

---

## Démarrer l'application

**Une seule commande.** Il faut Docker, et rien d'autre — ni Java, ni Node, ni PostgreSQL.

```bash
git clone https://github.com/clementange/kfokam48-epreuve-195.git
cd kfokam48-epreuve-195
docker compose up --build
```

Puis ouvrir **<http://localhost:3000>**.

| Service | Adresse |
|---|---|
| Application | <http://localhost:3000> |
| API | <http://localhost:8080> |
| Base PostgreSQL | interne au réseau Docker, non exposée |

Le premier lancement construit les images et prend quelques minutes. Les suivants démarrent en
quelques secondes.

### Ce que vous trouverez en arrivant

**L'application est déjà peuplée** : aucune manipulation n'est nécessaire pour voir des données.
Le jeu de démonstration est chargé par une migration, au premier démarrage.

| Écran | Ce qu'il y a à voir |
|---|---|
| **Formateur** | Trois séances, dont une encore ouverte. Le tableau montre des moyennes, des moyennes absentes (« — »), et des relectures en attente |
| **Étudiant** | Choisissez *Awono Marie* : deux exercices, dont un relu avec sa note et son commentaire |
| **Relecteur** | Choisissez *Awono Marie* ou *Bello Idriss* : chacun a une relecture à rendre |

Le jeu est construit pour que **chaque statut soit visible sans rien faire**, y compris les deux
cas que le client n'avait pas prévus : un exercice `Aucun relecteur disponible` (RG16, séance 11)
et des relectures jamais rendues (RG21, séance 10).

Pour essayer le parcours complet : la séance 12 est ouverte mais **son code est expiré** — c'est
volontaire, cela montre que le dépôt reste possible après expiration (Q12) alors que le marquage
de présence ne l'est plus (RG5). Ouvrez une nouvelle séance depuis l'écran formateur pour obtenir
un code valide.

### Si un port est déjà pris

Le contrat d'API fixe l'adresse `http://localhost:8080`, qui est donc le défaut. Si ce port est
occupé sur votre machine :

```bash
PORT_API=8081 API_URL_NAVIGATEUR=http://localhost:8081 docker compose up --build
```

`API_URL_NAVIGATEUR` doit accompagner `PORT_API` : Next remplace les variables `NEXT_PUBLIC_*`
**à la compilation**, et c'est l'adresse vue depuis votre navigateur qui compte — pas le nom de
service Docker, inconnu en dehors du réseau interne. `PORT_WEB` et `ORIGINE_WEB` font de même
pour le frontend.

### Repartir de zéro

```bash
docker compose down -v    # -v supprime aussi le volume de la base
docker compose up --build
```

### Développer sans Docker

```bash
cd backend  && ./mvnw spring-boot:run   # nécessite un PostgreSQL local
cd frontend && npm install && npm run dev
```

Les tests, eux, ne demandent rien : ils tournent sur H2 en mémoire.

```bash
cd backend && ./mvnw test
```

## État du dépôt

| Étape | État |
|---|---|
| 1 — Analyse, spécification, conception | **terminée** — jalon `[JALON] analyse` posé |
| 2 — Première version (`Must`) | **terminée** — jalon `[JALON] v0.1` |
| 3 — Enveloppe | **terminée** — bug et changement de besoin, deux PR séparées |
| 4 — Version finale | **terminée** — jalon `[JALON] v1.0` |
| 5 — Soumission | en cours |

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
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | Les stories, l'ordre de traitement assumé, et ce qui est sorti du périmètre |
| [`CHANGELOG.md`](CHANGELOG.md) | Ce qui a été livré, corrigé et abandonné, avec le renvoi à chaque issue et pull request |
| [`api/contrat.yaml`](api/contrat.yaml) | Les 5 opérations imposées, reprises à l'identique, et les 8 opérations ajoutées avec leur justification |
| [`docs/JOURNAL.md`](docs/JOURNAL.md) | Le journal de bord, une entrée par étape |

## Organisation des branches

Le dépôt suit un git-flow allégé :

| Branche | Rôle |
|---|---|
| `main` | Ne reçoit que des **livraisons**. Porte les trois commits `[JALON]`, n'est jamais cassée. C'est la branche déclarée dans la soumission |
| `develop` | Branche d'**intégration**, et branche par défaut du dépôt. Toutes les branches fonctionnelles y sont fusionnées par pull request |
| `feat/<n°>-<intitulé>` | Une par issue, fusionnée dans `develop` |
| `fix/<n°>-<intitulé>` | Correctifs, séparés des évolutions |

`develop` est fusionnée dans `main` **aux jalons seulement** : `v0.1`, puis `v1.0`.

## Choix techniques

**Backend : Java 21 · Spring Boot 4.1 · Maven** (imposé par le sujet), PostgreSQL, schéma versionné
par **Flyway** en SQL portable — ce qui permet d'exécuter les tests d'intégration sur H2 sans
aucune base locale.

**Frontend : Next.js**, parce que son routage par fichiers donne directement les trois écrans
imposés — formateur, étudiant, relecteur — et que son découpage `app/` / `services/` impose
naturellement la couche d'appels API exigée par la contrainte F3.

## Ce que ce produit ne fait pas

Il n'y a **aucune authentification** : l'étudiant se désigne dans une liste, conformément à la
réponse Q1 du client. C'est une simplification assumée du sujet, pas un oubli — elle est
documentée au §3 du cahier des charges, avec les autres exclusions de périmètre.
