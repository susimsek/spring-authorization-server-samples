import { Suspense } from "react";
import { notFound } from "next/navigation";
import { UserFormRoute } from "@/components/admin/UserFormRoute";
import { isLocale, type Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";
export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const locale: Locale = lang;
  const dictionary = getDictionary(locale);
  return (
    <>
      <div className="admin-page-header">
        <h1>{dictionary.admin.resources.editUserTitle}</h1>
        <p>{dictionary.admin.resources.editUserSubtitle}</p>
      </div>
      <Suspense>
        <UserFormRoute locale={locale} dictionary={dictionary} />
      </Suspense>
    </>
  );
}
