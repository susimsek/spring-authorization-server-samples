"use client";

import axios from "axios";
import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import type { Locale } from "@/i18n/config";
import { adminRequest, registerAdminTokenHandlers } from "@/lib/admin-api";

import { type AdminAccess, useAdminAuth } from "./AdminAuthProvider";

type AdminWhoAmI = {
  username: string;
  authorities: string[];
  access: AdminAccess;
};

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

export function AdminAuthGuard({
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
    setAccess,
    setUsername,
  } = useAdminAuth();
  const isAuthorizationCallback = pathname.replace(/\/+$/, "").endsWith("/callback");

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
    // Tokens intentionally remain in memory. After a reload, probe the browser SSO
    // session without showing the login page; the callback retries interactively only
    // when the Authorization Server reports that no SSO session is available.
    void beginAuthorization(locale, `${window.location.pathname}${window.location.search}`, {
      prompt: "none",
    }).catch(() => {
      bootstrapStarted.current = false;
    });
  }, [beginAuthorization, locale]);

  useEffect(() => {
    if (!accessToken || isAuthorizationCallback) return;

    registerAdminTokenHandlers({
      refresh: refreshAccessToken,
      unauthorized: startLogin,
    });

    return () => registerAdminTokenHandlers(undefined);
  }, [accessToken, isAuthorizationCallback, refreshAccessToken, startLogin]);

  useEffect(() => {
    if (!expiresAt || isAuthorizationCallback) return;

    const renewIn = Math.max(expiresAt - Date.now() - 30_000, 0);
    const timer = window.setTimeout(() => {
      void refreshAccessToken().then((token) => {
        if (!token) startLogin();
      });
    }, renewIn);

    return () => window.clearTimeout(timer);
  }, [expiresAt, isAuthorizationCallback, refreshAccessToken, startLogin]);

  useEffect(() => {
    if (!initialized || isLoggingOut || isAuthorizationCallback) return;

    if (!accessToken) {
      restoreSession();
      return;
    }
    bootstrapStarted.current = false;

    const controller = new AbortController();

    adminRequest<AdminWhoAmI>(accessToken, {
      url: "/api/admin/whoami",
      signal: controller.signal,
    })
      .then((response) => {
        // admin-api already attempts a single refresh and invokes the registered
        // unauthorized handler when the refresh cannot recover the request.
        if (response.status === 401) return null;

        if (response.status === 403) {
          router.replace(`/${locale}/error?type=access_denied`);
          return null;
        }

        if (response.status >= 300) {
          throw new Error("Admin identity could not be loaded");
        }

        return response.data;
      })
      .then((admin) => {
        if (!admin) return;

        const hasAdminAccess = Object.values(admin.access).some(Boolean);
        if (!hasAdminAccess) {
          router.replace(`/${locale}/error?type=access_denied`);
          return;
        }

        setAccess(admin.access);
        setUsername(admin.username);
        setAuthorized(true);
      })
      .catch((error: unknown) => {
        // Route transitions abort the in-flight whoami request. Axios reports
        // this as CanceledError/ERR_CANCELED, not DOMException AbortError.
        // Treating it as a server failure caused the Clients/Scopes error page.
        if (isCanceledRequest(error)) return;
        router.replace(`/${locale}/error?type=server_error`);
      });

    return () => controller.abort();
  }, [
    accessToken,
    initialized,
    isAuthorizationCallback,
    isLoggingOut,
    locale,
    refreshAccessToken,
    restoreSession,
    router,
    startLogin,
    setAccess,
    setUsername,
  ]);

  if (isAuthorizationCallback) return children;

  if (!initialized || !authorized || !accessToken) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return children;
}
