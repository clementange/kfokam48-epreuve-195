import Link from 'next/link';

export default function Accueil() {
  return (
    <>
      <h2>Trois écrans, trois rôles</h2>
      <p className="sous-titre">
        Il n&apos;y a pas de mot de passe : chacun se désigne dans une liste. C&apos;est une
        simplification assumée du sujet, documentée au §3 du cahier des charges.
      </p>

      <div className="grille">
        <article className="carte">
          <h3>Formateur</h3>
          <p className="aide">
            Ouvrir une séance et obtenir son code, la clôturer, et lire le tableau
            récapitulatif de la promotion.
          </p>
          <Link href="/formateur">
            <button>Ouvrir l&apos;écran formateur</button>
          </Link>
        </article>

        <article className="carte">
          <h3>Étudiant</h3>
          <p className="aide">
            Marquer sa présence avec le code, déposer le lien de son exercice, et
            consulter la note reçue.
          </p>
          <Link href="/etudiant">
            <button>Ouvrir l&apos;écran étudiant</button>
          </Link>
        </article>

        <article className="carte">
          <h3>Relecteur</h3>
          <p className="aide">
            Relire l&apos;exercice d&apos;un pair désigné au hasard, et lui attribuer une note
            sur 20 accompagnée d&apos;un commentaire.
          </p>
          <Link href="/relecteur">
            <button>Ouvrir l&apos;écran relecteur</button>
          </Link>
        </article>
      </div>
    </>
  );
}
