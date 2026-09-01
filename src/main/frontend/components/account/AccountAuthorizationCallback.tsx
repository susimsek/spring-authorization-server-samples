"use client";

import { ConsoleAuthorizationCallback } from "@/components/auth/ConsoleAuthorizationCallback";
import type { Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

import { useAccountAuth } from "./AccountAuthProvider";

export function AccountAuthorizationCallback({ locale }: { locale: Locale }) {
  const { completeAuthorization } = useAccountAuth();
  return (
    <ConsoleAuthorizationCallback
      completeAuthorization={completeAuthorization}
      errorMessage={getDictionary(locale).account.common.authorizationCallbackError}
    />
  );
}
