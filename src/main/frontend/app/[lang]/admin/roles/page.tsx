import { notFound } from "next/navigation";

import { RolesTable } from "@/components/admin/RolesTable";
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
        title={dictionary.admin.roles.title}
        description={dictionary.admin.roles.subtitle}
      />
      <RolesTable dictionary={dictionary} />
    </>
  );
}
