import { notFound } from "next/navigation";

import { AdminDashboard } from "@/components/admin/AdminDashboard";

import { isLocale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();

  const dictionary = getDictionary(lang);

  return (
    <>
      <div className="admin-page-header">
        <h1>{dictionary.admin.dashboard.title}</h1>
        <p>{dictionary.admin.dashboard.subtitle}</p>
      </div>

      <AdminDashboard dictionary={dictionary} />
    </>
  );
}
