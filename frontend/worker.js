// Minimal same-origin API proxy: the browser only ever talks to this Worker's own
// origin, so no CORS setup is needed on the backend for this path — the proxying
// fetch() below happens server-side (Worker-to-tunnel), which browsers' CORS
// rules never apply to. Everything that isn't /api/* falls through to the static
// assets binding (see wrangler.json's assets.binding), which also preserves the
// SPA fallback (not_found_handling) for client-side routes like /products.
export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname.startsWith("/api/")) {
      if (!env.API_ORIGIN) {
        return new Response("API_ORIGIN is not configured.", { status: 502 });
      }

      const target = new URL(url.pathname + url.search, env.API_ORIGIN);

      // Drop the incoming Host header (it's this Worker's own domain) so fetch()
      // sets the correct one for the proxied origin instead of forwarding ours.
      const headers = new Headers(request.headers);
      headers.delete("host");

      const proxyRequest = new Request(target.toString(), {
        method: request.method,
        headers,
        body: request.body,
      });

      return fetch(proxyRequest);
    }

    return env.ASSETS.fetch(request);
  },
};
