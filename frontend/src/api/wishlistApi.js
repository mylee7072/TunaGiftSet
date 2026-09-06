import { apiClient } from "./apiClient";

function buildQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, value);
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export const wishlistApi = {
  add: (productId) => apiClient.post(`/api/products/${productId}/wishlist`),
  remove: (productId) => apiClient.delete(`/api/products/${productId}/wishlist`),
  findMyWishlist: ({ page = 0, size = 20 } = {}) =>
    apiClient.get(`/api/members/me/wishlist${buildQuery({ page, size })}`),
  findStatus: (productId) => apiClient.get(`/api/members/me/wishlist/products/${productId}/status`),
  findWishlistedProductIds: (productIds) =>
    apiClient.post("/api/members/me/wishlist/product-ids", { productIds }),
};
