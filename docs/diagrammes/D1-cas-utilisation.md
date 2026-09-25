# D1 — Diagramme de cas d'utilisation

> Acteurs et cas d'utilisation de **Présence & Relecture KFOKAM48**.
> Le **relecteur** n'est pas un acteur physique distinct : c'est un étudiant que le système
> a désigné pour relire l'exercice d'un pair (cf. cahier des charges §2). Il est représenté
> séparément parce que ses cas d'utilisation sont distincts, mais il hérite de l'étudiant.

```mermaid
flowchart LR
    Formateur(("👤 Formateur"))
    Etudiant(("👤 Étudiant"))
    Relecteur(("👤 Relecteur"))
    Systeme(("⚙️ Système"))

    subgraph SUT["Présence &amp; Relecture KFOKAM48"]
        direction TB
        UC1["UC1 — Ouvrir une session<br/>et obtenir un code<br/><i>EF1</i>"]
        UC2["UC2 — Clôturer une session<br/><i>EF5 · RG14</i>"]
        UC3["UC3 — Ajouter une présence<br/>à la main<br/><i>EF9 · RG8</i>"]
        UC4["UC4 — Consulter le tableau<br/>récapitulatif<br/><i>EF8 · RG22</i>"]
        UC5["UC5 — Se désigner dans<br/>la liste de sa promotion<br/><i>EF3 · RG25</i>"]
        UC6["UC6 — Marquer sa présence<br/>avec le code<br/><i>EF2 · RG1 RG3 RG5</i>"]
        UC7["UC7 — Déposer le lien<br/>de son exercice<br/><i>EF4 · RG9 RG10</i>"]
        UC8["UC8 — Remplacer le lien<br/>de son exercice<br/><i>EF10 · RG13</i>"]
        UC9["UC9 — Consulter la note<br/>et le commentaire reçus<br/><i>EF11 · RG20</i>"]
        UC10["UC10 — Rendre une note<br/>et un commentaire<br/><i>EF7 · RG18 RG19</i>"]
        UC11["UC11 — Assigner un relecteur<br/>au hasard parmi les présents<br/><i>EF6 · RG15 RG16</i>"]
        UC12["UC12 — Limiter les tentatives<br/>après 5 codes erronés<br/><i>EF12 · RG7</i>"]
    end

    Formateur --- UC1
    Formateur --- UC2
    Formateur --- UC3
    Formateur --- UC4

    Etudiant --- UC5
    Etudiant --- UC6
    Etudiant --- UC7
    Etudiant --- UC8
    Etudiant --- UC9

    Relecteur --- UC10
    Relecteur -. "hérite de" .-> Etudiant

    Systeme --- UC11
    Systeme --- UC12

    UC2 -. "«include»" .-> UC11
    UC6 -. "«include»" .-> UC12
    UC7 -. "«precondition»<br/>RG11 : être présent" .-> UC6
    UC10 -. "«exclut»<br/>RG17 : jamais son propre exercice" .-> UC7
```

## Lecture

| Acteur | Cas d'utilisation | Remarque |
|---|---|---|
| **Formateur** | UC1 à UC4 | UC2 déclenche systématiquement UC11 : clôturer, c'est assigner (RG14) |
| **Étudiant** | UC5 à UC9 | UC7 suppose UC6 : on ne dépose que si l'on est présent (RG11) |
| **Relecteur** | UC10 | Étudiant assigné par UC11 ; ne peut jamais relire son propre dépôt (RG17) |
| **Système** | UC11, UC12 | Acteur non humain : le tirage du relecteur (Q7) et le blocage après 5 erreurs (Q4) ne sont déclenchés par personne |

**Hors périmètre, donc absents du diagramme :** s'authentifier, gérer les comptes et les promotions, être notifié d'une assignation, exporter le tableau (cahier des charges §3).
