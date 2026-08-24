"use client";

import { usePathname } from "next/navigation";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { ClientDetail } from "./ClientDetail";

const SECTIONS = new Set(["settings", "credentials", "scopes", "sessions", "consents", "events"]);

export function ClientEntityRoute({
  locale,
  dictionary,
}: {
  locale: Locale;
  dictionary: Dictionary;
}) {
  const pathname = usePathname();
  const parts = pathname.split("/").filter(Boolean);
  const clientsIndex = parts.indexOf("clients");
  const id = clientsIndex >= 0 ? decodeURIComponent(parts[clientsIndex + 1] ?? "") : "";
  const candidate = clientsIndex >= 0 ? parts[clientsIndex + 2] : "";
  const section = candidate && SECTIONS.has(candidate) ? candidate : "settings";

  return <ClientDetail locale={locale} dictionary={dictionary} id={id || null} tab={section} />;
}
