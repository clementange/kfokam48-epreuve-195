# Cahier des charges — Présence & Relecture KFOKAM48

**Auteur :** Ange Clément · matricule **195**
**Version :** 1 · **Date :** 25 septembre 2026
**Frontend choisi :** **Next.js**, parce que son routage par fichiers donne gratuitement les trois écrans imposés (F2) et que son découpage `app/` / `services/` impose naturellement la couche d'appels API exigée par F3.

---

## 1. Contexte et objectif

La formation KFOKAM48 fait l'appel à la main et corrige les exercices de ses étudiants en dehors de toute trace écrite. Le formateur perd du temps en début de séance, ne sait pas à la fin du mois qui était présent à quelle séance, et n'a aucune vision de ce que valent les exercices rendus.

L'application répond à trois besoins concrets. Elle **remplace l'appel oral par un code de présence** que le formateur ouvre en début de séance et que chaque étudiant saisit depuis son téléphone. Elle **organise la relecture des exercices entre pairs** : chaque étudiant dépose le lien de son travail, et le système désigne un autre étudiant présent à la même séance pour le noter. Elle **donne enfin au formateur un tableau unique** où il lit, promotion par promotion, qui était là, qui a rendu, quelle moyenne chacun obtient et quelles relectures restent en souffrance.

L'objectif n'est pas de noter à la place du formateur, mais de faire porter la relecture par le groupe tout en gardant la trace de ce qui s'est passé pendant chaque séance.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session et obtenir son code · ajouter une présence à la main · clôturer une session · consulter le tableau récapitulatif de sa promotion | Marquer une présence à la place d'un étudiant sans que ce soit tracé · noter un exercice · rouvrir une session clôturée |
| **Étudiant** | Se désigner dans la liste de sa promotion · marquer sa présence avec le code · déposer le lien de son exercice et le remplacer · consulter la note et le commentaire reçus | Marquer sa présence après expiration du code ou après clôture · déposer deux exercices pour la même session · voir l'identité de son relecteur |
| **Relecteur** | Consulter l'exercice qui lui est assigné · rendre une note entière de 0 à 20 et un commentaire | Relire son propre exercice · modifier sa note une fois validée · choisir l'exercice qu'il relit |

**Le relecteur n'est pas un acteur distinct.** C'est un étudiant placé dans un état temporaire par le système : il est porteur d'une relecture assignée. La conséquence sur le modèle de données est directe — **aucune table `Relecteur`** ; le rôle est porté par la colonne `relecteur_id` de la table `relecture`, et un même étudiant est simultanément auteur d'un exercice et relecteur d'un autre.

## 3. Périmètre

**Inclus dans cette version :**

- Ouverture d'une session de cours par le formateur, avec génération d'un code de présence à durée de vie limitée
- Clôture explicite d'une session par le formateur
- Marquage de présence par l'étudiant au moyen du code, et ajout manuel d'une présence par le formateur, les deux distingués par leur source
- Limitation des tentatives après plusieurs codes erronés
- Dépôt du lien d'un exercice par l'étudiant, et remplacement de ce lien tant que la session est ouverte
- Assignation automatique et aléatoire d'un relecteur unique parmi les étudiants présents à la session
- Rendu d'une relecture : note entière de 0 à 20 et commentaire, définitive une fois validée
- Consultation par l'auteur de la note et du commentaire reçus, sans l'identité du relecteur
- Tableau récapitulatif du formateur par promotion
- Jeu de données de démonstration chargé au démarrage

**Explicitement exclu :**

