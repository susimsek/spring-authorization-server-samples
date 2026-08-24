import { notFound } from "next/navigation";
import { AccountProfileForm } from "@/components/account/AccountProfileForm";
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
      <AccountBreadcrumb homeLabel={d.account.manage} current={d.account.profile.title} />
      <AccountPageHeader title={d.account.profile.title} description={d.account.profile.subtitle} />
      <AccountProfileForm dictionary={d} />
    </div>
  );
}
