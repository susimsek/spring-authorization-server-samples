import { notFound } from "next/navigation";
import { UserForm } from "@/components/admin/UserForm";
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
        <h1>{dictionary.admin.resources.createUserTitle}</h1>
        <p>{dictionary.admin.resources.createUserSubtitle}</p>
      </div>
      <UserForm locale={locale} dictionary={dictionary} />
    </>
  );
}
