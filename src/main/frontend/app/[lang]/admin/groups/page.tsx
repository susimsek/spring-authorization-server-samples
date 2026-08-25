import { notFound } from "next/navigation";

import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { GroupsTable } from "@/components/admin/GroupsTable";
import { getDictionary } from "@/i18n/get-dictionary";
import { isLocale } from "@/i18n/config";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const dictionary = getDictionary(lang);
  return (
    <>
      <AdminPageHeader
        title={dictionary.admin.groups.title}
        description={dictionary.admin.groups.subtitle}
      />
      <GroupsTable dictionary={dictionary} />
    </>
  );
}
