"use client";

import { usePathname } from "next/navigation";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { UserForm } from "./UserForm";

const SECTIONS = new Set([
  "details",
  "credentials",
  "roles",
  "groups",
  "sessions",
  "consents",
  "events",
]);

export function UserEntityRoute({
  locale,
  dictionary,
}: {
  locale: Locale;
  dictionary: Dictionary;
}) {
  const pathname = usePathname();
  const parts = pathname.split("/").filter(Boolean);
  const usersIndex = parts.indexOf("users");
  const id = usersIndex >= 0 ? decodeURIComponent(parts[usersIndex + 1] ?? "") : "";
  const candidate = usersIndex >= 0 ? parts[usersIndex + 2] : "";
  const section = candidate && SECTIONS.has(candidate) ? candidate : "details";

  return <UserForm locale={locale} dictionary={dictionary} id={id || null} tab={section} />;
}
