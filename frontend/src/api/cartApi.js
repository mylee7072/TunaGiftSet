import { apiClient } from "./apiClient";

export const cartApi = {
  getCart: () => apiClient.get("/api/cart"),
  addItem: (productId, quantity) => apiClient.post("/api/cart/items", { productId, quantity }),
  updateQuantity: (cartItemId, quantity) => apiClient.patch(`/api/cart/items/${cartItemId}`, { quantity }),
  deleteItem: (cartItemId) => apiClient.delete(`/api/cart/items/${cartItemId}`),
  deleteItems: (cartItemIds) => apiClient.delete("/api/cart/items", { cartItemIds }),
};
