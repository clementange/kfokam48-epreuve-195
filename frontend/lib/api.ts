/**
 * La couche d'appels API — contrainte F3.
 *
 * Aucun `fetch` ne doit exister ailleurs dans l'application. Tout passe par ici,
 * pour trois raisons : l'adresse du backend est définie à un seul endroit, le
 * format d'erreur imposé par le contrat est traduit une seule fois, et aucune
 * règle métier ne peut se glisser dans un composant.
 */

const BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

/** Le corps d'erreur imposé par le contrat, pour toutes les erreurs sans exception. */
export type ErreurApi = { code: string; message: string };

/**
 * Erreur métier remontée par le backend.
 *
 * On conserve `code` en plus du message : l'interface peut ainsi réagir
 * différemment selon le cas — un `CODE_EXPIRE` n'appelle pas la même action
 * qu'un `DEJA_PRESENT` — sans analyser une chaîne de caractères.
 */
export class ErreurMetier extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly statut: number,
  ) {
    super(message);
    this.name = 'ErreurMetier';
  }
}

async function appeler<T>(chemin: string, options: RequestInit = {}): Promise<T> {
  let reponse: Response;
  try {
    reponse = await fetch(`${BASE}${chemin}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
      cache: 'no-store',
    });
  } catch {
    // Le backend est injoignable. Ce n'est pas une erreur métier : on le dit
    // clairement plutôt que d'afficher « undefined ».
    throw new ErreurMetier(
      'BACKEND_INJOIGNABLE',
      "Le serveur ne répond pas. Vérifiez qu'il est démarré.",
      0,
    );
  }

  if (reponse.status === 204) return undefined as T;

  const texte = await reponse.text();
  const corps = texte ? JSON.parse(texte) : null;

  if (!reponse.ok) {
    const erreur = corps as ErreurApi | null;
    throw new ErreurMetier(
      erreur?.code ?? 'ERREUR_INCONNUE',
      erreur?.message ?? `Erreur ${reponse.status}.`,
      reponse.status,
    );
  }

  return corps as T;
}

const get = <T>(chemin: string) => appeler<T>(chemin);
const post = <T>(chemin: string, corps?: unknown) =>
  appeler<T>(chemin, { method: 'POST', body: corps ? JSON.stringify(corps) : undefined });
const put = <T>(chemin: string, corps: unknown) =>
  appeler<T>(chemin, { method: 'PUT', body: JSON.stringify(corps) });

// --- Types du contrat -------------------------------------------------------

export type StatutSession = 'OUVERTE' | 'CLOTUREE';
export type StatutExercice = 'DEPOSE' | 'EN_ATTENTE_RELECTURE' | 'RELU' | 'NON_ASSIGNE';
export type StatutRelecture = 'EN_ATTENTE' | 'RENDUE';
export type Source = 'ETUDIANT' | 'FORMATEUR';

export type Promotion = { id: number; nom: string; formateurId: number; formateurNom: string };
export type Etudiant = { id: number; nom: string; promotionId: number };

export type Session = {
  id: number;
  titre: string;
  promotionId: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
  statut: StatutSession;
  clotureAt: string | null;
};

export type SessionOuverte = {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
};

export type Cloture = {
  id: number;
  statut: StatutSession;
  clotureAt: string;
  relecturesAssignees: number;
  exercicesNonAssignes: number;
};

export type Presence = { id: number; sessionId: number; etudiantId: number; source: Source };

export type ExerciceDepose = { id: number; statut: StatutExercice };

export type Exercice = {
  id: number;
  sessionId: number;
  etudiantId: number;
  lien: string;
  statut: StatutExercice;
  deposeAt: string;
  majAt: string | null;
};

export type MonExercice = {
  exerciceId: number;
  sessionId: number;
  sessionTitre: string;
  lien: string;
  statut: StatutExercice;
  note: number | null;
  commentaire: string | null;
};

export type RelectureAssignee = {
  relectureId: number;
  exerciceId: number;
  sessionId: number;
  sessionTitre: string;
  auteurNom: string;
  lien: string;
  statut: StatutRelecture;
  note: number | null;
  commentaire: string | null;
};

/**
 * Une ligne du tableau du formateur.
 *
 * `moyenne` arrive déjà calculée et arrondie par l'API : F3 interdit de la
 * recalculer ici. `null` signifie « aucune note reçue », ce qui n'est pas zéro.
 */
export type LigneTableau = {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  relecturesEnAttente: number;
};

// --- Opérations -------------------------------------------------------------

export const api = {
  promotions: () => get<Promotion[]>('/api/promotions'),
  etudiants: (promotionId: number) => get<Etudiant[]>(`/api/promotions/${promotionId}/etudiants`),

  sessions: (promotionId: number) => get<Session[]>(`/api/sessions?promotionId=${promotionId}`),
  ouvrirSession: (titre: string, promotionId: number) =>
    post<SessionOuverte>('/api/sessions', { titre, promotionId }),
  cloturerSession: (id: number) => post<Cloture>(`/api/sessions/${id}/cloture`),

  marquerPresence: (code: string, etudiantId: number) =>
    post<Presence>('/api/presences', { code, etudiantId }),

  deposerExercice: (sessionId: number, etudiantId: number, lien: string) =>
    post<ExerciceDepose>('/api/exercices', { sessionId, etudiantId, lien }),
  remplacerLien: (exerciceId: number, lien: string) =>
    put<Exercice>(`/api/exercices/${exerciceId}`, { lien }),

  mesExercices: (etudiantId: number) => get<MonExercice[]>(`/api/etudiants/${etudiantId}/exercices`),
  mesRelectures: (etudiantId: number, statut?: StatutRelecture) =>
    get<RelectureAssignee[]>(
      `/api/etudiants/${etudiantId}/relectures${statut ? `?statut=${statut}` : ''}`,
    ),
  rendreRelecture: (relectureId: number, note: number, commentaire: string) =>
    post<void>(`/api/relectures/${relectureId}`, { note, commentaire }),

  tableau: (promotionId: number) => get<LigneTableau[]>(`/api/tableau?promotionId=${promotionId}`),
};
