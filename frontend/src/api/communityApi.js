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

export const reviewApi = {
  findProductReviews: (productId, { page = 0, size = 10, sort = "LATEST" } = {}) =>
    apiClient.get(`/api/products/${productId}/reviews${buildQuery({ page, size, sort })}`),
  createReview: (productId, payload) => apiClient.post(`/api/products/${productId}/reviews`, payload),
  updateReview: (reviewId, payload) => apiClient.put(`/api/reviews/${reviewId}`, payload),
  deleteReview: (reviewId) => apiClient.delete(`/api/reviews/${reviewId}`),
  findMyReviews: (page = 0, size = 10) => apiClient.get(`/api/members/me/reviews${buildQuery({ page, size })}`),
};

export const questionApi = {
  findProductQuestions: (productId, { page = 0, size = 10 } = {}) =>
    apiClient.get(`/api/products/${productId}/questions${buildQuery({ page, size })}`),
  createQuestion: (productId, payload) => apiClient.post(`/api/products/${productId}/questions`, payload),
  updateQuestion: (questionId, payload) => apiClient.put(`/api/questions/${questionId}`, payload),
  deleteQuestion: (questionId) => apiClient.delete(`/api/questions/${questionId}`),
  findMyQuestions: (page = 0, size = 10) => apiClient.get(`/api/members/me/questions${buildQuery({ page, size })}`),
};
