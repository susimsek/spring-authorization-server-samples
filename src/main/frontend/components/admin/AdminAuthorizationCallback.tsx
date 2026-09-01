"use client";

import { ConsoleAuthorizationCallback } from "@/components/auth/ConsoleAuthorizationCallback";
import type { Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

import { useAdminAuth } from "./AdminAuthProvider";

export function AdminAuthorizationCallback({ locale }: { locale: Locale }) {
  const { completeAuthorization } = useAdminAuth();
  return (
    <ConsoleAuthorizationCallback
      completeAuthorization={completeAuthorization}
      errorMessage={getDictionary(locale).admin.common.authorizationCallbackError}
    />
  );
}
