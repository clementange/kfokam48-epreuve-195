'use client';

import { useEffect, useState } from 'react';
import { api, type Etudiant, type Promotion } from '@/lib/api';
import { Chargement, Erreur } from './Etat';

/**
 * Sélecteur de promotion, et optionnellement d'étudiant.
 *
 * Q1, RG25 — « l'étudiant choisit son nom dans une liste ». Il n'y a aucune
 * authentification : cette liste en tient lieu. Simplification assumée du
 * sujet, pas un oubli.
 */
export function SelecteurPromotion({
  promotionId,
  onPromotion,
  etudiantId,
  onEtudiant,
}: {
  promotionId: number | null;
  onPromotion: (id: number) => void;
  etudiantId?: number | null;
  onEtudiant?: (id: number) => void;
}) {
  const [promotions, setPromotions] = useState<Promotion[] | null>(null);
  const [erreur, setErreur] = useState<unknown>(null);

  useEffect(() => {
    let annule = false;
    api.promotions()
      .then((p) => {
        if (annule) return;
        setPromotions(p);
        if (p.length > 0 && promotionId === null) onPromotion(p[0].id);
      })
      .catch((e) => !annule && setErreur(e));
    return () => {
      annule = true;
    };
    // Chargé une seule fois : la liste des promotions ne change pas en séance.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (erreur) return <Erreur erreur={erreur} />;
  if (!promotions) return <Chargement quoi="des promotions" />;

  return (
    <>
      <div className="champ">
        <label htmlFor="promotion">Promotion</label>
        <select
          id="promotion"
          value={promotionId ?? ''}
          onChange={(e) => onPromotion(Number(e.target.value))}
        >
          {promotions.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nom} — {p.formateurNom}
            </option>
          ))}
        </select>
      </div>

      {onEtudiant && promotionId !== null && (
        // La `key` remonte le composant quand la promotion change : son état
        // se réinitialise de lui-même. C'est la façon dont React recommande de
        // réinitialiser un état sur changement de prop — un `setState` dans un
        // effet provoquerait un rendu en cascade, que la règle
        // react-hooks/set-state-in-effect interdit à juste titre.
        <ListeEtudiants
          key={promotionId}
          promotionId={promotionId}
          etudiantId={etudiantId ?? null}
          onEtudiant={onEtudiant}
        />
      )}
    </>
  );
}

function ListeEtudiants({
  promotionId,
  etudiantId,
  onEtudiant,
}: {
  promotionId: number;
  etudiantId: number | null;
  onEtudiant: (id: number) => void;
}) {
  const [etudiants, setEtudiants] = useState<Etudiant[] | null>(null);
  const [erreur, setErreur] = useState<unknown>(null);

  useEffect(() => {
    let annule = false;
    api.etudiants(promotionId)
      .then((e) => !annule && setEtudiants(e))
      .catch((e) => !annule && setErreur(e));
    return () => {
      annule = true;
    };
  }, [promotionId]);

  return (
    <div className="champ">
      <label htmlFor="etudiant">Je suis</label>
      {erreur ? (
        <Erreur erreur={erreur} />
      ) : !etudiants ? (
        <Chargement quoi="de la liste des étudiants" />
      ) : (
        <select
          id="etudiant"
          value={etudiantId ?? ''}
          onChange={(e) => onEtudiant(Number(e.target.value))}
        >
          <option value="">— choisissez votre nom —</option>
          {etudiants.map((e) => (
            <option key={e.id} value={e.id}>
              {e.nom}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
