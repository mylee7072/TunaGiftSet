import { useEffect } from "react";
import { getCompanyInfoEntries } from "../config/company";
import { siteConfig } from "../config/siteConfig";

export function CompanyInfoPage() {
  useEffect(() => {
    document.title = `사업자 정보 - ${siteConfig.siteName}`;
  }, []);

  const entries = getCompanyInfoEntries();

  return (
    <div className="container section company-info-page">
      <p className="breadcrumb">홈 / 사업자 정보</p>
      <h1 className="page-title">사업자 정보</h1>
      <p className="section__description">
        전자상거래 등에서의 소비자보호에 관한 법률에 따라 {siteConfig.siteName}의 사업자 정보를 아래와 같이 표기합니다.
      </p>
      <dl className="company-info-page__table">
        {entries.map((entry) => (
          <div key={entry.label}>
            <dt>{entry.label}</dt>
            <dd>{entry.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
