import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { ErrorView } from "@/components/auth/ErrorView";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

type ErrorPageProps = {
  params: Promise<{ lang: string }>;
};

export function generateStaticParams() {
  return [{ lang: "en" }, { lang: "tr" }];
}

export async function generateMetadata({ params }: ErrorPageProps): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) {
    return {};
  }

  const dictionary = getDictionary(lang);
  return {
    title: `${dictionary.error.types.server_error.title} | ${dictionary.brand.product}`,
    description: dictionary.error.types.server_error.description,
  };
}

export default async function ErrorPage({ params }: ErrorPageProps) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }

  const locale: Locale = lang;
  const dictionary = getDictionary(locale);

  return (
    <AuthLayout locale={locale} dictionary={dictionary}>
      <ErrorView dictionary={dictionary} />
    </AuthLayout>
  );
}
