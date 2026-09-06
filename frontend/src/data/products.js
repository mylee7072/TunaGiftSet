export const PLACEHOLDER_IMAGE = "/placeholder-product.svg";

export const PRODUCTS = [
  {
    id: 1,
    productCode: "GIFT-TUNA-001",
    name: "프리미엄 참치 선물세트 1호",
    brandId: 1,
    brandDisplayName: "세영기프트",
    categoryId: "tuna-richam",
    categoryName: "튜나리챔 세트",
    thumbnailImageUrl: "/images/products/1.jpg",
    originalPrice: 52000,
    salePrice: 46800,
    stockQuantity: 18,
    status: "ACTIVE",
    featured: true,
    averageRating: 0,
    reviewCount: 0,
    wishlistCount: 0,
    shortDescription: "참치와 리챔을 함께 담은 실속형 명절 선물 구성입니다.",
    description:
      "부담 없는 가격대와 익숙한 구성으로 명절 선물, 거래처 답례, 가족 선물에 두루 어울리는 선물세트입니다.",
  },
  {
    id: 2,
    productCode: "GIFT-TUNA-002",
    name: "실속 튜나리챔 선물세트 2호",
    brandId: 1,
    brandDisplayName: "세영기프트",
    categoryId: "tuna-richam",
    categoryName: "튜나리챔 세트",
    thumbnailImageUrl: "/images/products/2.jpg",
    originalPrice: null,
    salePrice: 39800,
    stockQuantity: 24,
    status: "ACTIVE",
    featured: false,
    averageRating: 0,
    reviewCount: 0,
    wishlistCount: 0,
    shortDescription: "여러 곳에 나누어 선물하기 좋은 기본형 구성입니다.",
    description:
      "참치캔과 리챔을 균형 있게 구성해 일상에서 활용도가 높은 상품입니다. 단체 선물이나 가족 선물로 선택하기 좋습니다.",
  },
  {
    id: 3,
    productCode: "GIFT-SPECIAL-003",
    name: "참치 리챔 혼합 선물세트 3호",
    brandId: 1,
    brandDisplayName: "세영기프트",
    categoryId: "special",
    categoryName: "스페셜 세트",
    thumbnailImageUrl: "/images/products/3.jpg",
    originalPrice: 68000,
    salePrice: 57800,
    stockQuantity: 12,
    status: "ACTIVE",
    featured: true,
    averageRating: 0,
    reviewCount: 0,
    wishlistCount: 0,
    shortDescription: "참치, 리챔, 식용유를 함께 담은 풍성한 혼합 구성입니다.",
    description:
      "선물 받는 분의 활용도를 고려해 다양한 식품을 함께 담았습니다. 정갈한 패키지로 명절과 감사 선물에 어울립니다.",
  },
  {
    id: 4,
    productCode: "GIFT-SPECIAL-004",
    name: "스페셜 감사 선물세트 4호",
    brandId: 1,
    brandDisplayName: "세영기프트",
    categoryId: "special",
    categoryName: "스페셜 세트",
    thumbnailImageUrl: "/images/products/4.jpg",
    originalPrice: null,
    salePrice: 32900,
    stockQuantity: 0,
    status: "SOLD_OUT",
    featured: false,
    averageRating: 0,
    reviewCount: 0,
    wishlistCount: 0,
    shortDescription: "가벼운 감사 인사에 알맞은 소형 선물세트입니다.",
    description:
      "부담 없는 구성으로 지인, 동료, 이웃에게 마음을 전하기 좋은 상품입니다. 현재는 준비된 수량이 모두 소진되었습니다.",
  },
  {
    id: 5,
    productCode: "GIFT-TUNA-005",
    name: "프리미엄 명절 선물세트 5호",
    brandId: 1,
    brandDisplayName: "세영기프트",
    categoryId: "tuna-richam",
    categoryName: "튜나리챔 세트",
    thumbnailImageUrl: "/images/products/5.jpg",
    originalPrice: 89000,
    salePrice: 75600,
    stockQuantity: 9,
    status: "ACTIVE",
    featured: true,
    averageRating: 0,
    reviewCount: 0,
    wishlistCount: 0,
    shortDescription: "조금 더 격식을 갖춘 선물이 필요할 때 선택하기 좋은 구성입니다.",
    description:
      "가격대와 구성이 안정적인 프리미엄 선물세트입니다. 가족 선물은 물론 기업 답례품으로도 사용하기 좋습니다.",
  },
];

