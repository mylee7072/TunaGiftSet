// Business registration info required under Korean e-commerce law (전자상거래 등에서의
// 소비자보호에 관한 법률 제13조 / 통신판매업 신고). Kept in one place so Footer.jsx and
// CompanyInfoPage.jsx never hardcode these values directly.
//
// A field left as "" is treated as "not yet available" — components that render this
// object filter out empty values instead of showing a placeholder string.
export const companyInfo = {
  name: "세영종합유통",
  ceoName: "노영기",
  businessRegistrationNumber: "609-24-44780",
  address: "경상남도 창원시 의창구 원이대로200번길 42-1 (명서동)",
  businessType: "도소매",
  businessCategory: "통조림, 일용잡화, 선물세트",
  email: "", // TODO: 실제 연락용 이메일 주소를 입력하세요.
  mailOrderSalesNumber: "", // TODO: 통신판매업 신고 후 신고번호를 입력하세요.
};

// Ordered list of { label, value } pairs for display — filters out empty values so
// TODO fields above simply don't render anywhere this is used.
export function getCompanyInfoEntries() {
  return [
    { label: "상호", value: companyInfo.name },
    { label: "대표자", value: companyInfo.ceoName },
    { label: "사업자등록번호", value: companyInfo.businessRegistrationNumber },
    { label: "사업장 소재지", value: companyInfo.address },
    { label: "업태", value: companyInfo.businessType },
    { label: "종목", value: companyInfo.businessCategory },
    { label: "이메일", value: companyInfo.email },
    { label: "통신판매업 신고번호", value: companyInfo.mailOrderSalesNumber },
  ].filter((entry) => Boolean(entry.value));
}
