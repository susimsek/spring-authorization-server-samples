import { Suspense } from "react";
import { notFound } from "next/navigation";

import { ClientFormRoute } from "@/components/admin/ClientFormRoute";
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
        <h1>{dictionary.admin.clients.editTitle}</h1>
        <p>{dictionary.admin.clients.editSubtitle}</p>
      </div>
      <Suspense>
        <ClientFormRoute locale={locale} dictionary={dictionary} />
      </Suspense>
    </>
  );
}
