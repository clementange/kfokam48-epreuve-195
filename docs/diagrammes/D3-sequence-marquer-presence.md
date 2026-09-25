# D3 — Séquence : marquer sa présence

> **Ce diagramme fait foi pour les codes HTTP de `POST /api/presences`.** Chaque branche
> correspond à une réponse déclarée dans `api/contrat.yaml`. Toute divergence entre les deux
> est un défaut à corriger dans le même commit.

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (Next.js)
    participant API as PresenceController
    participant S as PresenceService
    participant R as Repositories
    participant DB as PostgreSQL

    E->>F: choisit son nom puis saisit le code
    F->>API: POST /api/presences { code, etudiantId }

    alt corps invalide : code vide ou etudiantId absent
        API-->>F: 400 { code: "REQUETE_INVALIDE" }
        Note over API: @Valid, intercepté par le @RestControllerAdvice (B4)
    else étudiant bloqué — 5 erreurs en moins de 2 min (RG7)
        API->>S: enregistrer(code, etudiantId)
        S-->>API: TropDEssaisException
        API-->>F: 429 { code: "TROP_D_ESSAIS" }
    else
        API->>S: enregistrer(code, etudiantId)
        S->>R: etudiantRepository.findById(etudiantId)
        R->>DB: SELECT * FROM etudiant WHERE id = ?
        DB-->>R: étudiant + promotion_id
        R-->>S: Etudiant

        alt étudiant inconnu
            S-->>API: EtudiantInconnuException
            API-->>F: 400 { code: "ETUDIANT_INCONNU" }
        else
            S->>R: sessionRepository.findByCodeAndPromotion(code, promotionId)
            R->>DB: SELECT * FROM session WHERE code = ? AND promotion_id = ?
            DB-->>R: 0 ou 1 ligne
            R-->>S: Optional~Session~

            alt aucune session pour ce code dans sa promotion (RG4, RG24)
                S->>S: incrémente le compteur d'échecs (RG7)
                S-->>API: CodeInconnuException
                API-->>F: 400 { code: "CODE_INCONNU" }
            else session clôturée (RG6)
                S-->>API: SessionClotureeException
                API-->>F: 409 { code: "SESSION_CLOTUREE" }
            else maintenant > expiration_at (RG1, RG5)
                S-->>API: CodeExpireException
                API-->>F: 410 { code: "CODE_EXPIRE" }
            else présence déjà enregistrée (RG3)
                S->>R: presenceRepository.existsBySessionAndEtudiant(...)
                R-->>S: true
                S-->>API: DejaPresentException
                API-->>F: 409 { code: "DEJA_PRESENT" }
            else cas nominal
                S->>S: remet le compteur d'échecs à zéro (RG7)
                S->>R: presenceRepository.save(Presence source=ETUDIANT)
                R->>DB: INSERT INTO presence (...) VALUES (...)
                DB-->>R: id généré
                Note over DB: la contrainte UNIQUE(session_id, etudiant_id)<br/>protège contre deux requêtes simultanées ;<br/>sa violation est traduite en 409 DEJA_PRESENT
                R-->>S: Presence
                S-->>API: PresenceDTO
                API-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
                F-->>E: « Présence enregistrée »
            end
        end
    end
```

## Correspondance avec le contrat d'API

| Situation | Règle | Code HTTP | `code` d'erreur | Au contrat |
|---|---|---|---|---|
| Présence enregistrée | EF2 | **201** | — | ✅ imposé |
| Corps de requête invalide | B4 | **400** | `REQUETE_INVALIDE` | ✅ imposé (`400`) |
| Étudiant inexistant | — | **400** | `ETUDIANT_INCONNU` | ✅ imposé (`400`) |
| Code introuvable, ou appartenant à une autre promotion | RG4, RG24 | **400** | `CODE_INCONNU` | ✅ imposé |
| Présence déjà enregistrée | RG3 | **409** | `DEJA_PRESENT` | ✅ imposé |
| Session clôturée | RG6 | **409** | `SESSION_CLOTUREE` | ➕ ajouté |
| Code expiré depuis plus de 15 min | RG1, RG5 | **410** | `CODE_EXPIRE` | ✅ imposé |
| Six tentatives en moins de deux minutes | RG7 | **429** | `TROP_D_ESSAIS` | ➕ ajouté |

**Toutes** les réponses d'erreur, sans exception, portent le corps `{ "code": ..., "message": ... }`
produit par le `@RestControllerAdvice` (B4, ENF4). Aucune stack trace, aucun corps vide, jamais
la page d'erreur par défaut de Spring.

## Deux points d'attention

1. **L'ordre des contrôles est signifiant.** La clôture est vérifiée **avant** l'expiration : une
   session clôturée le reste définitivement, alors qu'un code expiré sur une session ouverte
   décrit une situation différente que l'étudiant doit pouvoir comprendre. Un formateur peut
   d'ailleurs encore ajouter cette présence à la main (RG8).
2. **`CODE_INCONNU` est renvoyé aussi lorsque le code existe mais appartient à une autre
   promotion** (RG24). C'est délibéré : répondre « ce code n'est pas pour vous » confirmerait
   son existence, ce que Q4 cherche justement à éviter.
