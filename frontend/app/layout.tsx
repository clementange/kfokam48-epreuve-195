import type { Metadata } from 'next';
import './globals.css';
import { Navigation } from '@/components/Navigation';

export const metadata: Metadata = {
  title: 'Présence & Relecture KFOKAM48',
  description:
    'Suivi de présence et relecture par les pairs pour la formation KFOKAM48.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body>
        <header className="entete">
          <div className="entete-contenu">
            <h1>Présence &amp; Relecture KFOKAM48</h1>
            <Navigation />
          </div>
        </header>
        <main className="page">{children}</main>
      </body>
    </html>
  );
}
