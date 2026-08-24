"use client";

import axios from "axios";
import { useCallback, useEffect, useRef } from "react";

import type { Locale } from "@/i18n/config";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import {
  applyConsoleToken,
  clearConsoleAuth,
  setConsoleInitialized,
  type ConsoleKind,
} from "@/store/auth-slice";

export type ConsoleTokenResponse = {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  id_token?: string;
};

export type JwtPayload = {
  exp?: number;
  iat?: number;
  nonce?: string;
  sid?: string;
  sub?: string;
  preferred_username?: string;
  realm_access?: { roles?: string[] };
  resource_access?: Record<string, { roles?: string[] }>;
};

type AuthorizationTransaction = {
  codeVerifier: string;
  createdAt: number;
  expires: number;
  nonce: string;
  prompt?: "none";
  redirectUri: string;
  returnTo: string;
  state: string;
};

type ConsoleAuthConfig = {
  clientId: string;
  scope: string;
  transactionKey: string;
  redirectPath: (locale: Locale) => string;
  postLogoutRedirectPath: (locale: Locale) => string;
  postLoginReturnToKey?: string;
};

type AuthorizationOptions = {
  prompt?: "none";
};

const CALLBACK_TTL_MS = 5 * 60 * 1000;

function base64Url(bytes: Uint8Array) {
  let value = "";
  bytes.forEach((byte) => {
    value += String.fromCharCode(byte);
  });
  return btoa(value).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function randomValue() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function codeChallenge(codeVerifier: string) {
  const bytes = new TextEncoder().encode(codeVerifier);
  return base64Url(new Uint8Array(await crypto.subtle.digest("SHA-256", bytes)));
}

function decodeJwt(token: string | undefined): JwtPayload | null {
  if (!token) return null;
  try {
    const [, encodedPayload] = token.split(".");
    if (!encodedPayload) return null;
    const padded = encodedPayload
      .replaceAll("-", "+")
      .replaceAll("_", "/")
      .padEnd(Math.ceil(encodedPayload.length / 4) * 4, "=");
    return JSON.parse(atob(padded)) as JwtPayload;
  } catch {
    return null;
  }
}

function ensureOpenIdScope(scope: string) {
  const values = scope.split(/\s+/).filter(Boolean);
  if (!values.includes("openid")) values.unshift("openid");
  return values.join(" ");
}

function callbackKey(config: ConsoleAuthConfig, state: string) {
  return `${config.transactionKey}:${state}`;
}

function cookieRead(key: string) {
  const name = `${encodeURIComponent(key)}=`;
  const value = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(name))
    ?.slice(name.length);
  if (!value) return null;
  try {
    return decodeURIComponent(value);
  } catch {
    return null;
  }
}

function cookieWrite(key: string, value: string, expires: number) {
  document.cookie = `${encodeURIComponent(key)}=${encodeURIComponent(value)}; expires=${new Date(expires).toUTCString()}; path=/; SameSite=Lax`;
}

function cookieRemove(key: string) {
  document.cookie = `${encodeURIComponent(key)}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; SameSite=Lax`;
}

function storeTransaction(config: ConsoleAuthConfig, transaction: AuthorizationTransaction) {
  const key = callbackKey(config, transaction.state);
  const value = JSON.stringify(transaction);
  try {
    localStorage.setItem(key, value);
  } catch {
    cookieWrite(key, value, transaction.expires);
  }
}

function readAndRemoveTransaction(config: ConsoleAuthConfig, state: string) {
  const key = callbackKey(config, state);
  let value: string | null = null;
  try {
    value = localStorage.getItem(key);
    localStorage.removeItem(key);
  } catch {
    value = cookieRead(key);
    cookieRemove(key);
  }
  if (!value) {
    value = cookieRead(key);
    cookieRemove(key);
  }
  if (!value) return null;
  try {
    const parsed = JSON.parse(value) as AuthorizationTransaction;
    if (!isTransaction(parsed) || parsed.expires < Date.now() || parsed.state !== state)
      return null;
    return parsed;
  } catch {
    return null;
  }
}

