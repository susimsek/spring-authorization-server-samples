import { notFound } from "next/navigation";

import { UserEntityRoute } from "@/components/admin/UserEntityRoute";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

const sections = [
  "details",
  "credentials",
  "roles",
  "groups",
  "sessions",
  "consents",
  "events",
] as const;

export function generateStaticParams() {
  return sections.map((section) => ({ id: "_", section }));
}

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  return <UserEntityRoute locale={locale} dictionary={getDictionary(locale)} />;
}
