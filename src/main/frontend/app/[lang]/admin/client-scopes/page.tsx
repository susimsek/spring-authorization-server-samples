import { notFound } from "next/navigation";
import { ClientScopesTable } from "@/components/admin/ClientScopesTable";
import { isLocale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

export default async function Page({ params }: { params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  return <ClientScopesTable dictionary={getDictionary(lang)} />;
}
