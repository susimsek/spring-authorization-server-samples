"use client";

import axios from "axios";
import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import type { Locale } from "@/i18n/config";
import { accountRequest, registerAccountTokenHandlers } from "@/lib/account-api";
import { useAccountAuth } from "./AccountAuthProvider";

type Profile = { username: string };

function isCanceledRequest(error: unknown) {
  return (
    axios.isCancel(error) ||
    (error instanceof DOMException && error.name === "AbortError") ||
    (typeof error === "object" &&
      error !== null &&
      "code" in error &&
      error.code === "ERR_CANCELED")
  );
}

export function AccountAuthGuard({
  locale,
  children,
}: {
  locale: Locale;
  children: React.ReactNode;
}) {
  const [authorized, setAuthorized] = useState(false);
  const bootstrapStarted = useRef(false);
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

  const startLogin = useCallback(() => {
    if (bootstrapStarted.current) return;
    bootstrapStarted.current = true;
    // Equivalent to Keycloak init({ onLoad: "login-required" }).
    void beginAuthorization(locale, `${window.location.pathname}${window.location.search}`).catch(
      () => {
        bootstrapStarted.current = false;
      },
    );
  }, [beginAuthorization, locale]);

  const restoreSession = useCallback(() => {
    if (bootstrapStarted.current) return;
    bootstrapStarted.current = true;
    // Equivalent to Keycloak init({ onLoad: "check-sso" }). Tokens intentionally remain
    // in memory, so a reload obtains a new authorization code from the browser SSO session.
    void beginAuthorization(locale, `${window.location.pathname}${window.location.search}`, {
      prompt: "none",
    }).catch(() => {
      bootstrapStarted.current = false;
    });
  }, [beginAuthorization, locale]);

  useEffect(() => {
    if (!accessToken || callback) return;
    registerAccountTokenHandlers({
      refresh: refreshAccessToken,
      unauthorized: startLogin,
    });
    return () => registerAccountTokenHandlers(undefined);
  }, [accessToken, callback, refreshAccessToken, startLogin]);

  useEffect(() => {
    if (!expiresAt || callback) return;
    const timer = window.setTimeout(
      () => {
        void refreshAccessToken().then((token) => {
          if (!token) startLogin();
        });
      },
      Math.max(expiresAt - Date.now() - 30_000, 0),
    );
    return () => window.clearTimeout(timer);
  }, [callback, expiresAt, refreshAccessToken, startLogin]);

  useEffect(() => {
    if (!initialized || isLoggingOut || callback) return;
    if (!accessToken) {
      restoreSession();
      return;
    }
    bootstrapStarted.current = false;

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
        router.replace(`/${locale}/error?type=server_error`);
      });
    return () => controller.abort();
  }, [
    accessToken,
    beginAuthorization,
    callback,
    initialized,
    isLoggingOut,
    locale,
    refreshAccessToken,
    restoreSession,
    router,
    startLogin,
    setUsername,
  ]);

  if (callback) return children;
  if (!initialized || !authorized || !accessToken) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <div className="spinner-border text-primary" role="status" />
      </div>
    );
  }
  return children;
}
