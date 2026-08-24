"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Spinner } from "react-bootstrap";

import type { Locale } from "@/i18n/config";

import { useAdminAuth } from "./AdminAuthProvider";

type AuthorizationResponse = {
  code: string | null;
  state: string | null;
  error: string | null;
};

function readAuthorizationResponse(): AuthorizationResponse {
  const hash = new URLSearchParams(window.location.hash.replace(/^#/, ""));
  const query = new URLSearchParams(window.location.search);
  return {
    code: hash.get("code") ?? query.get("code"),
    state: hash.get("state") ?? query.get("state"),
    error: hash.get("error") ?? query.get("error"),
  };
}

function clearAuthorizationResponseFromUrl() {
  window.history.replaceState(window.history.state, "", window.location.pathname);
}

export function AdminAuthorizationCallback({ locale }: { locale: Locale }) {
  const router = useRouter();
  const { beginAuthorization, completeAuthorization, retryAuthorization } = useAdminAuth();
  const [failed, setFailed] = useState(false);
  const completed = useRef(false);

  useEffect(() => {
    if (completed.current) return;
    completed.current = true;

    const { code, state, error } = readAuthorizationResponse();
    clearAuthorizationResponseFromUrl();

    if (error) {
      void retryAuthorization(locale, state, error).catch(() => setFailed(true));
      return;
    }

    const recover = async () => {
      clearAuthorizationResponseFromUrl();
      await beginAuthorization(locale, `/${locale}/admin`, { prompt: "none" });
    };

    if (!code || !state) {
      void recover().catch(() => setFailed(true));
      return;
    }

    void completeAuthorization(locale, code, state)
      .then((returnTo) => router.replace(returnTo))
      .catch(() => recover().catch(() => setFailed(true)));
  }, [beginAuthorization, completeAuthorization, locale, retryAuthorization, router]);

  if (failed) {
    return <Alert variant="danger">The administration session could not be established.</Alert>;
  }

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
      <Spinner />
    </div>
  );
}
