import { notFound } from "next/navigation";
import { AccountAuthGuard } from "@/components/account/AccountAuthGuard";
import { AccountAuthProvider } from "@/components/account/AccountAuthProvider";
import { AccountShell } from "@/components/account/AccountShell";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function AccountLayout({
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
    <AccountAuthProvider>
      <AccountAuthGuard locale={locale} callbackContent={children}>
        <AccountShell locale={locale} dictionary={getDictionary(locale)}>
          {children}
        </AccountShell>
      </AccountAuthGuard>
    </AccountAuthProvider>
  );
}
