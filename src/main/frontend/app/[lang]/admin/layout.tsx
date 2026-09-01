import { notFound } from "next/navigation";

import { AdminAuthGuard } from "@/components/admin/AdminAuthGuard";
import { AdminAuthProvider } from "@/components/admin/AdminAuthProvider";
import { AdminShell } from "@/components/admin/AdminShell";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function AdminLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ lang: string }>;
}) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();

  const locale: Locale = lang;

  return (
    <AdminAuthProvider>
      <AdminAuthGuard locale={locale} callbackContent={children}>
        <AdminShell locale={locale} dictionary={getDictionary(locale)}>
          {children}
        </AdminShell>
      </AdminAuthGuard>
    </AdminAuthProvider>
  );
}
