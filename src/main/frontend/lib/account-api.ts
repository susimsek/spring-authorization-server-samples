import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";

type TokenHandlers = {
  refresh: () => Promise<string | null>;
  unauthorized: () => void;
};

type RetriableRequest = InternalAxiosRequestConfig & { _accountRetried?: boolean };

let tokenHandlers: TokenHandlers | undefined;
const accountApi = axios.create({ validateStatus: () => true });

accountApi.interceptors.response.use(async (response) => {
  const request = response.config as RetriableRequest;
  if (response.status !== 401 || request._accountRetried || !tokenHandlers) return response;
  const accessToken = await tokenHandlers.refresh();
  if (!accessToken) {
    tokenHandlers.unauthorized();
    return response;
  }
  request._accountRetried = true;
  request.headers.set("Authorization", `Bearer ${accessToken}`);
  return accountApi.request(request);
});

export function registerAccountTokenHandlers(handlers: TokenHandlers | undefined) {
  tokenHandlers = handlers;
}

export function accountRequest<T>(accessToken: string, config: AxiosRequestConfig) {
  return accountApi.request<T>({
    ...config,
    headers: { ...config.headers, Authorization: `Bearer ${accessToken}` },
    validateStatus: () => true,
  });
}
