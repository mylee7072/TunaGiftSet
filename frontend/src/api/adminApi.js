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

export const adminDashboardApi = {
  getSummary: () => apiClient.get("/api/admin/dashboard/summary"),
};

export const adminOrderApi = {
  findOrders: ({
    orderNumber,
    memberEmail,
    recipientName,
    recipientPhone,
    orderStatus,
    paymentStatus,
    deliveryStatus,
    startDate,
    endDate,
    page = 0,
    size = 20,
  } = {}) =>
    apiClient.get(
      `/api/admin/orders${buildQuery({
        orderNumber,
        memberEmail,
        recipientName,
        recipientPhone,
        orderStatus,
        paymentStatus,
        deliveryStatus,
        startDate,
        endDate,
        page,
        size,
      })}`
    ),
  findOrder: (orderNumber) => apiClient.get(`/api/admin/orders/${orderNumber}`),
  prepare: (orderNumber) => apiClient.post(`/api/admin/orders/${orderNumber}/prepare`, {}),
  registerDelivery: (orderNumber, payload) => apiClient.put(`/api/admin/orders/${orderNumber}/delivery`, payload),
  ship: (orderNumber) => apiClient.post(`/api/admin/orders/${orderNumber}/ship`, {}),
  deliver: (orderNumber) => apiClient.post(`/api/admin/orders/${orderNumber}/deliver`, {}),
  cancel: (orderNumber, reason) => apiClient.post(`/api/admin/orders/${orderNumber}/cancel`, reason ? { reason } : {}),
};
