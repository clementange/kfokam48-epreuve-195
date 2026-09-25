'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

/** Les trois écrans imposés par F2, plus l'accueil. */
const ECRANS = [
  { href: '/', libelle: 'Accueil' },
  { href: '/formateur', libelle: 'Formateur' },
  { href: '/etudiant', libelle: 'Étudiant' },
  { href: '/relecteur', libelle: 'Relecteur' },
];

export function Navigation() {
  const chemin = usePathname();
  return (
    <nav className="nav">
      {ECRANS.map(({ href, libelle }) => (
        <Link key={href} href={href} aria-current={chemin === href ? 'page' : undefined}>
          {libelle}
        </Link>
      ))}
    </nav>
  );
}
