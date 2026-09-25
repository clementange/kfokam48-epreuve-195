import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  /**
   * Produit un serveur autonome dans `.next/standalone`, avec les seules
   * dépendances réellement utilisées. C'est ce qui permet à l'image Docker
   * finale de ne pas transporter l'intégralité de `node_modules`.
   */
  output: 'standalone',
};

export default nextConfig;
