"use client";

import { useSearchParams } from "next/navigation";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { ClientForm } from "./ClientForm";

export function ClientFormRoute({
  locale,
  dictionary,
}: {
  locale: Locale;
  dictionary: Dictionary;
}) {
  const searchParams = useSearchParams();

  return (
    <ClientForm locale={locale} dictionary={dictionary} mode="edit" id={searchParams.get("id")} />
  );
}
