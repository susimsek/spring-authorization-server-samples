import { notFound } from "next/navigation";

import { ConsentDetail } from "@/components/admin/ConsentDetail";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export function generateStaticParams() {
  return [{ key: "_" }];
}

export default async function Page({ params }: { params: Promise<{ lang: string; key: string }> }) {
  const { lang, key } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  return <ConsentDetail locale={locale} dictionary={getDictionary(locale)} routeKey={key} />;
}
