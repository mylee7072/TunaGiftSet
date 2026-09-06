// Static storefront product catalog for the current testing phase. The storefront
// pages (HomePage/ProductListPage/ProductDetailPage) read from this file directly
// instead of the backend product API, which doesn't yet model composition/box-unit/
// barcode fields. Everything lives in this one module so swapping it for real API
// calls later only means changing the functions below, not every page that uses them.
//
// Product images aren't uploaded yet — until then every product resolves to
// PLACEHOLDER_IMAGE. Drop a real file at `frontend/public/images/products/{id}.jpg`
// (see imagePath below) and it starts rendering automatically, no code change needed.
export const PLACEHOLDER_IMAGE = "/placeholder-product.svg";

export const PRODUCTS = [
  {
    id: 1,
    name: "동원 튜나리챔 11호",
    categoryId: "tuna-richam",
    brandName: "동원",
    price: 139000,
    originalPrice: null,
    badge: null,
    barcode: "",
    boxUnit: "2세트/BOX",
    composition: [
      { name: "라이트스탠다드참치", weight: "135g", count: 12 },
      { name: "고추참치", weight: "90g", count: 8 },
      { name: "리챔더블라이트", weight: "200g", count: 4 },
    ],
    note: "재활용 플라스틱 고정재를 사용한 에코 패키지",
    description:
      "정성껏 고른 참치와 리챔 구성으로 명절 선물, 기업 답례, 가족 선물까지 두루 어울리는 선물세트입니다.",
    imagePath: "/images/products/1.jpg",
  },
  {
    id: 2,
    name: "동원 튜나리챔 10호",
    categoryId: "tuna-richam",
    brandName: "동원",
    price: 135000,
    originalPrice: null,
    badge: null,
    barcode: "",
    boxUnit: "3세트/BOX",
    composition: [
      { name: "라이트스탠다드참치", weight: "135g", count: 18 },
      { name: "리챔 오리지널", weight: "200g", count: 2 },
      { name: "리챔 오리지널", weight: "340g", count: 1 },
    ],
    note: "",
    description: "실속 있게 구성한 참치와 리챔 세트로, 부담 없이 전할 수 있는 선물입니다.",
    imagePath: "/images/products/2.jpg",
  },
  {
    id: 3,
    name: "동원 튜나리챔 30호",
    categoryId: "tuna-richam",
    brandName: "동원",
    price: 37940,
    originalPrice: 54200,
    badge: "BEST",
    barcode: "8801047865728",
    boxUnit: "5세트/BOX",
    composition: [
      { name: "살코기참치", weight: "90g", count: 12 },
      { name: "리챔더블라이트", weight: "120g", count: 3 },
    ],
    note: "",
    description: "가장 많이 찾는 구성으로, 합리적인 가격에 준비한 베스트 선물세트입니다.",
    imagePath: "/images/products/3.jpg",
  },
  {
    id: 4,
    name: "동원 스페셜 10호",
    categoryId: "special",
    brandName: "동원",
    price: 29880,
    originalPrice: null,
    badge: null,
    barcode: "",
    boxUnit: "",
    composition: [
      { name: "살코기참치", weight: "90g", count: 6 },
      { name: "리챔", weight: "120g", count: 2 },
      { name: "카놀라유", weight: "480ml", count: 2 },
    ],
    note: "",
    description: "참치와 리챔, 식용유를 함께 담아 실용성을 높인 스페셜 구성입니다.",
    imagePath: "/images/products/4.jpg",
  },
  {
    id: 5,
    name: "동원 스페셜 39호",
    categoryId: "special",
    brandName: "동원",
    price: 43960,
    originalPrice: 62800,
    badge: null,
    barcode: "8801047865964",
    boxUnit: "",
    composition: [
      { name: "살코기참치", weight: "135g", count: 6 },
      { name: "리챔", weight: "120g", count: 4 },
      { name: "리챔", weight: "200g", count: 2 },
      { name: "리챔", weight: "340g", count: 1 },
      { name: "건강요리유", weight: "480ml", count: 2 },
    ],
    note: "",
    description: "참치와 리챔을 다양한 용량으로 구성하고 건강요리유를 더한 프리미엄 스페셜 세트입니다.",
    imagePath: "/images/products/5.jpg",
  },
];

export function findProductById(id) {
  return PRODUCTS.find((product) => String(product.id) === String(id)) || null;
}

export function findProducts({ categoryId, keyword } = {}) {
  const normalizedKeyword = keyword?.trim().toLowerCase();
  return PRODUCTS.filter((product) => {
    if (categoryId && product.categoryId !== categoryId) return false;
    if (normalizedKeyword && !product.name.toLowerCase().includes(normalizedKeyword)) return false;
    return true;
  });
}

export function computeDiscountRate(product) {
  const original = Number(product.originalPrice);
  const price = Number(product.price);
  if (!original || original <= 0 || !Number.isFinite(price) || price >= original) {
    return 0;
  }
  return Math.floor(((original - price) / original) * 100);
}

export function getProductImageUrl(product) {
  return product.imagePath || PLACEHOLDER_IMAGE;
}
