import { Suspense } from "react";
import { notFound } from "next/navigation";

import { AdminAuthorizationCallback } from "@/components/admin/AdminAuthorizationCallback";
import { isLocale, type Locale } from "@/i18n/config";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();

  return (
    <Suspense>
      <AdminAuthorizationCallback locale={lang as Locale} />
    </Suspense>
  );
}
