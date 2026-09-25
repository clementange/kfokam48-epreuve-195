import type { Source, StatutExercice, StatutRelecture, StatutSession } from '@/lib/api';

/**
 * Les étiquettes de statut — ENF9.
 *
 * Deux règles tenues ici :
 *
 * 1. **Un statut porte toujours un libellé en plus de sa couleur.** L'information
 *    ne doit jamais reposer sur la seule teinte : daltonisme, impression en noir
 *    et blanc, écran mal calibré.
 * 2. **La même couleur signifie la même chose partout.** Vert = abouti,
 *    orange = en attente de quelqu'un, rouge = impossible, gris = neutre.
 */

type Ton = 'succes' | 'attente' | 'danger' | 'neutre' | 'info';

function Etiquette({ ton, children }: { ton: Ton; children: React.ReactNode }) {
  return <span className={`etiquette ${ton}`}>{children}</span>;
}

const EXERCICE: Record<StatutExercice, { ton: Ton; libelle: string }> = {
  DEPOSE: { ton: 'info', libelle: 'Déposé' },
  EN_ATTENTE_RELECTURE: { ton: 'attente', libelle: 'En attente de relecture' },
  // Orange comme « en attente » : il manque encore quelqu'un. La note existe
  // mais elle n'est pas figée (RG26).
  PARTIELLEMENT_RELU: { ton: 'attente', libelle: 'Relu par 1 pair sur 2' },
  RELU: { ton: 'succes', libelle: 'Relu' },
  // Rouge, parce que c'est une impasse : personne ne le notera jamais (RG16).
  NON_ASSIGNE: { ton: 'danger', libelle: 'Aucun relecteur disponible' },
};

export function StatutExerciceEtiquette({ statut }: { statut: StatutExercice }) {
  const { ton, libelle } = EXERCICE[statut];
  return <Etiquette ton={ton}>{libelle}</Etiquette>;
}

export function StatutSessionEtiquette({ statut }: { statut: StatutSession }) {
  return statut === 'OUVERTE' ? (
    <Etiquette ton="succes">Ouverte</Etiquette>
  ) : (
    <Etiquette ton="neutre">Clôturée</Etiquette>
  );
}

export function StatutRelectureEtiquette({ statut }: { statut: StatutRelecture }) {
  return statut === 'RENDUE' ? (
    <Etiquette ton="succes">Rendue</Etiquette>
  ) : (
    <Etiquette ton="attente">À rendre</Etiquette>
  );
}

/**
 * RG8, Q14 — « il faut que ça se voie : marquez "ajouté par le formateur" ».
 * C'est la raison d'être du champ `source` : elle doit être visible à l'écran,
 * pas seulement en base.
 */
export function SourceEtiquette({ source }: { source: Source }) {
  return source === 'FORMATEUR' ? (
    <Etiquette ton="attente">Ajouté par le formateur</Etiquette>
  ) : (
    <Etiquette ton="neutre">Saisi par l&apos;étudiant</Etiquette>
  );
}

/**
 * RG22 — une moyenne absente s'écrit « — », jamais 0 : ce n'est pas la même chose.
 *
 * RG26 — une note provisoire est signalée explicitement. Le client l'a demandé
 * mot pour mot : « on affiche sa note en attendant, mais marquée comme
 * provisoire ». Sans cette mention, l'étudiant croirait sa note définitive.
 */
export function Moyenne({ valeur, provisoire }: { valeur: number | null; provisoire?: boolean }) {
  if (valeur === null || valeur === undefined) {
    return <span className="sans-valeur" title="Aucune note reçue pour l'instant">—</span>;
  }
  return (
    <>
      <strong>{valeur.toFixed(2).replace('.', ',')} / 20</strong>
      {provisoire && (
        <>
          {' '}
          <span className="etiquette attente" title="Une seconde relecture est attendue : cette note peut encore changer">
            provisoire
          </span>
        </>
      )}
    </>
  );
}
