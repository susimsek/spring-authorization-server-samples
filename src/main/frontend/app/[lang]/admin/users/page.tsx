import { notFound } from "next/navigation";

import { isLocale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";
import { AdminResources } from "@/components/admin/AdminResources";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();

  const dictionary = getDictionary(lang);
  return <AdminResources resource="users" copy={dictionary.admin.resources} locale={lang} />;
}
