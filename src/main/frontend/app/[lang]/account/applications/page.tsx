import { notFound } from "next/navigation";
import { AccountApplications } from "@/components/account/AccountApplications";
import { AccountPageHeader } from "@/components/account/AccountPageHeader";
import { AccountBreadcrumb } from "@/components/account/AccountBreadcrumb";
import { isLocale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";
export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const d = getDictionary(lang);
  return (
    <div className="d-grid gap-4">
      <AccountBreadcrumb homeLabel={d.account.manage} current={d.account.applications.title} />
      <AccountPageHeader
        title={d.account.applications.title}
        description={d.account.applications.subtitle}
      />
      <AccountApplications dictionary={d} />
    </div>
  );
}
