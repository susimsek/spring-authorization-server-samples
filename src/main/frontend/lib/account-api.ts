import type { AxiosRequestConfig } from "axios";

import { createAuthenticatedApiClient, type TokenHandlers } from "./authenticated-api-client";

const accountClient = createAuthenticatedApiClient();

export type AccountApiError = {
  status: number;
  data: unknown;
};

export type AccountProfile = {
  username: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AccountApplication = {
  clientId: string;
  clientName: string;
  scopes: string[];
  createdAt: string;
  updatedAt: string;
};

export type AccountSession = {
  id: string;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
  current: boolean;
  clients: Array<{ clientId: string; clientName: string }>;
};

export function registerAccountTokenHandlers(handlers: TokenHandlers | undefined) {
  accountClient.registerTokenHandlers(handlers);
}

export function accountRequest<T>(accessToken: string, config: AxiosRequestConfig) {
  return accountClient.request<T>(accessToken, config);
}

export async function requestAccount<T>(accessToken: string, config: AxiosRequestConfig) {
  try {
    const response = await accountRequest<T>(accessToken, config);
    if (response.status >= 300) {
      throw { status: response.status, data: response.data } satisfies AccountApiError;
    }
    return response.data;
  } catch (error) {
    if (isAccountApiError(error)) throw error;
    throw {
      status: 0,
      data: { message: error instanceof Error ? error.message : "Account request failed" },
    } satisfies AccountApiError;
  }
}

function isAccountApiError(error: unknown): error is AccountApiError {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    typeof error.status === "number" &&
    "data" in error
  );
}