- **Toute authentification** — pas de mot de passe, pas de session utilisateur, pas de contrôle d'accès réel. L'étudiant se désigne dans une liste (Q1). Une application réelle ne pourrait pas se le permettre ; c'est une simplification assumée du sujet.
- **La gestion des comptes** — création, modification, suppression de promotions, d'étudiants et de formateurs. Ces données sont injectées par le jeu de démonstration.
- **Plusieurs relecteurs par exercice** (Q6 fixe le nombre à un) et la relecture croisée ou en cascade.
- **L'historique des modifications d'une note** — sans objet, la note est définitive (RG18).
- **Toute notification** par courriel, SMS ou notification poussée : rien ne prévient un relecteur qu'il a été assigné, il le découvre sur son écran.
- **L'envoi de fichiers** : on stocke un lien, jamais un document.
- **L'export** du tableau en PDF ou tableur, et toute statistique au-delà de ce que Q16 demande.
- **Le soin apporté à l'interface** : le sujet exclut explicitement le rendu visuel de la notation, aucun effort n'est investi dans le CSS au-delà du lisible.
- **La réouverture d'une session clôturée** : la clôture est un aller simple.

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| **EF1** | Le formateur ouvre une session de cours et obtient un code de présence | Quand je poste un titre et une promotion valides, alors je reçois `201` avec un code, une date d'ouverture et une date d'expiration fixée 15 minutes plus tard | Must |
| **EF2** | L'étudiant marque sa présence à l'aide du code | Quand je saisis un code valide, non expiré, d'une session ouverte de ma promotion, alors je reçois `201` et ma présence apparaît dans le tableau du formateur avec la source `ETUDIANT` | Must |
| **EF3** | L'étudiant se désigne dans la liste des étudiants de sa promotion | Quand j'ouvre l'écran étudiant, alors la liste des étudiants de la promotion s'affiche et mon choix conditionne les actions suivantes | Must |
| **EF4** | L'étudiant dépose le lien de son exercice pour une session | Quand je poste un lien `http(s)` valide pour une session ouverte où je suis présent, alors je reçois `201` avec le statut `DEPOSE` | Must |
| **EF5** | Le formateur clôture une session | Quand je clôture une session ouverte, alors son statut passe à `CLOTUREE`, les relecteurs sont assignés, et tout nouveau dépôt ou marquage de présence est refusé | Must |
| **EF6** | Le système assigne à chaque exercice un relecteur, tiré au hasard parmi les étudiants présents à la session, l'auteur exclu | Quand la session est clôturée, alors chaque exercice déposé porte exactement une relecture en attente dont le relecteur est présent à cette session et différent de l'auteur | Must |
| **EF7** | Le relecteur rend sa note et son commentaire | Quand je poste une note entière entre 0 et 20 et un commentaire sur une relecture qui m'est assignée et non encore rendue, alors je reçois `200` et la relecture passe au statut `RENDUE` | Must |
| **EF8** | Le formateur consulte le tableau récapitulatif de sa promotion | Quand j'appelle le tableau d'une promotion existante, alors j'obtiens pour chaque étudiant son nombre de présences, son nombre d'exercices déposés, sa moyenne et son nombre de relectures restant à faire | Must |
| **EF9** | Le formateur ajoute une présence à la main | Quand j'ajoute manuellement la présence d'un étudiant à une session ouverte, alors elle est enregistrée avec la source `FORMATEUR` et cette origine est visible dans l'interface | Should |
| **EF10** | L'étudiant remplace le lien de son exercice | Quand je remplace le lien d'un exercice déposé sur une session encore ouverte, alors le nouveau lien est enregistré ; si la session est clôturée, je reçois `409` | Should |
| **EF11** | L'étudiant relu consulte la note et le commentaire reçus | Quand je consulte mon exercice relu, alors je vois la note et le commentaire, et à aucun endroit le nom du relecteur | Should |
| **EF12** | Le système limite les tentatives après plusieurs codes erronés | Quand j'échoue cinq fois de suite sur un code, alors mes tentatives suivantes sont refusées avec `429` pendant deux minutes | Could |
| **EF13** | Le formateur distingue dans son tableau les exercices encore en attente de relecture | Quand un exercice n'a pas été relu, alors il est compté dans `relecturesEnAttente` du relecteur assigné et son statut reste `EN_ATTENTE_RELECTURE` | Should |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| **ENF1** | L'écran de marquage de présence est utilisable sur un téléphone : un champ, un bouton, aucun défilement horizontal | Ouvrir l'écran étudiant dans un navigateur réduit à 360 px de large et marquer une présence sans zoomer |
| **ENF2** | Le tableau du formateur répond en moins de 2 secondes pour une promotion de 60 étudiants et 30 sessions | Charger le jeu de démonstration à cette volumétrie et mesurer le temps de réponse de `GET /api/tableau` |
| **ENF3** | La volumétrie cible est de 5 promotions, 60 étudiants par promotion, une session par jour ouvré, soit de l'ordre de 12 000 présences par an — une base relationnelle mono-instance suffit | Dimensionnement documenté ; aucune optimisation prématurée, index posés sur les clés de recherche |
| **ENF4** | Aucune erreur ne renvoie de stack trace ni de corps vide : toutes sans exception respectent le format `{ code, message }` | Un test d'intégration par famille d'erreur vérifie le code HTTP et la présence des deux champs |
| **ENF5** | Le code de présence n'est pas devinable : 6 caractères tirés aléatoirement dans un alphabet sans caractères ambigus | Inspection du générateur ; deux sessions ouvertes simultanément n'ont jamais le même code (RG2) |
| **ENF6** | L'application démarre chez un tiers depuis un clone vierge, en une commande `docker compose up` ou trois commandes documentées, avec des données de démonstration | Cloner le dépôt dans un dossier vide et suivre le README sans aucune autre information |
| **ENF7** | La suite de tests s'exécute sur un poste vierge, sans base de données locale ni configuration préalable | `./mvnw test` passe après un clone, sur une base embarquée |
| **ENF8** | Les messages d'erreur destinés à l'utilisateur sont en français et compréhensibles sans connaissance technique | Relecture des libellés de chaque code d'erreur |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| **RG1** | Un code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| **RG2** | Le code d'une session est unique parmi les sessions ouvertes | Décision — sans unicité, un code peut désigner deux séances |
| **RG3** | Une présence est unique pour un couple (étudiant, session) ; une seconde tentative est refusée en `409 DEJA_PRESENT` | Contrat |
| **RG4** | Un code qui ne correspond à aucune session ouverte de la promotion de l'étudiant est refusé en `400 CODE_INCONNU` | Contrat |
| **RG5** | Un code dont la date d'expiration est dépassée est refusé en `410 CODE_EXPIRE` | Q2, Contrat |
| **RG6** | Aucune présence ne peut être marquée sur une session clôturée | Q3 |
| **RG7** | Après cinq codes erronés consécutifs, l'étudiant est bloqué 2 minutes et reçoit `429 TROP_D_ESSAIS` | Q4 |
| **RG8** | Une présence ajoutée par le formateur porte la source `FORMATEUR` ; une présence marquée par l'étudiant porte `ETUDIANT`. Cette origine est toujours visible | Q14 |
| **RG9** | Un étudiant ne dépose qu'un seul exercice par session ; un second dépôt est refusé en `409 EXERCICE_DEJA_DEPOSE` | Contrat |
| **RG10** | Le lien d'un exercice doit être une URL absolue en `http` ou `https` ; sinon `400 LIEN_INVALIDE` | Contrat |
| **RG11** | Un exercice ne peut être déposé que par un étudiant présent à la session concernée | Décision — Q7 tire le relecteur parmi les présents, un absent n'est pas relisible |
| **RG12** | Le dépôt d'un exercice reste possible après l'expiration du code, jusqu'à la clôture de la session | Q12 |
| **RG13** | Le lien d'un exercice est remplaçable tant que la session n'est pas clôturée, c'est-à-dire tant qu'aucun relecteur n'a été assigné | Q13, tranchée §7 |
| **RG14** | La clôture d'une session déclenche l'assignation des relecteurs. Elle est irréversible | Décision, comble le trou de §7 |
| **RG15** | Chaque exercice reçoit exactement un relecteur, tiré au hasard parmi les étudiants présents à la session, l'auteur exclu | Q6, Q7, Q5 |
| **RG16** | Si aucun étudiant présent n'est éligible, l'exercice reste au statut `NON_ASSIGNE` et apparaît comme tel dans le tableau | Décision, comble le trou de §7 |
| **RG17** | Un étudiant ne peut jamais relire son propre exercice ; la tentative est refusée en `403 AUTO_RELECTURE` | Q5, Contrat |
| **RG18** | Une note est un entier compris entre 0 et 20 ; toute autre valeur est refusée en `400 NOTE_INVALIDE` | Q9 |
| **RG19** | Une relecture rendue est définitive : toute nouvelle soumission est refusée en `409 RELECTURE_DEJA_RENDUE` | Q15 retenu contre Q10, tranché §7 |
| **RG20** | L'auteur d'un exercice voit la note et le commentaire reçus, jamais l'identité de son relecteur | Q8 |
| **RG21** | Un exercice dont la relecture n'a pas été rendue reste au statut `EN_ATTENTE_RELECTURE` et est visible comme tel par le formateur | Q11 |
| **RG22** | La moyenne d'un étudiant est la moyenne des notes reçues sur ses exercices relus ; elle vaut `null` tant qu'aucune note n'a été reçue | Q16, Contrat |
| **RG23** | Le tableau d'une promotion inexistante est refusé en `404 PROMOTION_INCONNUE` | Contrat |
| **RG24** | Un étudiant appartient à une seule promotion et ne peut marquer sa présence que sur une session de sa promotion | Décision, comble le trou de §7 |
| **RG25** | Aucun mot de passe n'est demandé : l'étudiant se désigne dans la liste de sa promotion | Q1 |

