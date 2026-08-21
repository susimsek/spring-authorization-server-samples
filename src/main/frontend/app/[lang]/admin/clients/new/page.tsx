import { notFound } from "next/navigation";

import { ClientForm } from "@/components/admin/ClientForm";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  const dictionary = getDictionary(locale);

  return (
    <>
      <div className="admin-page-header">
        <h1>{dictionary.admin.clients.createTitle}</h1>
        <p>{dictionary.admin.clients.createSubtitle}</p>
      </div>
      <ClientForm locale={locale} dictionary={dictionary} mode="create" />
    </>
  );
}