function clearStoredTransactions(config: ConsoleAuthConfig) {
  const prefix = `${config.transactionKey}:`;
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith(prefix))
      .forEach((key) => localStorage.removeItem(key));
  } catch {
    // Cookie fallback entries are state keyed and removed when consumed.
  }
  if (config.postLoginReturnToKey) sessionStorage.removeItem(config.postLoginReturnToKey);
}

function isTransaction(value: unknown): value is AuthorizationTransaction {
  if (!value || typeof value !== "object") return false;
  const transaction = value as Partial<AuthorizationTransaction>;
  return (
    typeof transaction.codeVerifier === "string" &&
    transaction.codeVerifier.length > 0 &&
    typeof transaction.createdAt === "number" &&
    Number.isFinite(transaction.createdAt) &&
    typeof transaction.expires === "number" &&
    Number.isFinite(transaction.expires) &&
    typeof transaction.nonce === "string" &&
    transaction.nonce.length > 0 &&
    (transaction.prompt === undefined || transaction.prompt === "none") &&
    typeof transaction.redirectUri === "string" &&
    transaction.redirectUri.length > 0 &&
    typeof transaction.returnTo === "string" &&
    transaction.returnTo.startsWith("/") &&
    typeof transaction.state === "string" &&
    transaction.state.length > 0
  );
}

function isPermanentRefreshFailure(error: unknown) {
  return (
    axios.isAxiosError(error) && (error.response?.status === 400 || error.response?.status === 401)
  );
}

