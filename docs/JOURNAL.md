# Journal de bord — matricule 195

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges complet (10 sections, 13 exigences fonctionnelles, 25 règles de gestion, 8 exigences non fonctionnelles), les trois diagrammes imposés plus le quatrième en bonus, tous en Mermaid versionné, backlog de 15 stories priorisées et ouvertes en issues, contrat d'API complété et figé. Aucune ligne de code, aucun `spring init`.

**Bloqué :** environ 25 minutes sur le cycle de vie d'une session. Q3 parle de « fin de la session », Q10 et Q12 de « clôture par le formateur », et le contrat imposé ne contient aucune opération de clôture — c'est le trou que le sujet annonce. J'ai d'abord voulu assigner le relecteur au moment du dépôt, puis j'ai vu que cela rendait Q12 (dépôt tardif) et Q13 (remplacement du lien) inapplicables. Décision retenue : l'assignation a lieu **à la clôture**, seul instant où les présents et les exercices sont tous deux définitifs. Cette seule décision rend Q7, Q12 et Q13 compatibles et donne sa raison d'être à la clôture, qui devient une story `Must`.

Dix minutes de plus sur la contradiction Q10 / Q15. Tranchée en faveur de Q15 : le contrat impose `409 RELECTURE_DEJA_RENDUE`, et retenir Q10 rendrait ce code inatteignable, donc le contrat incohérent. Le contrat prime sur une réponse orale.

**IA :** je m'en suis servi pour dégrossir la rédaction du cahier des charges et des diagrammes, puis j'ai vérifié trois choses par moi-même, chacune par un contrôle et non à l'œil :

1. **Les diagrammes se parsent vraiment.** Je les ai passés dans `mermaid.parse` plutôt que de me fier au rendu supposé. D3 échouait sur un point-virgule dans une note — un point-virgule termine une instruction en Mermaid. Corrigé dans un commit dédié ; sans ce contrôle, GitHub aurait affiché une erreur à la place du diagramme le plus noté.
2. **Les cinq opérations imposées sont intactes.** Un script compare `api/contrat.yaml` au contrat d'origine : mêmes chemins, mêmes verbes, mêmes codes de statut, mêmes champs requis. Seuls des ajouts apparaissent — `429` et `409 SESSION_CLOTUREE` — et ils sont justifiés dans le fichier.
3. **Les références croisées existent.** Un script vérifie que chaque `RGx`, `EFx` et `ENFx` cité dans les diagrammes, le contrat, le backlog et le README est bien défini dans le cahier des charges, que la numérotation est contiguë, et qu'aucune règle n'est orpheline. 13 EF, 25 RG, 8 ENF, zéro référence morte.

