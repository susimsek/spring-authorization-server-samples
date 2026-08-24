import { notFound } from "next/navigation";

import { RoleDetail } from "@/components/admin/RoleDetail";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export function generateStaticParams() {
  return [{ name: "_" }];
}

export default async function Page({
  params,
}: {
  params: Promise<{ lang: string; name: string }>;
}) {
  const { lang, name } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  return <RoleDetail locale={locale} dictionary={getDictionary(locale)} name={name} />;
}
