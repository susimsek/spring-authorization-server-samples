import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";

type TokenHandlers = {
  refresh: () => Promise<string | null>;
  unauthorized: () => void;
};

type RetriableRequest = InternalAxiosRequestConfig & { _adminRetried?: boolean };

let tokenHandlers: TokenHandlers | undefined;

const adminApi = axios.create({ validateStatus: () => true });

adminApi.interceptors.response.use(async (response) => {
  const request = response.config as RetriableRequest;
  if (response.status !== 401 || request._adminRetried || !tokenHandlers) {
    return response;
  }

  const accessToken = await tokenHandlers.refresh();
  if (!accessToken) {
    tokenHandlers.unauthorized();
    return response;
  }

  request._adminRetried = true;
  request.headers.set("Authorization", `Bearer ${accessToken}`);
  return adminApi.request(request);
});

export function registerAdminTokenHandlers(handlers: TokenHandlers | undefined) {
  tokenHandlers = handlers;
}

export function adminRequest<T>(accessToken: string, config: AxiosRequestConfig) {
  return adminApi.request<T>({
    ...config,
    headers: {
      ...config.headers,
      Authorization: `Bearer ${accessToken}`,
    },
    validateStatus: () => true,
  });
}
