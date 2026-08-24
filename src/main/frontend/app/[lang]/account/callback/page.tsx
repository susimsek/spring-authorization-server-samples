import { Suspense } from "react";
import { notFound } from "next/navigation";
import { AccountAuthorizationCallback } from "@/components/account/AccountAuthorizationCallback";
import { isLocale, type Locale } from "@/i18n/config";
export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  return (
    <Suspense>
      <AccountAuthorizationCallback locale={lang as Locale} />
    </Suspense>
  );
}
