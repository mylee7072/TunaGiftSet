import { apiClient } from "./apiClient";

export const orderApi = {
  createOrder: (payload) => apiClient.post("/api/orders", payload),
  findOrders: (page = 0, size = 10) => apiClient.get(`/api/orders?page=${page}&size=${size}`),
  findOrder: (orderNumber) => apiClient.get(`/api/orders/${orderNumber}`),
  cancelOrder: (orderNumber) => apiClient.post(`/api/orders/${orderNumber}/cancel`, {}),
};

export const paymentApi = {
  confirm: (payload) => apiClient.post("/api/payments/confirm", payload),
};
