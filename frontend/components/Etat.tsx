'use client';

import { ErreurMetier } from '@/lib/api';

/**
 * Les états transverses — contrainte F3 : « états de chargement et d'erreur
 * gérés ». Regroupés ici pour qu'aucun écran ne puisse les oublier, et pour
 * qu'ils se ressemblent d'un écran à l'autre.
 */

export function Chargement({ quoi }: { quoi: string }) {
  return <p className="chargement">Chargement {quoi}…</p>;
}

/**
 * Affiche le message renvoyé par l'API, jamais un message inventé.
 *
 * Le `code` du contrat est montré en petit : il ne sert à rien à l'utilisateur,
 * mais il permet au formateur de le citer s'il signale un problème — et il rend
 * visible que le format d'erreur imposé est bien respecté.
 */
export function Erreur({ erreur }: { erreur: unknown }) {
  if (!erreur) return null;
  const metier = erreur instanceof ErreurMetier ? erreur : null;
  return (
    <div className="message erreur" role="alert">
      {metier ? metier.message : "Une erreur inattendue s'est produite."}
      {metier && <span className="code-erreur">{metier.code}</span>}
    </div>
  );
}

export function Succes({ children }: { children: React.ReactNode }) {
  return (
    <div className="message succes" role="status">
      {children}
    </div>
  );
}

export function Info({ children }: { children: React.ReactNode }) {
  return <div className="message info">{children}</div>;
}

/** Un tableau vide explique pourquoi il est vide, plutôt que de rester blanc. */
export function Vide({ children }: { children: React.ReactNode }) {
  return <p className="vide">{children}</p>;
}
