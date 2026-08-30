import { notFound } from "next/navigation";

import { AdminDashboard } from "@/components/admin/AdminDashboard";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";

import { isLocale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();

  const dictionary = getDictionary(lang);

  return (
    <>
      <AdminPageHeader
        title={dictionary.admin.dashboard.title}
        description={dictionary.admin.dashboard.subtitle}
      />

      <AdminDashboard dictionary={dictionary} />
    </>
  );
}
