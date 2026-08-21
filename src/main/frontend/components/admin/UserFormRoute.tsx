"use client";
import { useSearchParams } from "next/navigation";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { UserForm } from "./UserForm";
export function UserFormRoute({ locale, dictionary }: { locale: Locale; dictionary: Dictionary }) {
  return <UserForm locale={locale} dictionary={dictionary} id={useSearchParams().get("id")} />;
}
