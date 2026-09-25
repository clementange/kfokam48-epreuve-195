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

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
