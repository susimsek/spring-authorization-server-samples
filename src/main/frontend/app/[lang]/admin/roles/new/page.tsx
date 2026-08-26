import { notFound } from "next/navigation";

import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { RoleCreateForm } from "@/components/admin/RoleCreateForm";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  const dictionary = getDictionary(locale);

  return (
    <>
      <AdminPageHeader
        title={dictionary.admin.roles.create}
        description={dictionary.admin.roles.help}
      />
      <RoleCreateForm dictionary={dictionary} locale={locale} />
    </>
  );
}
