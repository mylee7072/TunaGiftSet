import { loadTossPayments } from "@tosspayments/tosspayments-sdk";

const CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY;
const CUSTOMER_KEY_STORAGE_PREFIX = "tunagiftset.tossCustomerKey.";

const widgetsPromises = new Map();

export function getTossCustomerKey(memberId) {
  const storageKey = `${CUSTOMER_KEY_STORAGE_PREFIX}${memberId}`;
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved) return saved;

    const customerKey = `customer-${createRandomId()}`;
    localStorage.setItem(storageKey, customerKey);
    return customerKey;
  } catch {
    return `customer-${createRandomId()}`;
  }
}

function createRandomId() {
  return typeof crypto !== "undefined" && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export async function getTossWidgets(memberId) {
  if (!CLIENT_KEY) {
    throw new Error("VITE_TOSS_CLIENT_KEY가 설정되어 있지 않습니다.");
  }

  const customerKey = getTossCustomerKey(memberId);
  if (!widgetsPromises.has(customerKey)) {
    widgetsPromises.set(
      customerKey,
      loadTossPayments(CLIENT_KEY).then((tossPayments) => tossPayments.widgets({ customerKey }))
    );
  }

  return widgetsPromises.get(customerKey);
}

export async function requestPaymentWindow({
  memberId,
  orderNumber,
  amount,
  orderName,
  customerName,
  customerEmail,
  customerMobilePhone,
}) {
  const widgets = await getTossWidgets(memberId);
  await widgets.setAmount({ currency: "KRW", value: amount });

  const paymentWindow = await widgets.renderPaymentWindow();

  return new Promise((resolve, reject) => {
    let paymentRequested = false;

    paymentWindow.on("paymentRequest", async () => {
      if (paymentRequested) return;
      paymentRequested = true;

      try {
        await widgets.requestPayment({
          orderId: orderNumber,
          orderName,
          successUrl: `${window.location.origin}/payment/success`,
          failUrl: `${window.location.origin}/payment/fail`,
          customerName,
          customerEmail,
          customerMobilePhone,
        });
        resolve();
      } catch (error) {
        paymentWindow.destroy();
        reject(error);
      }
    });

    paymentWindow.on("cancel", () => {
      reject(new Error("결제가 취소되었습니다."));
    });
  });
}
