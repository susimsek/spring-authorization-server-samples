import { notFound } from "next/navigation";

import { LocaleDocumentLanguage } from "@/components/auth/LocaleDocumentLanguage";
import { isLocale, locales } from "@/i18n/config";

export function generateStaticParams() {
  return locales.map((lang) => ({ lang }));
}

export default async function LocaleLayout({
  children,
  params,
}: Readonly<{
  children: React.ReactNode;
  params: Promise<{ lang: string }>;
}>) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }

  return <LocaleDocumentLanguage lang={lang}>{children}</LocaleDocumentLanguage>;
}
