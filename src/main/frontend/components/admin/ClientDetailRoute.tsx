"use client";

import { useSearchParams } from "next/navigation";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { ClientDetail } from "./ClientDetail";

export function ClientDetailRoute({
  locale,
  dictionary,
}: {
  locale: Locale;
  dictionary: Dictionary;
}) {
  const searchParams = useSearchParams();

  return <ClientDetail locale={locale} dictionary={dictionary} id={searchParams.get("id")} />;
}
