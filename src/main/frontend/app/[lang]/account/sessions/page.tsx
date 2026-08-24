import { notFound } from "next/navigation";
import { AccountSessions } from "@/components/account/AccountSessions";
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
      <AccountBreadcrumb homeLabel={d.account.manage} current={d.account.sessions.title} />
      <AccountPageHeader
        title={d.account.sessions.title}
        description={d.account.sessions.subtitle}
      />
      <AccountSessions dictionary={d} />
    </div>
  );
}
