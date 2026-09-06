// Static storefront category list for the current testing phase — see
// src/data/products.js for the matching product catalog and the note there about
// eventually moving both to the backend category/product API.
export const PRODUCT_CATEGORIES = [
  { id: "tuna-richam", name: "튜나리챔 세트", description: "참치 + 리챔 구성" },
  { id: "special", name: "스페셜 세트", description: "참치 + 리챔 + 식용유 구성" },
];

export function findCategoryById(categoryId) {
  return PRODUCT_CATEGORIES.find((category) => category.id === categoryId) || null;
}
