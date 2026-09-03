"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "@/routing/navigation";

import type { Locale } from "@/i18n/config";
import { accountRequest, registerAccountTokenHandlers } from "@/lib/account-api";
import {
  isCanceledRequest,
  useConsoleSessionLifecycle,
} from "@/components/auth/useConsoleSessionLifecycle";
import { useAccountAuth } from "./AccountAuthProvider";

type Profile = { username: string };

export function AccountAuthGuard({
  locale,
  children,
  callbackContent,
}: {
  locale: Locale;
  children: React.ReactNode;
  callbackContent?: React.ReactNode;
}) {
  const [authorized, setAuthorized] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const {
    accessToken,
    beginAuthorization,
    expiresAt,
    initialized,
    isLoggingOut,
    refreshAccessToken,
    setUsername,
  } = useAccountAuth();
  const callback = pathname.replace(/\/+$/, "").endsWith("/callback");

  const startLogin = useConsoleSessionLifecycle({
    accessToken,
    beginAuthorization,
    expiresAt,
    isAuthorizationCallback: callback,
    locale,
    refreshAccessToken,
    registerTokenHandlers: registerAccountTokenHandlers,
  });

  useEffect(() => {
    if (!initialized || isLoggingOut || callback) return;
    if (!accessToken) {
      startLogin();
      return;
    }
    const controller = new AbortController();
    accountRequest<Profile>(accessToken, { url: "/api/account/profile", signal: controller.signal })
      .then((response) => {
        if (response.status === 401) return null;
        if (response.status >= 300) throw new Error("Account profile could not be loaded");
        return response.data;
      })
      .then((profile) => {
        if (!profile) return;
        setUsername(profile.username);
        setAuthorized(true);
      })
      .catch((error: unknown) => {
        if (isCanceledRequest(error)) return;
        router.replace(`/auth-error?type=server_error`);
      });
    return () => controller.abort();
  }, [
    accessToken,
    callback,
    initialized,
    isLoggingOut,
    locale,
    refreshAccessToken,
    router,
    startLogin,
    setUsername,
  ]);

  if (callback) return callbackContent ?? children;
  if (!initialized || !authorized || !accessToken) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <div className="spinner-border text-primary" role="status" />
      </div>
    );
  }
  return children;
}
