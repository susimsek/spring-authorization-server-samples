import type { AxiosRequestConfig } from "axios";

import { createAuthenticatedApiClient, type TokenHandlers } from "./authenticated-api-client";

const accountApi = createAuthenticatedApiClient();

export function registerAccountTokenHandlers(handlers: TokenHandlers | undefined) {
  accountApi.registerTokenHandlers(handlers);
}

export function accountRequest<T>(accessToken: string, config: AxiosRequestConfig) {
  return accountApi.request<T>(accessToken, config);
}