## 7. Zones d'ombre, hypothèses et contradictions

**Points que la demande ne tranche pas :**

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| **Le cycle de vie d'une session n'est jamais défini.** Q3 parle de « fin de la session », Q10 et Q12 de « clôture par le formateur », et rien ne dit ce que sont ces deux moments ni s'ils sont le même | Aucune réponse. Le contrat d'API imposé ne contient aucune opération de clôture — **c'est le trou principal de la demande** | Une session a deux états : `OUVERTE` et `CLOTUREE`. **L'expiration du code (15 min) et la clôture sont deux choses distinctes** : le code cesse de marcher au bout de 15 minutes, la session reste ouverte jusqu'à ce que le formateur la clôture explicitement. « Fin de la session » au sens de Q3 est interprété comme la clôture | Ajout d'une opération `POST /api/sessions/{id}/cloture` au contrat, d'un champ `statut` sur la session, et de EF5 au périmètre. Sans cette décision, Q12 et Q10 sont inapplicables |
| **À quel moment le relecteur est-il assigné ?** Q7 dit « au hasard parmi les étudiants présents à cette session » mais ne dit pas quand | Aucune réponse. Assigner au dépôt est impossible à concilier avec Q12 (dépôt tardif) et Q13 (remplacement du lien) | **L'assignation a lieu à la clôture de la session.** À cet instant seulement, la liste des présents et la liste des exercices sont toutes deux définitives | Rend Q12, Q13 et Q7 compatibles entre elles, et donne sa raison d'être à la clôture. EF5 devient une story `Must` : sans clôture, aucune relecture n'existe |
| **Que signifie « avoir commencé à relire » ?** (Q13) | Aucun événement observable ne correspond : une relecture est assignée, puis rendue. Rien entre les deux | « Commencé à relire » est assimilé à « avoir été assigné ». Le lien est donc remplaçable tant que la session est ouverte | RG13. On évite d'inventer un état `EN_COURS` que rien ne déclencherait côté système |
| **Que faire si aucun relecteur n'est éligible ?** Un seul étudiant présent, ou un seul déposant | Aucune réponse | L'exercice reste `NON_ASSIGNE`, sans relecture ni note, et le formateur le voit dans son tableau | RG16. Le cas se produit à chaque session de démonstration à un seul étudiant, il ne peut pas être ignoré |
| **Le blocage de Q4 porte sur quoi ?** Cinq erreurs de qui : d'un étudiant, d'un appareil, d'une adresse réseau ? | Q4 dit seulement « bloquez-le deux minutes » | Blocage **par étudiant sélectionné**, compteur en mémoire remis à zéro après un succès ou après 2 minutes | RG7. Le contrat ne prévoit pas de code HTTP pour ce cas : on ajoute `429 TROP_D_ESSAIS`, documenté comme extension |
| **Qui crée les promotions, les étudiants et les formateurs ?** | Q1 supprime l'authentification mais ne dit rien de l'origine des données | Hors périmètre. Les données sont injectées par une migration de démonstration | Le correcteur ouvre une application déjà peuplée (ENF6) |
| **Comment le formateur est-il identifié ?** | Aucune réponse. Q1 ne concerne que l'étudiant | Un formateur unique par promotion dans le jeu de démonstration, aucun contrôle d'accès | Assumé comme une faiblesse : en production, ouvrir une session ou clôturer une séance exigerait une authentification |
| **Un étudiant peut-il marquer sa présence à la session d'une autre promotion ?** | Aucune réponse | Non : le code est recherché parmi les sessions ouvertes de la promotion de l'étudiant uniquement. Un code valide d'une autre promotion est traité comme inconnu | RG24, réponse `400 CODE_INCONNU` — on ne révèle pas l'existence d'un code qui ne concerne pas l'étudiant |
| **Sur quoi porte la moyenne du tableau ?** Notes reçues ou notes données ? | Q16 dit « la moyenne des notes reçues » | Moyenne des notes **reçues** par l'étudiant sur ses propres exercices. `null` si aucune | RG22, conforme au champ `moyenne` nullable du contrat |

