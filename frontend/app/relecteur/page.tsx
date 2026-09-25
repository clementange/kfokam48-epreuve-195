'use client';

import { useCallback, useEffect, useState } from 'react';
import { api, type RelectureAssignee } from '@/lib/api';
import { Chargement, Erreur, Info, Succes, Vide } from '@/components/Etat';
import { StatutRelectureEtiquette } from '@/components/Etiquette';
import { SelecteurPromotion } from '@/components/SelecteurPromotion';

/**
 * Écran relecteur — EF7.
 *
 * Le relecteur n'est pas un acteur distinct : c'est un étudiant que le système
 * a désigné à la clôture d'une séance. On se désigne donc de la même façon que
 * sur l'écran étudiant.
 *
 * RG19 — une relecture rendue s'affiche en lecture seule. La note est
 * définitive : lui laisser un formulaire modifiable serait mentir sur ce que
 * l'application accepte.
 */
export default function EcranRelecteur() {
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  const [relectures, setRelectures] = useState<RelectureAssignee[] | null>(null);
  const [erreur, setErreur] = useState<unknown>(null);
  const [succes, setSucces] = useState<string | null>(null);

  const [ouverte, setOuverte] = useState<number | null>(null);
  const [note, setNote] = useState('');
  const [commentaire, setCommentaire] = useState('');
  const [envoiEnCours, setEnvoiEnCours] = useState(false);

  const charger = useCallback(async (id: number) => {
    // Aucun setState avant le premier await — voir le commentaire équivalent
    // sur l'écran formateur.
    try {
      const miennes = await api.mesRelectures(id);
      setErreur(null);
      setRelectures(miennes);
    } catch (e) {
      setErreur(e);
    }
  }, []);

  useEffect(() => {
    if (etudiantId === null) return;
    let annule = false;
    api.mesRelectures(etudiantId)
      .then((m) => {
        if (annule) return;
        setErreur(null);
        setRelectures(m);
      })
      .catch((e) => !annule && setErreur(e));
    return () => {
      annule = true;
    };
  }, [etudiantId]);

  async function rendre(e: React.FormEvent, relectureId: number) {
    e.preventDefault();
    if (etudiantId === null) return;
    setErreur(null);
    setSucces(null);
    setEnvoiEnCours(true);
    try {
      await api.rendreRelecture(relectureId, Number(note), commentaire);
      setSucces('Relecture rendue. La note est définitive : elle ne peut plus être modifiée.');
      setOuverte(null);
      setNote('');
      setCommentaire('');
      await charger(etudiantId);
    } catch (err) {
      setErreur(err);
    } finally {
      setEnvoiEnCours(false);
    }
  }

  const aRendre = (relectures ?? []).filter((r) => r.statut === 'EN_ATTENTE');
  const rendues = (relectures ?? []).filter((r) => r.statut === 'RENDUE');

  return (
    <>
      <h2>Écran relecteur</h2>
      <p className="sous-titre">
        Relire l&apos;exercice d&apos;un pair et lui attribuer une note entière sur 20.
      </p>

      <section className="carte">
        <h3>Qui êtes-vous ?</h3>
        <p className="aide">
          Les relectures vous sont attribuées au hasard, à la clôture de la séance, parmi les
          étudiants présents (Q7).
        </p>
        <SelecteurPromotion
          promotionId={promotionId}
          onPromotion={(id) => {
            setPromotionId(id);
            setEtudiantId(null);
            setRelectures(null);
          }}
          etudiantId={etudiantId}
          onEtudiant={(id) => {
            setRelectures(null);
            setOuverte(null);
            setEtudiantId(id);
          }}
        />
      </section>

      <Erreur erreur={erreur} />
      {succes && <Succes>{succes}</Succes>}

      {etudiantId === null ? (
        <Info>Choisissez votre nom ci-dessus pour voir vos relectures.</Info>
      ) : !relectures ? (
        <Chargement quoi="de vos relectures" />
      ) : (
        <>
          <section className="carte">
            <h3>À rendre ({aRendre.length})</h3>
            {aRendre.length === 0 ? (
              <Vide>
                Aucune relecture ne vous attend. Elles sont attribuées à la clôture d&apos;une
                séance.
              </Vide>
            ) : (
              aRendre.map((r) => (
                <article key={r.relectureId} className="carte" style={{ boxShadow: 'none' }}>
                  <h3>
                    {r.sessionTitre} — exercice de {r.auteurNom}
                  </h3>
                  <p className="aide">
                    <a href={r.lien} target="_blank" rel="noreferrer noopener">
                      {r.lien}
                    </a>
                  </p>

                  {ouverte === r.relectureId ? (
                    <form onSubmit={(e) => rendre(e, r.relectureId)}>
                      <div className="champ">
                        <label htmlFor={`note-${r.relectureId}`}>
                          Note sur 20 — nombre entier (Q9)
                        </label>
                        <input
                          id={`note-${r.relectureId}`}
                          type="number"
                          min={0}
                          max={20}
                          step={1}
                          value={note}
                          onChange={(e) => setNote(e.target.value)}
                          required
                        />
                      </div>
                      <div className="champ">
                        <label htmlFor={`com-${r.relectureId}`}>Commentaire</label>
                        <textarea
                          id={`com-${r.relectureId}`}
                          value={commentaire}
                          onChange={(e) => setCommentaire(e.target.value)}
                          placeholder="Ce qui va, ce qui manque…"
                          required
                        />
                      </div>
                      <Info>
                        Une fois validée, la note est <strong>définitive</strong> et ne pourra plus
                        être modifiée.
                      </Info>
                      <button disabled={envoiEnCours}>
                        {envoiEnCours ? 'Envoi…' : 'Rendre ma relecture'}
                      </button>{' '}
                      <button type="button" className="secondaire" onClick={() => setOuverte(null)}>
                        Annuler
                      </button>
                    </form>
                  ) : (
                    <button onClick={() => setOuverte(r.relectureId)}>Noter cet exercice</button>
                  )}
                </article>
              ))
            )}
          </section>

          <section className="carte">
            <h3>Déjà rendues ({rendues.length})</h3>
            {rendues.length === 0 ? (
              <Vide>Vous n&apos;avez encore rendu aucune relecture.</Vide>
            ) : (
              <div className="table-enveloppe">
                <table>
                  <thead>
                    <tr>
                      <th>Séance</th>
                      <th>Auteur</th>
                      <th className="nombre">Note</th>
                      <th>Commentaire</th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rendues.map((r) => (
                      <tr key={r.relectureId}>
                        <td>{r.sessionTitre}</td>
                        <td>{r.auteurNom}</td>
                        <td className="nombre">
                          <strong>{r.note} / 20</strong>
                        </td>
                        <td>{r.commentaire}</td>
                        <td>
                          <StatutRelectureEtiquette statut={r.statut} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </>
  );
}
