import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { paymentApi } from "../api/orderApi";
import { PaymentSuccessPage } from "./PaymentSuccessPage";

vi.mock("../api/orderApi", () => ({
  paymentApi: {
    confirm: vi.fn(),
  },
}));

function renderPage(entry) {
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/payment/success" element={<PaymentSuccessPage />} />
        <Route path="/payment/fail" element={<div>결제 실패 화면</div>} />
        <Route path="/order/complete/:orderNumber" element={<div>주문 완료 화면</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("PaymentSuccessPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("does not show completion before backend confirm succeeds", () => {
    paymentApi.confirm.mockReturnValue(new Promise(() => {}));
    renderPage("/payment/success?paymentKey=pay-key&orderId=ORDER-1&amount=45000");

    expect(screen.getByText("결제를 확인하고 있습니다...")).toBeInTheDocument();
    expect(screen.queryByText("주문 완료 화면")).not.toBeInTheDocument();
  });

  it("confirms payment once and navigates to order complete on success", async () => {
    paymentApi.confirm.mockResolvedValue({ orderNumber: "ORDER-1" });
    renderPage("/payment/success?paymentKey=pay-key&orderId=ORDER-1&amount=45000");

    await waitFor(() => expect(screen.getByText("주문 완료 화면")).toBeInTheDocument());
    expect(paymentApi.confirm).toHaveBeenCalledTimes(1);
    expect(paymentApi.confirm).toHaveBeenCalledWith({
      paymentKey: "pay-key",
      orderId: "ORDER-1",
      amount: 45000,
    });
  });

  it("routes invalid direct access to fail page without confirm", async () => {
    renderPage("/payment/success?orderId=ORDER-1&amount=45000");

    await waitFor(() => expect(screen.getByText("결제 실패 화면")).toBeInTheDocument());
    expect(paymentApi.confirm).not.toHaveBeenCalled();
  });
});
