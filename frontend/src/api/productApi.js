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

export const productApi = {
  // Only forwards query params the backend actually supports (see ProductController).
  findProducts: ({ keyword, categoryId, brandId, featured, sort, page = 0, size = 20 } = {}) =>
    apiClient.get(`/api/products${buildQuery({ keyword, categoryId, brandId, featured, sort, page, size })}`),
  findProduct: (productId) => apiClient.get(`/api/products/${productId}`),
};

export const categoryApi = {
  findActiveCategories: () => apiClient.get("/api/categories"),
};

export const brandApi = {
  findActiveBrands: () => apiClient.get("/api/brands"),
};

// ---------------------------------------------------------------- admin

export const adminProductApi = {
  // Admin listing/detail include every ProductStatus (HIDDEN/DISCONTINUED too), unlike the
  // storefront endpoints above which only ever show ACTIVE/SOLD_OUT products.
  findProducts: ({ keyword, categoryId, brandId, status, featured, sort, page = 0, size = 20 } = {}) =>
    apiClient.get(`/api/admin/products${buildQuery({ keyword, categoryId, brandId, status, featured, sort, page, size })}`),
  findProduct: (productId) => apiClient.get(`/api/admin/products/${productId}`),
  createProduct: (payload) => apiClient.post("/api/admin/products", payload),
  updateProduct: (productId, payload) => apiClient.put(`/api/admin/products/${productId}`, payload),
  changeStatus: (productId, status) => apiClient.patch(`/api/admin/products/${productId}/status`, { status }),
};

export const adminProductImageApi = {
  findImages: (productId) => apiClient.get(`/api/admin/products/${productId}/images`),
  upload: (productId, file, imageType, displayOrder) => {
    const formData = new FormData();
    formData.append("file", file);
    if (imageType) formData.append("imageType", imageType);
    if (displayOrder !== undefined && displayOrder !== null) formData.append("displayOrder", displayOrder);
    return apiClient.post(`/api/admin/products/${productId}/images`, formData);
  },
  setMain: (productId, imageId) => apiClient.patch(`/api/admin/products/${productId}/images/${imageId}/main`, {}),
  reorder: (productId, imageIds) => apiClient.put(`/api/admin/products/${productId}/images/order`, { imageIds }),
  delete: (productId, imageId) => apiClient.delete(`/api/admin/products/${productId}/images/${imageId}`),
};

export const adminInventoryApi = {
  getInventory: (productId) => apiClient.get(`/api/admin/products/${productId}/inventory`),
  adjust: (productId, payload) => apiClient.post(`/api/admin/products/${productId}/inventory/adjust`, payload),
  getHistory: (productId, page = 0, size = 20) =>
    apiClient.get(`/api/admin/products/${productId}/inventory/history?page=${page}&size=${size}`),
};

export const adminCategoryApi = {
  findAllCategories: () => apiClient.get("/api/admin/categories"),
  createCategory: (payload) => apiClient.post("/api/admin/categories", payload),
  updateCategory: (categoryId, payload) => apiClient.put(`/api/admin/categories/${categoryId}`, payload),
};

export const adminBrandApi = {
  findAllBrands: () => apiClient.get("/api/admin/brands"),
  createBrand: (payload) => apiClient.post("/api/admin/brands", payload),
  updateBrand: (brandId, payload) => apiClient.put(`/api/admin/brands/${brandId}`, payload),
};