**Contradictions relevées :**

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** — « Un relecteur peut corriger sa note, tant que le formateur n'a pas clôturé la session »<br>**Q15** — « Une fois que le relecteur a validé, c'est fini, il ne peut plus y revenir » | **Q15 : la note est définitive dès validation.** Une relecture rendue ne peut plus être modifiée (RG19) | Trois raisons. D'abord, **le contrat d'API impose `409 RELECTURE_DEJA_RENDUE`** sur `POST /api/relectures/{id}` : le comportement attendu côté API est explicitement le refus, et le contrat prime sur une réponse orale. Ensuite, Q15 énonce une intention argumentée par le client lui-même — « c'est plus honnête pour tout le monde » — quand Q10 n'est qu'un « oui » sans motif. Enfin, retenir Q10 rendrait le `409` du contrat inatteignable, donc le contrat incohérent. **Conséquence assumée :** une note erronée ne peut pas être rattrapée dans cette version ; si le client conteste, la reprise est une évolution à chiffrer, pas un correctif |

*Une hypothèse écrite est toujours acceptée. Une hypothèse silencieuse est une faute.*

## 8. Contraintes techniques

**Imposées par le sujet :**

| Réf | Contrainte |
|---|---|
| **B1** | Java 17 ou plus, Maven, wrapper `mvnw` commité |
| **B2** | `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes de statut, format d'erreur |
| **B3** | Séparation contrôleur / service / repository ; aucune requête base dans un contrôleur, aucune entité JPA exposée en JSON — passage obligatoire par des DTO |
| **B4** | Validation des entrées et gestion centralisée des erreurs par `@RestControllerAdvice` ; jamais de stack trace renvoyée au client |
| **B5** | Schéma versionné par Flyway, migrations commitées ; `ddl-auto=update` interdit hors tests |
| **B6** | Au moins un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, exécutables sur un poste vierge |
| **F1** | Framework frontend déclaré et justifié dans le README, et build qui passe |
| **F2** | Trois écrans : formateur, étudiant, relecteur |
| **F3** | Appels API dans une couche dédiée, états de chargement et d'erreur gérés, aucune règle métier dupliquée côté client |

**Que je m'impose en plus :**

- **Java 21** (installé sur le poste), **Spring Boot 4.1**, **Maven** avec wrapper. Spring Boot 3 n'est plus proposé par `start.spring.io`, dont la plage de compatibilité commence à 4.0.0 — la version retenue est donc 4.1.1.
- **PostgreSQL 16** en exécution, lancé par `docker compose`. **Aucune** génération de schéma par Hibernate : `spring.jpa.hibernate.ddl-auto=validate`.
- **Flyway**, migrations numérotées `V1__…`, `V2__…`, écrites en **SQL portable** — aucune extension propriétaire — de sorte que les tests d'intégration tournent sur **H2 en mode compatibilité PostgreSQL** sans Docker ni base locale (ENF7). Une migration déjà poussée n'est jamais modifiée : toute correction passe par une migration supplémentaire, ce qui sera déterminant à l'étape 3.
- **Mapping entité → DTO écrit à la main**, sans générateur : le volume est faible et la lisibilité prime.
- **Génération du code de présence** : 6 caractères tirés par `SecureRandom` dans l'alphabet `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, sans les caractères ambigus `I`, `O`, `0`, `1`.
- **Tirage du relecteur** : la source d'aléa est injectée par une interface, afin que le tirage soit déterministe en test.
- **Frontend Next.js** (App Router, TypeScript), tous les appels réseau regroupés dans `src/services/api.ts` — aucun `fetch` ailleurs dans le code. La moyenne affichée provient de l'API et n'est jamais recalculée côté client.
- **Fuseau horaire** : toutes les dates sont stockées et échangées en UTC, au format ISO-8601 avec décalage.
- **Aucun secret dans le dépôt** : les identifiants de base de données de développement vivent dans `docker-compose.yml` et sont, par nature, non sensibles.

