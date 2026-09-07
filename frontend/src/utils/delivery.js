const TRACKING_URL_BUILDERS = [
  { keywords: ["cj", "대한통운"], build: (no) => `https://www.cjlogistics.com/ko/tool/parcel/tracking?gnbInvcNo=${no}` },
  { keywords: ["한진"], build: (no) => `https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&schLang=KR&wblnumText2=${no}` },
  { keywords: ["롯데"], build: (no) => `https://www.lotteglogis.com/home/reservation/tracking/linkView?InvNo=${no}` },
  { keywords: ["우체국", "epost", "post"], build: (no) => `https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=${no}` },
  { keywords: ["로젠"], build: (no) => `https://www.ilogen.com/web/personal/trace/${no}` },
  { keywords: ["gs", "편의점"], build: (no) => `https://www.cvsnet.co.kr/invoice/tracking.do?invoice_no=${no}` },
];

// Carrier is free text entered by an admin (no fixed enum on the backend), so this
// matches loosely by keyword and returns null when nothing matches, rather than
// guessing a URL that would point the customer somewhere wrong.
export function buildTrackingUrl(carrier, trackingNumber) {
  if (!carrier || !trackingNumber) return null;
  const normalized = carrier.toLowerCase();
  const match = TRACKING_URL_BUILDERS.find(({ keywords }) => keywords.some((keyword) => normalized.includes(keyword)));
  return match ? match.build(encodeURIComponent(trackingNumber)) : null;
}
