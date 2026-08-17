import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { LoginForm } from "@/components/auth/LoginForm";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

type LoginPageProps = {
  params: Promise<{ lang: string }>;
};

export function generateStaticParams() {
  return [{ lang: "en" }, { lang: "tr" }];
}

export async function generateMetadata({ params }: LoginPageProps): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) {
    return {};
  }

  const dictionary = getDictionary(lang);
  return {
    title: `${dictionary.login.title} | ${dictionary.brand.product}`,
    description: dictionary.brand.description,
  };
}

export default async function LoginPage({ params }: LoginPageProps) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }

  const locale: Locale = lang;
  const dictionary = getDictionary(locale);

  return (
    <AuthLayout locale={locale} dictionary={dictionary}>
      <LoginForm dictionary={dictionary} />
    </AuthLayout>
  );
}
