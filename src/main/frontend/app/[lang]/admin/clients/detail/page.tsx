import { Suspense } from "react";
import { notFound } from "next/navigation";

import { ClientDetailRoute } from "@/components/admin/ClientDetailRoute";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;

  return (
    <Suspense>
      <ClientDetailRoute locale={locale} dictionary={getDictionary(locale)} />
    </Suspense>
  );
}