## 9. Livrables

- `docs/CAHIER_DES_CHARGES.md` — le présent document, maintenu à jour après l'étape 3
- `docs/diagrammes/` — D1 cas d'utilisation, D2 modèle de données, D3 séquence « marquer sa présence », D4 états-transitions de l'exercice, tous en Mermaid versionné
- `docs/JOURNAL.md` — une entrée par étape, écrite à la fin de chaque étape
- `api/contrat.yaml` — le contrat imposé complété des opérations nécessaires, figé avant le premier commit de code
- `backend/` — application Spring Boot avec ses migrations Flyway et ses tests
- `frontend/` — application Next.js avec ses trois écrans
- `README.md` — installation et démarrage, testés depuis un clone vierge
- `CHANGELOG.md` — cohérent avec l'historique Git
- Le backlog, sous forme d'issues sur le dépôt, priorisées et reliées aux `EFx` / `RGx`
- `docs/SOUMISSION.md` — téléversé sur la plateforme

## 10. Démarche prévue

**Étape 1 — Analyse.** Ce cahier des charges, les diagrammes, le contrat complété et le backlog en issues, puis le commit `[JALON] analyse`. Aucune ligne de code, aucun `spring init` avant ce jalon.

**Étape 2 — Première version.** Les seules stories `Must` : EF1 à EF8. Une branche par issue, une pull request par branche, l'issue fermée par le commit de fusion. Le schéma est versionné dès la première migration. Puis `[JALON] v0.1`.

