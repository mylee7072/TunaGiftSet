import { apiClient } from "./apiClient";

export const couponApi = {
  findMyCoupons: (page = 0, size = 20) => apiClient.get(`/api/members/me/coupons?page=${page}&size=${size}`),
  preview: (payload) => apiClient.post("/api/checkout/preview", payload),
};

export const adminCouponApi = {
  findCoupons: (status, page = 0, size = 20) =>
    apiClient.get(`/api/admin/coupons?${status ? `status=${status}&` : ""}page=${page}&size=${size}`),
  createCoupon: (payload) => apiClient.post("/api/admin/coupons", payload),
  issueCoupon: (couponId, memberId) => apiClient.post(`/api/admin/coupons/${couponId}/issue`, { memberId }),
};