export function useConsoleAuth(config: ConsoleAuthConfig, consoleKind: ConsoleKind) {
  const dispatch = useAppDispatch();
  const auth = useAppSelector((state) => state.auth[consoleKind]);
  const {
    accessToken,
    idToken,
    expiresAt,
    authenticated,
    initialized,
    sessionId,
    subject,
    tokenParsed,
    idTokenParsed,
    refreshTokenParsed,
    isLoggingOut,
  } = auth;
  const accessTokenRef = useRef<string | null>(null);
  const idTokenRef = useRef<string | null>(null);
  const refreshTokenRef = useRef<string | null>(null);
  const expiresAtRef = useRef<number | null>(null);
  const timeSkewRef = useRef<number | null>(null);
  const authorizationInProgress = useRef<Promise<void> | null>(null);
  const refreshInProgress = useRef<Promise<string | null> | null>(null);

  const releaseAuthorization = useCallback(() => {
    authorizationInProgress.current = null;
  }, []);

  const clearTransactionState = useCallback(() => {
    if (config.postLoginReturnToKey) sessionStorage.removeItem(config.postLoginReturnToKey);
    releaseAuthorization();
  }, [config.postLoginReturnToKey, releaseAuthorization]);

  const clearAuthentication = useCallback(
    (loggingOut = false) => {
      accessTokenRef.current = null;
      idTokenRef.current = null;
      refreshTokenRef.current = null;
      expiresAtRef.current = null;
      timeSkewRef.current = null;
      refreshInProgress.current = null;
      dispatch(clearConsoleAuth({ console: consoleKind, loggingOut }));
      clearTransactionState();
    },
    [clearTransactionState, consoleKind, dispatch],
  );

  const applyToken = useCallback(
    (token: ConsoleTokenResponse, timeLocal?: number, expectedNonce?: string) => {
      const accessPayload = decodeJwt(token.access_token);
      // Keycloak setToken() replaces the complete token set after every successful exchange.
      const nextIdToken = token.id_token ?? null;
      const nextRefreshToken = token.refresh_token ?? null;
      const idPayload = decodeJwt(nextIdToken ?? undefined);
      const refreshPayload = decodeJwt(nextRefreshToken ?? undefined);

      if (expectedNonce && (!idPayload || idPayload.nonce !== expectedNonce)) {
        clearAuthentication(false);
        throw new Error("Invalid nonce");
      }

      if (timeLocal && typeof accessPayload?.iat === "number") {
        timeSkewRef.current = Math.floor(timeLocal / 1000) - accessPayload.iat;
      }

      const responseExpiresAt = Date.now() + Math.max(token.expires_in, 0) * 1000;
      const jwtExpiresAt =
        typeof accessPayload?.exp === "number"
          ? (accessPayload.exp + (timeSkewRef.current ?? 0)) * 1000
          : null;
      const nextExpiresAt = jwtExpiresAt
        ? Math.min(responseExpiresAt, jwtExpiresAt)
        : responseExpiresAt;

      accessTokenRef.current = token.access_token;
      idTokenRef.current = nextIdToken;
      refreshTokenRef.current = nextRefreshToken;
      expiresAtRef.current = nextExpiresAt;
      dispatch(
        applyConsoleToken({
          console: consoleKind,
          accessToken: token.access_token,
          idToken: nextIdToken,
          expiresAt: nextExpiresAt,
          sessionId: accessPayload?.sid ?? null,
          subject: accessPayload?.sub ?? null,
          tokenParsed: accessPayload,
          idTokenParsed: idPayload,
          refreshTokenParsed: refreshPayload,
        }),
      );
      releaseAuthorization();
    },
    [clearAuthentication, consoleKind, dispatch, releaseAuthorization],
  );

  useEffect(() => {
    return () => {
      authorizationInProgress.current = null;
      refreshInProgress.current = null;
    };
  }, []);

  const refreshAccessToken = useCallback(
    async (minValidity = 5) => {
      const currentRefreshToken = refreshTokenRef.current;
      if (!currentRefreshToken) return null;
      if (refreshInProgress.current) return refreshInProgress.current;

      const expiresAt = expiresAtRef.current;
      const shouldRefresh =
        minValidity === -1 || !expiresAt || expiresAt <= Date.now() + minValidity * 1000;
      if (!shouldRefresh) return accessTokenRef.current;

      let timeLocal = Date.now();
      const request = axios
        .post<ConsoleTokenResponse>(
          "/oauth2/token",
          new URLSearchParams({
            client_id: config.clientId,
            grant_type: "refresh_token",
            refresh_token: currentRefreshToken,
          }),
          { headers: { "Content-Type": "application/x-www-form-urlencoded" } },
        )
        .then((response) => {
          timeLocal = (timeLocal + Date.now()) / 2;
          applyToken(response.data, timeLocal);
          return response.data.access_token;
        })
        .catch((error: unknown) => {
          // Same as Keycloak updateToken(): an invalid refresh token clears the in-memory set.
          if (isPermanentRefreshFailure(error)) {
            clearAuthentication(false);
            return null;
          }
          const currentAccessToken = accessTokenRef.current;
          const currentExpiresAt = expiresAtRef.current;
          return currentAccessToken && currentExpiresAt && currentExpiresAt > Date.now()
            ? currentAccessToken
            : null;
        })
        .finally(() => {
          if (refreshInProgress.current === request) refreshInProgress.current = null;
        });

      refreshInProgress.current = request;
      return request;
    },
    [applyToken, clearAuthentication, config.clientId],
  );

  useEffect(() => {
    // Mirrors Keycloak JS init({ onLoad: "check-sso" }): tokens stay in memory and
    // startup probes the Authorization Server session through prompt=none.
    dispatch(setConsoleInitialized({ console: consoleKind, initialized: true }));
  }, [consoleKind, dispatch]);

  const beginAuthorization = useCallback(
    async (locale: Locale, returnTo: string, options: AuthorizationOptions = {}) => {
      if (authorizationInProgress.current) return authorizationInProgress.current;

      const request = (async () => {
        const codeVerifier = randomValue();
        const state = randomValue();
        const nonce = randomValue();
        const redirectUri = `${window.location.origin}${config.redirectPath(locale)}`;
        const transaction: AuthorizationTransaction = {
          codeVerifier,
          createdAt: Date.now(),
          expires: Date.now() + CALLBACK_TTL_MS,
          nonce,
          prompt: options.prompt,
          redirectUri,
          returnTo,
          state,
        };
        storeTransaction(config, transaction);
        if (config.postLoginReturnToKey)
          sessionStorage.setItem(config.postLoginReturnToKey, returnTo);

        try {
          const parameters = new URLSearchParams({
            client_id: config.clientId,
            code_challenge: await codeChallenge(codeVerifier),
            code_challenge_method: "S256",
            nonce,
            redirect_uri: redirectUri,
            response_mode: "fragment",
            response_type: "code",
            scope: ensureOpenIdScope(config.scope),
            state,
            ui_locales: locale,
          });
          if (options.prompt) parameters.set("prompt", options.prompt);
          const authorizationUrl = new URL("/oauth2/authorize", window.location.origin);
          authorizationUrl.search = parameters.toString();
          window.location.assign(authorizationUrl);
        } catch (error) {
          readAndRemoveTransaction(config, state);
          clearTransactionState();
          throw error;
        }
      })();

      authorizationInProgress.current = request;
      return request;
    },
    [clearTransactionState, config],
  );

  const retryAuthorization = useCallback(
    async (locale: Locale, state: string | null, error: string | null) => {
      if (!state) throw new Error("Missing authorization state");
      const transaction = readAndRemoveTransaction(config, state);
      if (!transaction) {
        clearTransactionState();
        throw new Error("Missing authorization transaction");
      }

      const interactionRequired = new Set([
        "login_required",
        "interaction_required",
        "consent_required",
        "account_selection_required",
      ]);
      if (transaction.prompt !== "none" || !error || !interactionRequired.has(error)) {
        clearTransactionState();
        throw new Error("Authorization could not be resumed");
      }

      const returnTo = transaction.returnTo;
      clearTransactionState();
      await beginAuthorization(locale, returnTo);
    },
    [beginAuthorization, clearTransactionState, config],
  );

  const completeAuthorization = useCallback(
    async (_locale: Locale, code: string, state: string) => {
      const transaction = readAndRemoveTransaction(config, state);
      if (!transaction) {
        clearTransactionState();
        throw new Error("Missing authorization transaction");
      }

      let timeLocal = Date.now();
      try {
        const response = await axios.post<ConsoleTokenResponse>(
          "/oauth2/token",
          new URLSearchParams({
            client_id: config.clientId,
            code,
            code_verifier: transaction.codeVerifier,
            grant_type: "authorization_code",
            redirect_uri: transaction.redirectUri,
          }),
          { headers: { "Content-Type": "application/x-www-form-urlencoded" } },
        );
        timeLocal = (timeLocal + Date.now()) / 2;
        applyToken(response.data, timeLocal, transaction.nonce);
        return transaction.returnTo;
      } finally {
        clearTransactionState();
      }
    },
    [applyToken, clearTransactionState, config],
  );

  const logout = useCallback(
    async (locale: Locale) => {
      const idTokenHint = idTokenRef.current;
      clearStoredTransactions(config);
      clearAuthentication(true);

      const postLogoutRedirectUri = `${window.location.origin}${config.postLogoutRedirectPath(locale)}`;
      const parameters = new URLSearchParams({
        client_id: config.clientId,
        post_logout_redirect_uri: postLogoutRedirectUri,
      });
      if (idTokenHint) parameters.set("id_token_hint", idTokenHint);

      const logoutUrl = new URL("/connect/logout", window.location.origin);
      logoutUrl.search = parameters.toString();
      window.location.replace(logoutUrl);
    },
    [clearAuthentication, config],
  );

  return {
    accessToken,
    idToken,
    expiresAt,
    authenticated,
    initialized,
    sessionId,
    subject,
    tokenParsed,
    idTokenParsed,
    refreshTokenParsed,
    isLoggingOut,
    refreshAccessToken,
    logout,
    beginAuthorization,
    retryAuthorization,
    completeAuthorization,
  };
}
