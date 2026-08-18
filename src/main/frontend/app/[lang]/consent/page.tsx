import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { ConsentForm } from "@/components/auth/ConsentForm";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

type ConsentPageProps = {
  params: Promise<{ lang: string }>;
};

export function generateStaticParams() {
  return [{ lang: "en" }, { lang: "tr" }];
}

export async function generateMetadata({ params }: ConsentPageProps): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) {
    return {};
  }

  const dictionary = getDictionary(lang);
  return {
    title: `${dictionary.consent.title} | ${dictionary.brand.product}`,
    description: dictionary.brand.description,
  };
}

export default async function ConsentPage({ params }: ConsentPageProps) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }

  const locale: Locale = lang;
  const dictionary = getDictionary(locale);

  return (
    <AuthLayout locale={locale} dictionary={dictionary}>
      <ConsentForm dictionary={dictionary} />
    </AuthLayout>
  );
}
