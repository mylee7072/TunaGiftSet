import { apiClient } from "./apiClient";

const ADDRESS_PATH = "/api/members/me/addresses";

export const addressApi = {
  findAddresses: () => apiClient.get(ADDRESS_PATH),
  createAddress: (payload) => apiClient.post(ADDRESS_PATH, payload),
  updateAddress: (addressId, payload) => apiClient.put(`${ADDRESS_PATH}/${addressId}`, payload),
  deleteAddress: (addressId) => apiClient.delete(`${ADDRESS_PATH}/${addressId}`),
  setDefaultAddress: (addressId) => apiClient.patch(`${ADDRESS_PATH}/${addressId}/default`, {}),
};
