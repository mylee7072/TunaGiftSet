import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { paymentApi } from "../api/orderApi";
import { Loading } from "../components/common/Loading";
import { ApiError } from "../api/apiClient";

export function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const hasRequestedRef = useRef(false);

  useEffect(() => {
    document.title = "결제 확인 중 - SeyoungGiftSet";
    if (hasRequestedRef.current) return;
    hasRequestedRef.current = true;

    const paymentKey = searchParams.get("paymentKey");
    const orderId = searchParams.get("orderId");
    const amount = searchParams.get("amount");

    if (!paymentKey || !orderId || !amount) {
      navigate("/payment/fail", {
        replace: true,
        state: { message: "올바르지 않은 결제 접근입니다. 결제를 다시 시도해 주세요." },
      });
      return;
    }

    paymentApi
      .confirm({ paymentKey, orderId, amount: Number(amount) })
      .then(() => {
        navigate(`/order/complete/${orderId}`, { replace: true });
      })
      .catch((confirmError) => {
        const message = confirmError instanceof ApiError ? confirmError.message : "결제 확인 중 문제가 발생했습니다.";
        navigate("/payment/fail", { replace: true, state: { message, orderNumber: orderId } });
      });
  }, [searchParams, navigate]);

  return (
    <div className="container section">
      <Loading label="결제를 확인하고 있습니다..." />
    </div>
  );
}
