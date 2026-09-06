import { useEffect } from "react";

const DEFAULT_OG_IMAGE = "https://tunagiftset.mylee7072.workers.dev/og-image.png";

function setMetaTag(attr, key, content) {
  if (!content) return;
  let el = document.querySelector(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement("meta");
    el.setAttribute(attr, key);
    document.head.appendChild(el);
  }
  el.setAttribute("content", content);
}

// Sets document.title plus the description/Open Graph meta tags shareable
// pages need (Kakao/Facebook link previews read og:*, not the <title> tag).
// index.html carries the site-wide defaults — this only overrides them for the
// current page, and never has to clean up: the next page's call just
// overwrites the same tags again.
export function useDocumentMeta({ title, description, image }) {
  useEffect(() => {
    if (title) document.title = title;
    setMetaTag("name", "description", description);
    setMetaTag("property", "og:title", title);
    setMetaTag("property", "og:description", description);
    setMetaTag("property", "og:image", image || DEFAULT_OG_IMAGE);
    setMetaTag("property", "og:url", window.location.href);
  }, [title, description, image]);
}
