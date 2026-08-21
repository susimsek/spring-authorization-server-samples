"use client";

import { createContext, useCallback, useContext, useRef, useState } from "react";
import axios from "axios";

import type { Locale } from "@/i18n/config";

export type AdminAccess = {
  viewClients: boolean;
  manageClients: boolean;
  viewUsers: boolean;
  manageUsers: boolean;
  viewRoles: boolean;
  manageRoles: boolean;
  viewSessions: boolean;
  manageSessions: boolean;
  viewConsents: boolean;
  manageConsents: boolean;
  viewKeys: boolean;
  manageKeys: boolean;
};

type AdminAuthContextValue = {
  accessToken: string | null;
  expiresAt: number | null;
  isLoggingOut: boolean;
  access: AdminAccess | null;
  setAccess: (access: AdminAccess | null) => void;
  refreshAccessToken: () => Promise<string | null>;
  logout: () => Promise<void>;
  beginAuthorization: (locale: Locale, returnTo: string) => Promise<void>;
  completeAuthorization: (locale: Locale, code: string, state: string) => Promise<string>;
};

type AuthorizationTransaction = {
  codeVerifier: string;
  returnTo: string;
  state: string;
};

type TokenResponse = {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
};

const ADMIN_CLIENT_ID = "admin-console";
const ADMIN_SCOPE = "profile admin-api";
const TRANSACTION_KEY = "ADMIN_OIDC_TRANSACTION";
const POST_LOGIN_RETURN_TO_KEY = "AUTH_ADMIN_RETURN_TO";
const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

function randomValue() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

function base64Url(bytes: Uint8Array) {
  let value = "";
  bytes.forEach((byte) => {
    value += String.fromCharCode(byte);
  });
  return btoa(value).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function codeChallenge(codeVerifier: string) {
  const bytes = new TextEncoder().encode(codeVerifier);
  return base64Url(new Uint8Array(await crypto.subtle.digest("SHA-256", bytes)));
}

function redirectUri(locale: Locale) {
  return `${window.location.origin}/${locale}/admin/callback`;
}

export function AdminAuthProvider({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<number | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [access, setAccess] = useState<AdminAccess | null>(null);
  const authorizationStarted = useRef(false);
  const refreshInProgress = useRef<Promise<string | null> | null>(null);

  const applyToken = useCallback((token: TokenResponse) => {
    setAccessToken(token.access_token);
    setExpiresAt(Date.now() + token.expires_in * 1000);
    if (token.refresh_token) {
      setRefreshToken(token.refresh_token);
    }
    authorizationStarted.current = false;
  }, []);

  const refreshAccessToken = useCallback(async () => {
    if (!refreshToken) {
      return null;
    }
    if (refreshInProgress.current) {
      return refreshInProgress.current;
    }

    refreshInProgress.current = axios
      .post<TokenResponse>(
        "/oauth2/token",
        new URLSearchParams({
          client_id: ADMIN_CLIENT_ID,
          grant_type: "refresh_token",
          refresh_token: refreshToken,
        }),
        { headers: { "Content-Type": "application/x-www-form-urlencoded" } },
      )
      .then((response) => {
        applyToken(response.data);
        return response.data.access_token;
      })
      .catch(() => null)
      .finally(() => {
        refreshInProgress.current = null;
      });

    return refreshInProgress.current;
  }, [applyToken, refreshToken]);

  const logout = useCallback(async () => {
    const tokenToRevoke = refreshToken;
    setIsLoggingOut(true);
    setAccessToken(null);
    setExpiresAt(null);
    setRefreshToken(null);
    setAccess(null);
    authorizationStarted.current = false;

    try {
      if (tokenToRevoke) {
        await axios.post(
          "/oauth2/revoke",
          new URLSearchParams({
            client_id: ADMIN_CLIENT_ID,
            token: tokenToRevoke,
            token_type_hint: "refresh_token",
          }),
          { headers: { "Content-Type": "application/x-www-form-urlencoded" } },
        );
      }
    } finally {
      await axios.post("/logout");
    }
  }, [refreshToken]);

  const beginAuthorization = useCallback(async (locale: Locale, returnTo: string) => {
    if (authorizationStarted.current) {
      return;
    }
    authorizationStarted.current = true;

    const codeVerifier = randomValue();
    const state = randomValue();
    const transaction: AuthorizationTransaction = { codeVerifier, returnTo, state };
    sessionStorage.setItem(TRANSACTION_KEY, JSON.stringify(transaction));
    sessionStorage.setItem(POST_LOGIN_RETURN_TO_KEY, returnTo);

    const parameters = new URLSearchParams({
      client_id: ADMIN_CLIENT_ID,
      code_challenge: await codeChallenge(codeVerifier),
      code_challenge_method: "S256",
      redirect_uri: redirectUri(locale),
      response_type: "code",
      scope: ADMIN_SCOPE,
      state,
      ui_locales: locale,
    });
    const authorizationUrl = new URL("/oauth2/authorize", window.location.origin);
    authorizationUrl.search = parameters.toString();
    window.location.assign(authorizationUrl);
  }, []);

  const completeAuthorization = useCallback(
    async (locale: Locale, code: string, state: string) => {
      const saved = sessionStorage.getItem(TRANSACTION_KEY);
      sessionStorage.removeItem(TRANSACTION_KEY);
      if (!saved) {
        throw new Error("Missing authorization transaction");
      }

      const transaction = JSON.parse(saved) as AuthorizationTransaction;
      if (transaction.state !== state) {
        throw new Error("Invalid authorization state");
      }

      const response = await axios.post<TokenResponse>(
        "/oauth2/token",
        new URLSearchParams({
          client_id: ADMIN_CLIENT_ID,
          code,
          code_verifier: transaction.codeVerifier,
          grant_type: "authorization_code",
          redirect_uri: redirectUri(locale),
        }),
        { headers: { "Content-Type": "application/x-www-form-urlencoded" } },
      );

      applyToken(response.data);
      sessionStorage.removeItem(POST_LOGIN_RETURN_TO_KEY);
      return transaction.returnTo;
    },
    [applyToken],
  );

  return (
    <AdminAuthContext.Provider
      value={{
        accessToken,
        expiresAt,
        isLoggingOut,
        access,
        setAccess,
        refreshAccessToken,
        logout,
        beginAuthorization,
        completeAuthorization,
      }}
    >
      {children}
    </AdminAuthContext.Provider>
  );
}

export function useAdminAuth() {
  const value = useContext(AdminAuthContext);
  if (!value) {
    throw new Error("AdminAuthProvider is required");
  }
  return value;
}
