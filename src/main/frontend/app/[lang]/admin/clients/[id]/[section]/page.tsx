import { notFound } from "next/navigation";

import { ClientEntityRoute } from "@/components/admin/ClientEntityRoute";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

const sections = ["settings", "credentials", "scopes", "sessions", "consents", "events"] as const;

export function generateStaticParams() {
  return sections.map((section) => ({ id: "_", section }));
}

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  return <ClientEntityRoute locale={locale} dictionary={getDictionary(locale)} />;
}
