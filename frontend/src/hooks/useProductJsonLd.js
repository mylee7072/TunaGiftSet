import { useEffect } from "react";

const SCRIPT_ID = "product-json-ld";

// Injects/removes a schema.org Product <script type="application/ld+json">
// for the current product — lets Google (and other structured-data consumers)
// show price/availability directly in search results. Removed on unmount/
// product change so a stale product's data never lingers into the next page.
export function useProductJsonLd(product) {
  useEffect(() => {
    if (!product) return undefined;

    const price = product.salePrice ?? product.price;
    const schema = {
      "@context": "https://schema.org/",
      "@type": "Product",
      name: product.name,
      description: product.shortDescription || product.description || undefined,
      image: product.thumbnailImageUrl
        ? [new URL(product.thumbnailImageUrl, window.location.origin).toString()]
        : undefined,
      sku: product.productCode || undefined,
      brand: product.brandDisplayName ? { "@type": "Brand", name: product.brandDisplayName } : undefined,
      offers: {
        "@type": "Offer",
        priceCurrency: "KRW",
        price: price !== undefined && price !== null ? String(price) : undefined,
        availability: product.soldOut ? "https://schema.org/OutOfStock" : "https://schema.org/InStock",
        url: window.location.href,
      },
    };

    let script = document.getElementById(SCRIPT_ID);
    if (!script) {
      script = document.createElement("script");
      script.id = SCRIPT_ID;
      script.type = "application/ld+json";
      document.head.appendChild(script);
    }
    script.textContent = JSON.stringify(schema);

    return () => {
      document.getElementById(SCRIPT_ID)?.remove();
    };
  }, [product]);
}