J'ai aussi écarté une proposition de l'IA : découper le backlog en tickets techniques (« créer l'entité Session », « configurer Flyway »). Une issue doit décrire un résultat que le client comprend, pas une tâche d'implémentation. Les deux seules stories qui ne viennent pas du client — le format d'erreur et le démarrage chez un tiers — sont assumées comme telles dans `BACKLOG.md`, parce que le contrat et le barème les imposent.

---

## Étape 2 — Première version

**Fait :** les dix stories `Must`, chacune sur sa branche avec sa pull request et son issue fermée par le commit de fusion. Backend complet — les 5 opérations imposées du contrat plus 8 ajoutées —, 165 tests, les trois écrans Next.js, et `docker compose up --build` qui lève la pile entière avec ses données de démonstration. Onze pull requests fusionnées dans `develop`.

**Bloqué :** trois fois, et chaque fois par un défaut que je n'aurais pas vu sans vérifier.

Une heure environ, cumulée, sur des pièges d'outillage. **Surefire n'exécutait pas mes tests d'intégration** : le suffixe `IT` n'est pas dans ses `includes` par défaut, la classe compilait sans jamais tourner et je l'aurais crue verte. **Spring Boot 4 est passé à Jackson 3** (`tools.jackson.databind`) et a déplacé `AutoConfigureMockMvc`. Et un **résidu de build** — l'ancien `V3` resté dans `target/classes` après un déplacement de fichier — m'a fait chercher pendant vingt minutes un problème de configuration qui n'existait pas : `mvn test` ne nettoie pas les fichiers obsolètes.

Vingt minutes sur la **note décimale**. Reçue en `Integer`, `15.5` était acceptée par Jackson et **tronquée silencieusement en 15**. Q9 dit « en nombres entiers » : le relecteur aurait cru mettre 15,5, l'étudiant aurait reçu 15, et personne n'aurait été averti. J'ai d'abord essayé `spring.jackson.deserialization.accept-float-as-int` — sans effet en Boot 4 — puis je l'ai retirée plutôt que de laisser une configuration qui ne fait rien. La note est désormais reçue en `BigDecimal` et **refusée, pas arrondie** : arrondir déciderait à la place du relecteur.

Le plus grave, trouvé en lançant réellement Docker : **CORS n'était pas configuré**. Le frontend et l'API étant sur deux ports, le navigateur aurait bloqué tous les appels. Les 165 tests passaient, `curl` répondait `200` — `curl` n'applique aucune politique d'origine. L'application aurait été entièrement muette à l'écran. C'est devenu ENF11, avec sa méthode de vérification écrite noir sur blanc.

**IA :** je m'en suis servi pour écrire le gros du code et des tests, puis j'ai vérifié en exécutant, jamais en relisant. C'est ce qui a fait la différence à chaque fois : le test Surefire qui ne tournait pas, la troncature de note, et CORS ne se voient pas à la lecture du code — ils se voient quand on lance.

Deux propositions écartées : des tests qui dépendaient du tirage aléatoire du relecteur — un test qui dépend du hasard ne prouve rien le jour où il passe, j'ai injecté un tirage déterministe — et une désactivation des règles ESLint sur `setState` dans un effet, que j'ai corrigées à la racine avec une `key` et des drapeaux d'annulation.

**Ce que j'ai sorti du périmètre :** rien pour l'instant. Les deux stories `Should` et `Could` restantes — présence manuelle (#11) et blocage après cinq erreurs (#15) — sont reportées en v1.0, comme prévu au §10 du cahier des charges.

---

## Étape 3 — Enveloppe

**Fait :** le bug et le changement de besoin, sur **deux branches et deux pull requests séparées**, comme l'enveloppe l'exige. Côté bug : issue #31 ouverte avant tout code, commit de tests en échec, puis commit de correction — l'ordre est dans l'historique. Côté changement : issues #34 et #35, migration V4 ajoutée, contrat passé en 2.0, cahier des charges en version 3, D2 et D4 corrigés, frontend et seed de démonstration suivis.

**Bloqué :** deux fois, longuement.

**Le bug ne s'est pas reproduit.** J'ai écrit un test de concurrence — douze étudiants distincts, `CyclicBarrier` pour un départ simultané, sans `@Transactional` pour que chaque fil ouvre sa propre transaction — et les douze sont passés. J'ai refusé de m'arrêter là : H2 ne prouve rien sur la concurrence réelle. Rejoué en douze requêtes HTTP simultanées sur la pile Docker, donc sur PostgreSQL : douze `201` là aussi. La contrainte `uq_presence_session_etudiant` est en base depuis V1 et deux étudiants **différents** ne peuvent pas entrer en conflit dessus.

J'ai écrit dans l'issue que je n'avais pas reproduit le symptôme, plutôt que de fabriquer une correction qui aurait eu l'air de répondre. Mais l'analyse notée à l'ouverture de l'issue a payé : le `catch` attrapait **toute** `DataIntegrityViolationException` et la traduisait en `DEJA_PRESENT`. Une clé étrangère cassée répondait donc « Votre présence est déjà enregistrée » à un étudiant qui ne l'était pas — **exactement le message que le client a vu**, par un autre chemin. Deux tests l'ont démontré avant que je corrige quoi que ce soit.

**La migration V4 m'a coûté près d'une heure**, et c'est la leçon la plus utile de la journée. V1 déclarait l'unicité en ligne : `exercice_id BIGINT NOT NULL UNIQUE`. Une contrainte déclarée ainsi reçoit un **nom généré par le moteur**, et il diffère entre H2 et PostgreSQL. Trois tentatives :

1. `DatabaseMetaData.getIndexInfo` — donne le nom de l'*index*, pas celui de la *contrainte*. Sur H2 les deux diffèrent et le `DROP CONSTRAINT` échouait.
2. Le bon nom lu dans `information_schema` — le `DROP CONSTRAINT` passait, mais **H2 laissait l'index unique derrière lui**, et c'est lui qui refusait le second relecteur. Un `500` à la clôture, alors que la migration annonçait un succès.
3. **Recréer la table** — aucun nom généré, même résultat sur les deux moteurs. Il a fallu sauvegarder les données dans une table intermédiaire avant de supprimer l'ancienne, parce que H2 nomme les contraintes au niveau du **schéma** et non de la table : créer la nouvelle d'abord faisait entrer `ck_relecture_statut` en collision avec lui-même.

Le repositionnement de la séquence, impossible en SQL portable sans connaître la valeur maximale, s'est réglé tout seul : on était déjà en migration Java, il suffisait de lire `MAX(id)`.

**Ce que je retiens :** une contrainte déclarée en ligne est une contrainte qu'on ne pourra pas modifier proprement. Dans V1, j'aurais dû la nommer — comme je l'avais fait pour toutes les autres.

**IA :** elle a écrit la première version de la migration, et les trois tentatives successives viennent d'elle. Ce qui a fait la différence n'est pas ce qu'elle proposait mais le fait de **lancer la migration sur les deux moteurs à chaque fois** : les deux premières versions compilaient, s'exécutaient sans erreur apparente, et laissaient un schéma cassé. Seul le test l'a montré.

Même méthode pour le reste : j'ai vérifié sur la base PostgreSQL **déjà remplie** que les 4 relectures existantes survivaient à la V4 avec leurs notes, puis qu'une clôture assignait bien deux relecteurs, puis qu'une note passait de provisoire à définitive. L'enveloppe demandait que la base remplie survive : je l'ai constaté, pas supposé.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

**EF12 — le blocage après cinq codes erronés (issue #15) est abandonné.** Le raisonnement est écrit dans l'issue, qui est fermée en `not planned` pour que le backlog dise la vérité.

Trois raisons. Elle était déjà `Could` et **première de l'ordre de sacrifice inscrit au §10 du cahier des charges**, avant même que je sache ce que contiendrait l'enveloppe — je ne redécouvre pas la priorité après coup. La menace de Q4 est déjà largement couverte : 32⁶ combinaisons et 15 minutes de validité mettent la force brute hors de portée. Et elle n'est référencée par aucune autre règle : la retirer ne casse rien, contrairement à la clôture dont dépendait toute l'assignation.

Ce qu'on perd : rien de fonctionnel. Le risque réel est un abus de ressources serveur, pas une fraude réussie. Et RG24 — un code d'une autre promotion répond `CODE_INCONNU` sans confirmer son existence — reste en place : c'était la part vraiment utile de Q4.

**EF9 — la présence manuelle (issue #11) reste au périmètre mais passe derrière.** Q14 décrit un besoin réel et le formateur n'a aucun recours quand le code ne marche pas. La colonne `source`, le `CHECK`, le DTO et l'étiquette « Ajouté par le formateur » existent déjà : seul l'endpoint manque. Si elle tombe, ce sera faute de temps et ce sera écrit.

---

## Étape 4 — Version finale

**Fait :** dernière story du backlog livrée — EF9, la présence ajoutée à la main (#11) —, `CHANGELOG.md` écrit à partir de l'historique réel, README vérifié **depuis un clone vierge dans un dossier vide**, backlog trié, jalon `[JALON] v1.0`.

EF9 a survécu au sacrifice de l'étape 3 pour une raison concrète : tout existait déjà depuis la migration V1 — la colonne `source`, le `CHECK`, le DTO, l'étiquette « Ajouté par le formateur » à l'écran. Seule l'opération manquait. Le coût était faible et le trou fonctionnel réel : sans elle, un formateur n'a aucun recours quand le code ne marche pas pour quelqu'un.

**Bloqué :** rien de sérieux à cette étape, et c'est le résultat des précédentes plus qu'une réussite en soi. Le seul point à surveiller était le `docker compose up --build` depuis un clone : je l'ai fait dans un dossier vide, sur un clone frais du dépôt distant, pour ne pas me fier à un cache local ou à un fichier non versionné que j'aurais oublié d'ajouter.

Un détail a demandé réflexion : le port 8080 est occupé sur ma machine par le Keycloak d'un autre projet. Plutôt que de l'arrêter — ce n'est pas mon service — j'ai rendu les ports paramétrables dès l'étape 2. Cela s'est avéré utile deux fois : pour mes propres tests, et parce que le correcteur peut très bien être dans le même cas.

**IA :** peu sollicitée ici, le travail était surtout de la vérification. Elle a rédigé une première version du `CHANGELOG` à partir des messages de commit ; je l'ai reprise sur un point de fond. Elle présentait le bug de l'étape 3 comme corrigé, sans mentionner que **le symptôme signalé par le client n'avait pas été reproduit**. C'est précisément ce qu'il fallait écrire : un correctif qui a l'air de répondre à un bug inexistant vaut moins qu'un constat honnête accompagné du vrai défaut trouvé à côté.

**Ce que je referais autrement :** nommer **toutes** les contraintes dans la migration initiale. Celle de `relecture` était déclarée en ligne — `exercice_id BIGINT NOT NULL UNIQUE` — donc nommée par le moteur, avec un nom différent sur H2 et sur PostgreSQL. C'est ce qui a transformé une modification de cinq lignes en trois tentatives et près d'une heure à l'étape 3.

---

## Étape 5 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
