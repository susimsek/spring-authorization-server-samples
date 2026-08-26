import { notFound } from "next/navigation";

import { ClientScopeCreateForm } from "@/components/admin/ClientScopeCreateForm";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
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
        title={dictionary.admin.clientScopes.create}
        description={dictionary.admin.clientScopes.subtitle}
      />
      <ClientScopeCreateForm dictionary={dictionary} locale={locale} />
    </>
  );
}
