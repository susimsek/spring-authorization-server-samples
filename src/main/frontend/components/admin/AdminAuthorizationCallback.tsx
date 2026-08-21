"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Alert, Spinner } from "react-bootstrap";

import type { Locale } from "@/i18n/config";

import { useAdminAuth } from "./AdminAuthProvider";

const subscribeToHydration = () => () => {};
const getHydratedSnapshot = () => true;
const getServerHydratedSnapshot = () => false;

export function AdminAuthorizationCallback({ locale }: { locale: Locale }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { completeAuthorization } = useAdminAuth();
  const hydrated = useSyncExternalStore(
    subscribeToHydration,
    getHydratedSnapshot,
    getServerHydratedSnapshot,
  );
  const [failed, setFailed] = useState(false);
  const completed = useRef(false);
  const code = searchParams.get("code");
  const state = searchParams.get("state");
  const invalidResponse = !code || !state;

  useEffect(() => {
    if (!hydrated || completed.current || !code || !state) return;
    completed.current = true;

    void completeAuthorization(locale, code, state)
      .then((returnTo) => router.replace(returnTo))
      .catch(() => setFailed(true));
  }, [code, completeAuthorization, hydrated, locale, router, state]);

  if (hydrated && (invalidResponse || failed)) {
    return <Alert variant="danger">The administration session could not be established.</Alert>;
  }

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
      <Spinner />
    </div>
  );
}
