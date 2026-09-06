import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProductCard } from "./ProductCard";

function renderCard(product) {
  render(
    <MemoryRouter>
      <ProductCard product={product} />
    </MemoryRouter>
  );
}

describe("ProductCard", () => {
  it("renders product data with Korean price formatting", () => {
    renderCard({
      id: 1,
      name: "프리미엄 선물세트",
      brandDisplayName: "샘플 브랜드",
      thumbnailImageUrl: "/sample.png",
      originalPrice: 50000,
      salePrice: 45000,
      stockQuantity: 10,
      status: "ACTIVE",
      featured: true,
    });

    expect(screen.getByText("샘플 브랜드")).toBeInTheDocument();
    expect(screen.getByText("프리미엄 선물세트")).toBeInTheDocument();
    expect(screen.getByText("45,000원")).toBeInTheDocument();
    expect(screen.getByText("추천")).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", "/products/1");
  });

  it("shows sold out state before featured badge", () => {
    renderCard({
      id: 2,
      name: "품절 상품",
      salePrice: 12000,
      stockQuantity: 0,
      status: "SOLD_OUT",
      featured: true,
    });

    expect(screen.getByText("품절")).toBeInTheDocument();
    expect(screen.queryByText("추천")).not.toBeInTheDocument();
  });
});
