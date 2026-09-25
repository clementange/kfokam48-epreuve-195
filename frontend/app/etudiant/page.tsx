'use client';

import { useCallback, useEffect, useState } from 'react';
import { api, type MonExercice, type Session } from '@/lib/api';
import { Chargement, Erreur, Info, Succes, Vide } from '@/components/Etat';
import { Moyenne, StatutExerciceEtiquette, StatutSessionEtiquette } from '@/components/Etiquette';
import { SelecteurPromotion } from '@/components/SelecteurPromotion';

/**
 * Écran étudiant — EF2, EF3, EF4, EF10, EF11.
 *
 * L'écran de saisie du code est le premier bloc de la page et tient dans la
 * largeur d'un téléphone (ENF1) : c'est celui qu'on utilise debout, en début de
 * séance, sur un écran de 360 px.
 */
export default function EcranEtudiant() {
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  const [code, setCode] = useState('');
  const [presenceOk, setPresenceOk] = useState<string | null>(null);
  const [presenceEnCours, setPresenceEnCours] = useState(false);
  const [erreurPresence, setErreurPresence] = useState<unknown>(null);

  const [seances, setSeances] = useState<Session[] | null>(null);
  const [seanceChoisie, setSeanceChoisie] = useState<number | ''>('');
  const [lien, setLien] = useState('');
  const [depotOk, setDepotOk] = useState<string | null>(null);
  const [depotEnCours, setDepotEnCours] = useState(false);
  const [erreurDepot, setErreurDepot] = useState<unknown>(null);

  const [mesExercices, setMesExercices] = useState<MonExercice[] | null>(null);
  const [remplaceId, setRemplaceId] = useState<number | null>(null);
  const [nouveauLien, setNouveauLien] = useState('');

  const chargerMesExercices = useCallback(async (id: number) => {
    try {
      const miens = await api.mesExercices(id);
      setErreurDepot(null);
      setMesExercices(miens);
    } catch (e) {
      setErreurDepot(e);
    }
  }, []);

  useEffect(() => {
    if (promotionId === null) return;
    let annule = false;
    api.sessions(promotionId)
      .then((s) => !annule && setSeances(s))
      .catch((e) => !annule && setErreurDepot(e));
    return () => {
      annule = true;
    };
  }, [promotionId]);

  useEffect(() => {
    if (etudiantId === null) return;
    let annule = false;
    api.mesExercices(etudiantId)
      .then((x) => {
        if (annule) return;
        setErreurDepot(null);
        setMesExercices(x);
      })
      .catch((e) => !annule && setErreurDepot(e));
    return () => {
      annule = true;
    };
  }, [etudiantId]);

  async function marquerPresence(e: React.FormEvent) {
    e.preventDefault();
    if (etudiantId === null) return;
    setErreurPresence(null);
    setPresenceOk(null);
    setPresenceEnCours(true);
    try {
      const p = await api.marquerPresence(code, etudiantId);
      setPresenceOk(`Présence enregistrée pour la séance nº ${p.sessionId}.`);
      setCode('');
    } catch (err) {
      setErreurPresence(err);
    } finally {
      setPresenceEnCours(false);
    }
  }

  async function deposer(e: React.FormEvent) {
    e.preventDefault();
    if (etudiantId === null || seanceChoisie === '') return;
    setErreurDepot(null);
    setDepotOk(null);
    setDepotEnCours(true);
    try {
      await api.deposerExercice(Number(seanceChoisie), etudiantId, lien);
      setDepotOk('Exercice déposé.');
      setLien('');
      await chargerMesExercices(etudiantId);
    } catch (err) {
      setErreurDepot(err);
    } finally {
      setDepotEnCours(false);
    }
  }

  async function remplacer(exerciceId: number) {
    if (etudiantId === null) return;
    setErreurDepot(null);
    setDepotOk(null);
    try {
      await api.remplacerLien(exerciceId, nouveauLien);
      setDepotOk('Lien remplacé.');
      setRemplaceId(null);
      setNouveauLien('');
      await chargerMesExercices(etudiantId);
    } catch (err) {
      setErreurDepot(err);
    }
  }

  const seancesOuvertes = (seances ?? []).filter((s) => s.statut === 'OUVERTE');
  const pretSeulement = etudiantId === null;

  return (
    <>
      <h2>Écran étudiant</h2>
      <p className="sous-titre">Marquer sa présence, déposer son exercice, lire sa note.</p>

      <section className="carte">
        <h3>Qui êtes-vous ?</h3>
        <p className="aide">
          Il n&apos;y a pas de mot de passe : choisissez votre nom dans la liste (Q1).
        </p>
        <SelecteurPromotion
          promotionId={promotionId}
          onPromotion={(id) => {
            setPromotionId(id);
            setEtudiantId(null);
            setSeances(null);
            setMesExercices(null);
            setSeanceChoisie('');
          }}
          etudiantId={etudiantId}
          onEtudiant={(id) => {
            setMesExercices(null);
            setEtudiantId(id);
          }}
        />
      </section>

      {pretSeulement && <Info>Choisissez votre nom ci-dessus pour accéder aux actions.</Info>}

      <section className="carte">
        <h3>Marquer ma présence</h3>
        <p className="aide">
          Saisissez le code affiché par le formateur. Il expire 15 minutes après l&apos;ouverture
          de la séance.
        </p>

        <Erreur erreur={erreurPresence} />
        {presenceOk && <Succes>{presenceOk}</Succes>}

        <form onSubmit={marquerPresence}>
          <div className="champ">
            <label htmlFor="code">Code de présence</label>
            <input
              id="code"
              className="code-saisie"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="K7M2QX"
              maxLength={8}
              autoComplete="off"
              inputMode="text"
              required
              disabled={pretSeulement}
            />
          </div>
          <button disabled={pretSeulement || presenceEnCours}>
            {presenceEnCours ? 'Envoi…' : 'Je suis présent'}
          </button>
        </form>
      </section>

      <section className="carte">
        <h3>Déposer mon exercice</h3>
        <p className="aide">
          Le dépôt reste possible <strong>après l&apos;expiration du code</strong>, jusqu&apos;à ce
          que le formateur clôture la séance (Q12).
        </p>

        <Erreur erreur={erreurDepot} />
        {depotOk && <Succes>{depotOk}</Succes>}

        <form onSubmit={deposer}>
          <div className="champ">
            <label htmlFor="seance">Séance</label>
            <select
              id="seance"
              value={seanceChoisie}
              onChange={(e) => setSeanceChoisie(e.target.value === '' ? '' : Number(e.target.value))}
              disabled={pretSeulement}
              required
            >
              <option value="">— choisissez une séance ouverte —</option>
              {seancesOuvertes.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.titre}
                </option>
              ))}
            </select>
          </div>
          <div className="champ">
            <label htmlFor="lien">Lien de l&apos;exercice</label>
            <input
              id="lien"
              type="url"
              value={lien}
              onChange={(e) => setLien(e.target.value)}
              placeholder="https://github.com/moi/tp12"
              disabled={pretSeulement}
              required
            />
          </div>
          <button disabled={pretSeulement || depotEnCours || seanceChoisie === ''}>
            {depotEnCours ? 'Dépôt…' : 'Déposer'}
          </button>
        </form>

        {seances && seancesOuvertes.length === 0 && (
          <Info>Aucune séance ouverte : toutes les séances de la promotion sont clôturées.</Info>
        )}
      </section>

      <section className="carte">
        <h3>Mes exercices et les notes reçues</h3>
        <p className="aide">
          Chaque exercice est relu par <strong>deux pairs</strong> et la note retenue est la
          moyenne des deux. Tant qu&apos;un seul a rendu, la note affichée est marquée
          « provisoire ». Vous ne voyez jamais le nom de vos relecteurs (Q8).
        </p>

        {pretSeulement ? (
          <Vide>Choisissez votre nom pour voir vos exercices.</Vide>
        ) : !mesExercices ? (
          <Chargement quoi="de vos exercices" />
        ) : mesExercices.length === 0 ? (
          <Vide>Vous n&apos;avez encore déposé aucun exercice.</Vide>
        ) : (
          <div className="table-enveloppe">
            <table>
              <thead>
                <tr>
                  <th>Séance</th>
                  <th>Lien</th>
                  <th>Statut</th>
                  <th className="nombre">Note</th>
                  <th>Commentaire</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {mesExercices.map((x) => {
                  const seance = seances?.find((s) => s.id === x.sessionId);
                  const modifiable = seance?.statut === 'OUVERTE';
                  return (
                    <tr key={x.exerciceId}>
                      <td>
                        {x.sessionTitre}
                        {seance && (
                          <>
                            <br />
                            <StatutSessionEtiquette statut={seance.statut} />
                          </>
                        )}
                      </td>
                      <td>
                        {remplaceId === x.exerciceId ? (
                          <input
                            type="url"
                            value={nouveauLien}
                            onChange={(e) => setNouveauLien(e.target.value)}
                            placeholder="https://…"
                          />
                        ) : (
                          <a href={x.lien} target="_blank" rel="noreferrer noopener">
                            {x.lien}
                          </a>
                        )}
                      </td>
                      <td>
                        <StatutExerciceEtiquette statut={x.statut} />
                      </td>
                      <td className="nombre">
                        <Moyenne valeur={x.note} provisoire={x.provisoire} />
                        {x.relecturesAttendues > 0 && (
                          <>
                            <br />
                            <small className="sans-valeur">
                              {x.relecturesRendues} relecture{x.relecturesRendues > 1 ? 's' : ''} sur{' '}
                              {x.relecturesAttendues}
                            </small>
                          </>
                        )}
                      </td>
                      <td style={{ whiteSpace: 'pre-line' }}>
                        {x.commentaire ?? <span className="sans-valeur">—</span>}
                      </td>
                      <td>
                        {modifiable &&
                          (remplaceId === x.exerciceId ? (
                            <button className="secondaire" onClick={() => remplacer(x.exerciceId)}>
                              Valider
                            </button>
                          ) : (
                            <button
                              className="secondaire"
                              onClick={() => {
                                setRemplaceId(x.exerciceId);
                                setNouveauLien(x.lien);
                              }}
                            >
                              Remplacer
                            </button>
                          ))}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
