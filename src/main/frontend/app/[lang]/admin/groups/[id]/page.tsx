import { notFound } from "next/navigation";

import { GroupDetail } from "@/components/admin/GroupDetail";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export function generateStaticParams() {
  return [{ id: "_" }];
}

export default async function Page({ params }: { params: Promise<{ lang: string; id: string }> }) {
  const { lang, id } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  return <GroupDetail id={id} locale={locale} dictionary={getDictionary(locale)} />;
}