**Étape 3 — Enveloppe.** Dans cet ordre, sans raccourci : ouvrir l'issue avant d'écrire la moindre ligne, reproduire le bug par un test qui échoue, corriger, puis traiter le changement de besoin **dans une branche séparée** — le correctif et l'évolution ne partagent jamais un commit. Toute modification du schéma passe par une nouvelle migration. Le contrat, ce document et les diagrammes sont mis à jour dans un commit qui l'annonce, et le backlog re-priorisé par écrit dans le journal.

**Étape 4 — Version finale.** `[JALON] v1.0`, `CHANGELOG.md`, README vérifié depuis un clone vierge dans un dossier vide, backlog restant trié et assumé.

**Étape 5 — Épreuve Git**, sur un second dépôt, historique strictement séparé de celui-ci.

**Étape 6 — Soumission** avant 18h00, hash relevés une fois le travail terminé.

**Si je prends du retard :** je sacrifie dans cet ordre EF12, puis EF11, puis EF13, puis EF10, puis EF9. Je ne sacrifie jamais les migrations, les tests B6, ni la mise à jour documentaire de l'étape 3 — ce sont les postes du barème les plus coûteux à perdre. Un périmètre réduit et annoncé vaut mieux qu'une promesse non tenue.

**Definition of Done — un ticket est terminé quand :**

- le comportement décrit par son critère d'acceptation est vérifiable par un tiers sur l'application qui tourne ;
- le code respecte la séparation contrôleur / service / repository et n'expose aucune entité JPA ;
- les cas d'erreur de l'opération renvoient le code HTTP du contrat et le format `{ code, message }` ;
- toute règle de gestion implémentée est citée par son numéro `RGx` dans le message de commit ;
- toute modification de schéma est portée par une migration Flyway nouvelle ;
- `./mvnw test` passe et le build du frontend passe ;
- la pull request est fusionnée dans `main` et ferme son issue.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 25/09/2026 | Version initiale, avant tout code. Contradiction Q10/Q15 tranchée en faveur de Q15 ; quatre trous de la demande comblés par décision (cycle de vie de la session, moment de l'assignation, absence de relecteur éligible, appartenance à une promotion) |

*L'étape 3 rendra une partie de ce document faux. Revenir le corriger et le noter ici — un cahier des charges périmé est un cahier des charges mort.*
