import { describe, expect, it } from "vitest";
import { formatOrderStatus, formatPaymentMethod, formatPaymentStatus, formatPrice, isCancelableOrderStatus } from "./format";

describe("format utilities", () => {
  it("formats Korean won amounts", () => {
    expect(formatPrice(45000)).toBe("45,000원");
    expect(formatPrice(null)).toBe("0원");
  });

  it("maps backend enum values to customer-facing Korean labels", () => {
    expect(formatOrderStatus("PAYMENT_PENDING")).toBe("결제대기");
    expect(formatPaymentStatus("PAID")).toBe("결제완료");
    expect(formatPaymentMethod("CARD")).toBe("카드");
  });

  it("keeps cancelability aligned with customer-cancelable backend states", () => {
    expect(isCancelableOrderStatus("PAYMENT_PENDING")).toBe(true);
    expect(isCancelableOrderStatus("PAID")).toBe(true);
    expect(isCancelableOrderStatus("SHIPPING")).toBe(false);
  });
});
