"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import type { Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";
import { adminRequest, registerAdminTokenHandlers } from "@/lib/admin-api";
import {
  isCanceledRequest,
  useConsoleSessionLifecycle,
} from "@/components/auth/useConsoleSessionLifecycle";

import { type AdminAccess, useAdminAuth } from "./AdminAuthProvider";

type AdminWhoAmI = {
  username: string;
  authorities: string[];
  access: AdminAccess;
};

export function AdminAuthGuard({
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
    setAccess,
    setUsername,
  } = useAdminAuth();
  const isAuthorizationCallback = pathname.replace(/\/+$/, "").endsWith("/callback");

  const startLogin = useConsoleSessionLifecycle({
    accessToken,
    beginAuthorization,
    expiresAt,
    isAuthorizationCallback,
    locale,
    refreshAccessToken,
    registerTokenHandlers: registerAdminTokenHandlers,
  });

  useEffect(() => {
    if (!initialized || isLoggingOut || isAuthorizationCallback) return;

    if (!accessToken) {
      // A normal authorization request reuses the server's browser SSO session when it
      // exists, and displays the login page only when it does not. A separate silent
      // probe would start a second authorization transaction.
      startLogin();
      return;
    }
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
    router,
    startLogin,
    setAccess,
    setUsername,
  ]);

  if (isAuthorizationCallback) return callbackContent ?? children;

  if (!initialized || !authorized || !accessToken) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">{getDictionary(locale).admin.common.loading}</span>
        </div>
      </div>
    );
  }

  return children;
}
