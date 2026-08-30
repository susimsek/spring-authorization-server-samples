"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Spinner } from "react-bootstrap";

import type { Locale } from "@/i18n/config";
import { getDictionary } from "@/i18n/get-dictionary";

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
  const { completeAuthorization } = useAdminAuth();
  const [failed, setFailed] = useState(false);
  const completed = useRef(false);

  useEffect(() => {
    if (completed.current) return;
    completed.current = true;

    const { code, state, error } = readAuthorizationResponse();
    clearAuthorizationResponseFromUrl();

    if (error || !code || !state) {
      queueMicrotask(() => setFailed(true));
      return;
    }

    void completeAuthorization(locale, code, state)
      .then((returnTo) => router.replace(returnTo))
      .catch(() => setFailed(true));
  }, [completeAuthorization, locale, router]);

  if (failed) {
    return (
      <Alert variant="danger">
        {getDictionary(locale).admin.common.authorizationCallbackError}
      </Alert>
    );
  }

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
      <Spinner />
    </div>
  );
}
