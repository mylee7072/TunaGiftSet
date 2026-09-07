# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1: production build. VITE_* build args are inlined into the JS bundle by
# Vite at build time — they are NOT runtime-configurable once built (see README).
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Dependencies first (cached layer) so a source-only change doesn't reinstall everything.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# VITE_TOSS_CLIENT_KEY is Toss's public client key — safe to bake into a browser bundle,
# unlike the Toss *secret* key, which stays backend-only and is never passed here.
# Left empty by default; the Toss widget code already reports a clear runtime error if a
# checkout page loads without one (see src/api/tossPayments.js) rather than failing silently.
ARG VITE_TOSS_CLIENT_KEY=""
ENV VITE_TOSS_CLIENT_KEY=$VITE_TOSS_CLIENT_KEY

# Kakao/Google OAuth client IDs — public values, safe to bake in. Empty by default,
# which leaves that social-login button hidden (see src/utils/oauth.js).
ARG VITE_KAKAO_CLIENT_ID=""
ENV VITE_KAKAO_CLIENT_ID=$VITE_KAKAO_CLIENT_ID
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

# ---------------------------------------------------------------------------
# Stage 2: serve the static build with nginx, running fully as a non-root user.
# ---------------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
