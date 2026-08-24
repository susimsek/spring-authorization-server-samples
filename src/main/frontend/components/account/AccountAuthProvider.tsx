"use client";

import { createContext, useCallback, useContext, useMemo } from "react";

import type { Locale } from "@/i18n/config";
import { type JwtPayload, useConsoleAuth } from "@/lib/console-auth";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { setConsoleUsername } from "@/store/auth-slice";

type AccountAuthRuntime = {
  refreshAccessToken: (minValidity?: number) => Promise<string | null>;
  logout: (locale: Locale) => Promise<void>;
  beginAuthorization: (
    locale: Locale,
    returnTo: string,
    options?: { prompt?: "none" },
  ) => Promise<void>;
  retryAuthorization: (locale: Locale, state: string | null, error: string | null) => Promise<void>;
  completeAuthorization: (locale: Locale, code: string, state: string) => Promise<string>;
};

type AccountAuthValue = AccountAuthRuntime & {
  accessToken: string | null;
  idToken: string | null;
  expiresAt: number | null;
  isLoggingOut: boolean;
  authenticated: boolean;
  initialized: boolean;
  sessionId: string | null;
  subject: string | null;
  tokenParsed: JwtPayload | null;
  idTokenParsed: JwtPayload | null;
  refreshTokenParsed: JwtPayload | null;
  username: string | null;
  setUsername: (username: string | null) => void;
};

const ACCOUNT_AUTH_CONFIG = {
  clientId: "account-console",
  scope: "profile account-api",
  transactionKey: "ACCOUNT_OIDC_TRANSACTION",
  postLoginReturnToKey: "AUTH_ACCOUNT_RETURN_TO",
  redirectPath: (locale: Locale) => `/${locale}/account/callback`,
  postLogoutRedirectPath: (locale: Locale) => `/${locale}/account/`,
};

const AccountAuthRuntimeContext = createContext<AccountAuthRuntime | null>(null);

export function AccountAuthProvider({ children }: { children: React.ReactNode }) {
  const {
    refreshAccessToken,
    logout,
    beginAuthorization,
    retryAuthorization,
    completeAuthorization,
  } = useConsoleAuth(ACCOUNT_AUTH_CONFIG, "account");

  const runtime = useMemo(
    () => ({
      refreshAccessToken,
      logout,
      beginAuthorization,
      retryAuthorization,
      completeAuthorization,
    }),
    [beginAuthorization, completeAuthorization, logout, refreshAccessToken, retryAuthorization],
  );

  return (
    <AccountAuthRuntimeContext.Provider value={runtime}>
      {children}
    </AccountAuthRuntimeContext.Provider>
  );
}

export function useAccountAuth(): AccountAuthValue {
  const runtime = useContext(AccountAuthRuntimeContext);
  const dispatch = useAppDispatch();
  const auth = useAppSelector((state) => state.auth.account);
  if (!runtime) throw new Error("AccountAuthProvider is required");

  const setUsername = useCallback(
    (value: string | null) => dispatch(setConsoleUsername({ console: "account", username: value })),
    [dispatch],
  );

  return {
    ...auth,
    ...runtime,
    setUsername,
  };
}
