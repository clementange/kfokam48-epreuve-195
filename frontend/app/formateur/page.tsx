'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  api,
  type Cloture,
  type Etudiant,
  type LigneTableau,
  type Session,
  type SessionOuverte,
} from '@/lib/api';
import { Chargement, Erreur, Info, Succes, Vide } from '@/components/Etat';
import { Moyenne, StatutSessionEtiquette } from '@/components/Etiquette';
import { SelecteurPromotion } from '@/components/SelecteurPromotion';

/**
 * Écran formateur — EF1, EF5, EF6, EF8.
 *
 * Aucun `fetch` ici : tout passe par `lib/api` (F3). Aucun calcul métier non
 * plus — la moyenne du tableau arrive déjà calculée et arrondie par l'API, et
 * n'est pas recalculée.
 */
export default function EcranFormateur() {
  const [promotionId, setPromotionId] = useState<number | null>(null);

  const [titre, setTitre] = useState('');
  const [ouverture, setOuverture] = useState<SessionOuverte | null>(null);
  const [ouvertureEnCours, setOuvertureEnCours] = useState(false);

  const [seances, setSeances] = useState<Session[] | null>(null);
  const [tableau, setTableau] = useState<LigneTableau[] | null>(null);
  const [cloture, setCloture] = useState<Cloture | null>(null);
  const [clotureEnCours, setClotureEnCours] = useState<number | null>(null);

  const [erreur, setErreur] = useState<unknown>(null);

  // EF9, Q14 — ajout manuel d'une présence.
  const [etudiants, setEtudiants] = useState<Etudiant[] | null>(null);
  const [seanceManuelle, setSeanceManuelle] = useState<number | ''>('');
  const [etudiantManuel, setEtudiantManuel] = useState<number | ''>('');
  const [ajoutEnCours, setAjoutEnCours] = useState(false);
  const [ajoutOk, setAjoutOk] = useState<string | null>(null);

  const recharger = useCallback(async (id: number) => {
    // Aucun setState avant le premier await : appelée depuis un effet, une
    // écriture synchrone provoquerait un rendu en cascade. L'erreur est donc
    // effacée au succès, pas en préalable.
    try {
      const [s, t] = await Promise.all([api.sessions(id), api.tableau(id)]);
      setErreur(null);
      setSeances(s);
      setTableau(t);
    } catch (e) {
      setErreur(e);
    }
  }, []);

  useEffect(() => {
    if (promotionId === null) return;
    // Les setState vivent dans des callbacks, jamais dans le corps synchrone de
    // l'effet. Le drapeau évite d'écrire l'état d'une promotion qu'on a quittée
    // avant la fin de la requête.
    let annule = false;
    Promise.all([api.sessions(promotionId), api.tableau(promotionId)])
      .then(([s, t]) => {
        if (annule) return;
        setErreur(null);
        setSeances(s);
        setTableau(t);
      })
      .catch((e) => !annule && setErreur(e));
    return () => {
      annule = true;
    };
  }, [promotionId]);

  useEffect(() => {
    if (promotionId === null) return;
    let annule = false;
    api.etudiants(promotionId)
      .then((e) => !annule && setEtudiants(e))
      .catch((e) => !annule && setErreur(e));
    return () => {
      annule = true;
    };
  }, [promotionId]);

  async function ouvrirSeance(e: React.FormEvent) {
    e.preventDefault();
    if (promotionId === null) return;
    setErreur(null);
    setCloture(null);
    setOuvertureEnCours(true);
    try {
      setOuverture(await api.ouvrirSession(titre, promotionId));
      setTitre('');
      await recharger(promotionId);
    } catch (err) {
      setErreur(err);
    } finally {
      setOuvertureEnCours(false);
    }
  }

  async function cloturerSeance(id: number) {
    if (promotionId === null) return;
    setErreur(null);
    setOuverture(null);
    setClotureEnCours(id);
    try {
      setCloture(await api.cloturerSession(id));
      await recharger(promotionId);
    } catch (err) {
      setErreur(err);
    } finally {
      setClotureEnCours(null);
    }
  }

  async function ajouterPresence(e: React.FormEvent) {
    e.preventDefault();
    if (promotionId === null || seanceManuelle === '' || etudiantManuel === '') return;
    setErreur(null);
    setAjoutOk(null);
    setAjoutEnCours(true);
    try {
      await api.ajouterPresenceManuelle(Number(seanceManuelle), Number(etudiantManuel));
      const nom = etudiants?.find((s) => s.id === Number(etudiantManuel))?.nom ?? 'L’étudiant';
      setAjoutOk(`${nom} est marqué présent, avec la mention « ajouté par le formateur ».`);
      setEtudiantManuel('');
      await recharger(promotionId);
    } catch (err) {
      setErreur(err);
    } finally {
      setAjoutEnCours(false);
    }
  }

  const heure = (iso: string) =>
    new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

  return (
    <>
      <h2>Écran formateur</h2>
      <p className="sous-titre">Ouvrir une séance, la clôturer, et suivre la promotion.</p>

      <Erreur erreur={erreur} />

      <section className="carte">
        <h3>Promotion suivie</h3>
        <SelecteurPromotion
          promotionId={promotionId}
          onPromotion={(id) => {
            // Les remises à zéro sont faites ici, dans le gestionnaire, et non
            // dans l'effet : un setState synchrone dans un effet déclenche un
            // rendu en cascade (react-hooks/set-state-in-effect).
            setSeances(null);
            setTableau(null);
            setOuverture(null);
            setCloture(null);
            setPromotionId(id);
          }}
        />
      </section>

      <div className="grille">
        <section className="carte">
          <h3>Ouvrir une séance</h3>
          <p className="aide">
            Le code est valable <strong>15 minutes</strong> à compter de l&apos;ouverture (RG1).
            Passé ce délai, la séance reste ouverte : seul le code cesse de fonctionner.
          </p>

          <form onSubmit={ouvrirSeance}>
            <div className="champ">
              <label htmlFor="titre">Titre de la séance</label>
              <input
                id="titre"
                value={titre}
                onChange={(e) => setTitre(e.target.value)}
                placeholder="Séance 12 — Spring Data JPA"
                required
                maxLength={200}
              />
            </div>
            <button disabled={ouvertureEnCours || promotionId === null}>
              {ouvertureEnCours ? 'Ouverture…' : 'Ouvrir la séance'}
            </button>
          </form>

          {ouverture && (
            <>
              <p className="code-affiche">{ouverture.code}</p>
              <Succes>
                Séance ouverte à {heure(ouverture.ouvertureAt)}. Le code expire à{' '}
                <strong>{heure(ouverture.expirationAt)}</strong>.
              </Succes>
            </>
          )}

          {cloture && (
            <Succes>
              Séance clôturée. <strong>{cloture.relecturesAssignees}</strong> relecture(s)
              assignée(s).
              {cloture.exercicesUnSeulRelecteur > 0 && (
                <>
                  {' '}
                  <strong>{cloture.exercicesUnSeulRelecteur}</strong> exercice(s) n&apos;ont trouvé
                  qu&apos;un seul pair disponible : ils auront une note, mais pas une moyenne.
                </>
              )}
              {cloture.exercicesNonAssignes > 0 && (
                <>
                  {' '}
                  <strong>{cloture.exercicesNonAssignes}</strong> exercice(s) sans relecteur
                  disponible : ils ne recevront pas de note.
                </>
              )}
            </Succes>
          )}
        </section>

        <section className="carte">
          <h3>Séances</h3>
          <p className="aide">
            Clôturer une séance ferme les dépôts et désigne <strong>deux relecteurs</strong> par
            exercice. C&apos;est irréversible (RG14).
          </p>

          {!seances ? (
            <Chargement quoi="des séances" />
          ) : seances.length === 0 ? (
            <Vide>Aucune séance pour cette promotion. Ouvrez la première ci-contre.</Vide>
          ) : (
            <div className="table-enveloppe">
              <table>
                <thead>
                  <tr>
                    <th>Séance</th>
                    <th>Code</th>
                    <th>Statut</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {seances.map((s) => (
                    <tr key={s.id}>
                      <td>
                        {s.titre}
                        <br />
                        <small className="sans-valeur">ouverte à {heure(s.ouvertureAt)}</small>
                      </td>
                      <td>
                        <code>{s.code}</code>
                      </td>
                      <td>
                        <StatutSessionEtiquette statut={s.statut} />
                      </td>
                      <td>
                        {s.statut === 'OUVERTE' && (
                          <button
                            className="secondaire"
                            onClick={() => cloturerSeance(s.id)}
                            disabled={clotureEnCours === s.id}
                          >
                            {clotureEnCours === s.id ? 'Clôture…' : 'Clôturer'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <section className="carte">
        <h3>Ajouter une présence à la main</h3>
        <p className="aide">
          Pour un étudiant dont le code n&apos;a pas fonctionné — un téléphone en panne, par
          exemple. La présence reste <strong>possible après l&apos;expiration du code</strong>, et
          elle est marquée « ajouté par le formateur » pour que cela se voie (Q14).
        </p>

        {ajoutOk && <Succes>{ajoutOk}</Succes>}

        <form onSubmit={ajouterPresence}>
          <div className="champ">
            <label htmlFor="seance-manuelle">Séance</label>
            <select
              id="seance-manuelle"
              value={seanceManuelle}
              onChange={(e) =>
                setSeanceManuelle(e.target.value === '' ? '' : Number(e.target.value))
              }
              required
            >
              <option value="">— choisissez une séance ouverte —</option>
              {(seances ?? [])
                .filter((s) => s.statut === 'OUVERTE')
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.titre}
                  </option>
                ))}
            </select>
          </div>
          <div className="champ">
            <label htmlFor="etudiant-manuel">Étudiant</label>
            <select
              id="etudiant-manuel"
              value={etudiantManuel}
              onChange={(e) =>
                setEtudiantManuel(e.target.value === '' ? '' : Number(e.target.value))
              }
              required
            >
              <option value="">— choisissez un étudiant —</option>
              {(etudiants ?? []).map((e) => (
                <option key={e.id} value={e.id}>
                  {e.nom}
                </option>
              ))}
            </select>
          </div>
          <button
            className="secondaire"
            disabled={ajoutEnCours || seanceManuelle === '' || etudiantManuel === ''}
          >
            {ajoutEnCours ? 'Ajout…' : 'Marquer présent'}
          </button>
        </form>
      </section>

      <section className="carte">
        <h3>Tableau récapitulatif</h3>
        <p className="aide">
          Par étudiant : sa présence, ses dépôts, la moyenne des notes reçues et les relectures
          qu&apos;il doit encore rendre (Q16). Les valeurs viennent de l&apos;API et ne sont pas
          recalculées ici.
        </p>

        {!tableau ? (
          <Chargement quoi="du tableau" />
        ) : tableau.length === 0 ? (
          <Vide>Aucun étudiant dans cette promotion.</Vide>
        ) : (
          <>
            <Info>
              Une moyenne notée « — » signifie qu&apos;aucune note n&apos;a encore été reçue :
              ce n&apos;est pas un zéro. Une moyenne marquée « provisoire » attend encore une
              seconde relecture et peut changer.
            </Info>
            <div className="table-enveloppe">
              <table>
                <thead>
                  <tr>
                    <th>Étudiant</th>
                    <th className="nombre">Présences</th>
                    <th className="nombre">Exercices déposés</th>
                    <th className="nombre">Moyenne reçue</th>
                    <th className="nombre">Relectures à rendre</th>
                  </tr>
                </thead>
                <tbody>
                  {tableau.map((l) => (
                    <tr key={l.etudiantId}>
                      <td>{l.nom}</td>
                      <td className="nombre">{l.presences}</td>
                      <td className="nombre">{l.exercicesDeposes}</td>
                      <td className="nombre">
                        <Moyenne valeur={l.moyenne} provisoire={l.moyenneProvisoire} />
                      </td>
                      <td className="nombre">
                        {l.relecturesEnAttente > 0 ? (
                          <span className="etiquette attente">{l.relecturesEnAttente}</span>
                        ) : (
                          <span className="sans-valeur">0</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </>
  );
}
