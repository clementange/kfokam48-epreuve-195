# =============================================================================
# Frontend — image en trois etapes.
#
# `output: standalone` dans next.config.ts fait que Next produit un serveur
# autonome avec les seules dependances reellement utilisees. L'image finale ne
# contient donc pas les 277 paquets de node_modules, seulement ce qui tourne.
# =============================================================================

# --- Etape 1 : dependances ---------------------------------------------------
FROM node:22-alpine AS dependances
WORKDIR /app
# `npm ci` et non `npm install` : il installe exactement ce que decrit le
# package-lock.json, sans jamais le modifier. Une construction reproductible.
COPY package.json package-lock.json ./
RUN npm ci

# --- Etape 2 : construction --------------------------------------------------
FROM node:22-alpine AS construction
WORKDIR /app
COPY --from=dependances /app/node_modules ./node_modules
COPY . .

# L'adresse de l'API est figee dans le bundle au moment du build : Next remplace
# les NEXT_PUBLIC_* a la compilation, pas a l'execution. Elle doit donc etre
# celle que le NAVIGATEUR utilisera — localhost:8080 depuis la machine du
# correcteur, et non le nom de service Docker, inconnu hors du reseau interne.
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# --- Etape 3 : execution -----------------------------------------------------
FROM node:22-alpine
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 kfokam && adduser --system --uid 1001 kfokam

COPY --from=construction --chown=kfokam:kfokam /app/.next/standalone ./
COPY --from=construction --chown=kfokam:kfokam /app/.next/static ./.next/static
COPY --from=construction --chown=kfokam:kfokam /app/public ./public

USER kfokam
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

CMD ["node", "server.js"]
