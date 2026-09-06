const POSTCODE_SCRIPT_ID = "kakao-postcode-sdk";
const POSTCODE_SCRIPT_URL = "https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";

let loadingPromise = null;

export function loadKakaoPostcode() {
  if (window.kakao?.Postcode) {
    return Promise.resolve(window.kakao);
  }

  if (loadingPromise) {
    return loadingPromise;
  }

  loadingPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(POSTCODE_SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener("load", () => resolve(window.kakao), { once: true });
      existingScript.addEventListener("error", () => reject(new Error("Kakao postcode SDK load failed.")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.id = POSTCODE_SCRIPT_ID;
    script.src = POSTCODE_SCRIPT_URL;
    script.async = true;
    script.onload = () => {
      if (window.kakao?.Postcode) {
        resolve(window.kakao);
      } else {
        reject(new Error("Kakao postcode SDK is unavailable."));
      }
    };
    script.onerror = () => reject(new Error("Kakao postcode SDK load failed."));
    document.head.appendChild(script);
  }).catch((error) => {
    loadingPromise = null;
    throw error;
  });

  return loadingPromise;
}

export async function openKakaoPostcode(onComplete) {
  const kakao = await loadKakaoPostcode();
  new kakao.Postcode({
    oncomplete(data) {
      const extraAddress = buildExtraAddress(data);
      onComplete({
        zipCode: data.zonecode || "",
        roadAddress: data.roadAddress || data.address || "",
        jibunAddress: data.jibunAddress || "",
        extraAddress,
      });
    },
  }).open();
}

function buildExtraAddress(data) {
  const parts = [];
  if (data.bname && /[동로가]$/.test(data.bname)) {
    parts.push(data.bname);
  }
  if (data.buildingName && data.apartment === "Y") {
    parts.push(data.buildingName);
  }
  return parts.length > 0 ? `(${parts.join(", ")})` : "";
}