export const PRODUCT_CATEGORIES = [
  { id: "tuna-richam", name: "튜나리챔 세트", description: "참치와 리챔 중심의 실속 선물세트" },
  { id: "special", name: "스페셜 세트", description: "참치, 리챔, 식용유 등을 함께 담은 혼합 구성" },
];

export const PRODUCT_BRANDS = [
  { id: 1, name: "seyoung-gift", displayName: "세영기프트" },
];

function normalizeText(value) {
  return String(value || "").trim().toLowerCase();
}

export function computeDiscountRate(product) {
  const original = Number(product.originalPrice);
  const sale = Number(product.salePrice);
  if (!original || original <= 0 || !Number.isFinite(sale) || sale >= original) {
    return 0;
  }
  return Math.floor(((original - sale) / original) * 100);
}

function toProductResponse(product) {
  return {
    ...product,
    price: product.salePrice,
    discountRate: computeDiscountRate(product),
    soldOut: product.status === "SOLD_OUT" || product.stockQuantity === 0,
    images: [
      {
        id: `${product.id}-main`,
        imageUrl: product.thumbnailImageUrl || PLACEHOLDER_IMAGE,
        imageType: "MAIN",
        displayOrder: 0,
      },
    ],
  };
}

export async function fetchProductPage({ categoryId, brandId, featured, keyword, sort = "LATEST", page = 0, size = 12 } = {}) {
  const normalizedKeyword = normalizeText(keyword);
  let items = PRODUCTS.map(toProductResponse).filter((product) => {
    if (categoryId && String(product.categoryId) !== String(categoryId)) return false;
    if (brandId && String(product.brandId) !== String(brandId)) return false;
    if (featured !== undefined && featured !== "" && product.featured !== (featured === true || featured === "true")) return false;
    if (normalizedKeyword) {
      const searchable = normalizeText(`${product.name} ${product.shortDescription} ${product.categoryName} ${product.brandDisplayName}`);
      if (!searchable.includes(normalizedKeyword)) return false;
    }
    return true;
  });

  if (sort === "PRICE_ASC") {
    items = [...items].sort((a, b) => Number(a.salePrice) - Number(b.salePrice));
  } else if (sort === "PRICE_DESC") {
    items = [...items].sort((a, b) => Number(b.salePrice) - Number(a.salePrice));
  } else {
    items = [...items].sort((a, b) => Number(b.id) - Number(a.id));
  }

  const safePage = Math.max(0, Number(page) || 0);
  const safeSize = Math.max(1, Number(size) || 12);
  const start = safePage * safeSize;
  const content = items.slice(start, start + safeSize);
  const totalPages = Math.max(1, Math.ceil(items.length / safeSize));

  return {
    content,
    page: safePage,
    size: safeSize,
    totalElements: items.length,
    totalPages,
    first: safePage === 0,
    last: safePage >= totalPages - 1,
  };
}

export async function fetchProductById(productId) {
  const product = PRODUCTS.find((item) => String(item.id) === String(productId));
  return product ? toProductResponse(product) : null;
}

export async function fetchProductCategories() {
  return PRODUCT_CATEGORIES;
}

export async function fetchProductBrands() {
  return PRODUCT_BRANDS;
}

export function findCategoryById(categoryId) {
  return PRODUCT_CATEGORIES.find((category) => String(category.id) === String(categoryId)) || null;
}

export function getProductImageUrl(product) {
  return product?.thumbnailImageUrl || product?.images?.[0]?.imageUrl || PLACEHOLDER_IMAGE;
}
