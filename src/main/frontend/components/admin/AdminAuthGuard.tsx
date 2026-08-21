"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";

import type { Locale } from "@/i18n/config";
import { adminRequest, registerAdminTokenHandlers } from "@/lib/admin-api";

import { type AdminAccess, useAdminAuth } from "./AdminAuthProvider";

type AdminWhoAmI = {
  username: string;
  authorities: string[];
  access: AdminAccess;
};

export function AdminAuthGuard({
  locale,
  children,
}: {
  locale: Locale;
  children: React.ReactNode;
}) {
  const [authorized, setAuthorized] = useState(false);
  const pathname = usePathname();
  const {
    accessToken,
    beginAuthorization,
    expiresAt,
    isLoggingOut,
    refreshAccessToken,
    setAccess,
  } = useAdminAuth();
  const isAuthorizationCallback = pathname.replace(/\/+$/, "").endsWith("/callback");

  useEffect(() => {
    if (!accessToken || isAuthorizationCallback) {
      return;
    }

    const reauthorize = () => {
      void beginAuthorization(locale, `${window.location.pathname}${window.location.search}`);
    };
    registerAdminTokenHandlers({ refresh: refreshAccessToken, unauthorized: reauthorize });

    return () => registerAdminTokenHandlers(undefined);
  }, [
    accessToken,
    beginAuthorization,
    isAuthorizationCallback,
    locale,
    pathname,
    refreshAccessToken,
  ]);

  useEffect(() => {
    if (!expiresAt || isAuthorizationCallback) {
      return;
    }

    // Renew shortly before expiry so normal API requests continue without interruption.
    const renewIn = Math.max(expiresAt - Date.now() - 30_000, 0);
    const timer = window.setTimeout(() => {
      void refreshAccessToken().then((token) => {
        if (!token) {
          return beginAuthorization(locale, `${window.location.pathname}${window.location.search}`);
        }
      });
    }, renewIn);

    return () => window.clearTimeout(timer);
  }, [
    beginAuthorization,
    expiresAt,
    isAuthorizationCallback,
    locale,
    pathname,
    refreshAccessToken,
  ]);

  useEffect(() => {
    if (isLoggingOut || isAuthorizationCallback) {
      return;
    }

    if (!accessToken) {
      void beginAuthorization(locale, `${window.location.pathname}${window.location.search}`);
      return;
    }

    const controller = new AbortController();

    adminRequest<AdminWhoAmI>(accessToken, {
      url: "/api/admin/whoami",
      signal: controller.signal,
    })
      .then(async (response) => {
        if (response.status === 401) {
          await beginAuthorization(locale, `${window.location.pathname}${window.location.search}`);
          return null;
        }

        if (response.status === 403) {
          window.location.replace(`/${locale}/error?type=access_denied`);
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
          window.location.replace(`/${locale}/error?type=access_denied`);
          return;
        }

        setAccess(admin.access);
        setAuthorized(true);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        window.location.replace(`/${locale}/error?type=server_error`);
      });

    return () => controller.abort();
  }, [
    accessToken,
    beginAuthorization,
    isAuthorizationCallback,
    isLoggingOut,
    locale,
    pathname,
    setAccess,
  ]);

  if (isAuthorizationCallback) {
    return children;
  }

  if (!authorized) {
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
