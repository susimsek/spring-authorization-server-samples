import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";

type TokenHandlers = {
  refresh: () => Promise<string | null>;
  unauthorized: () => void;
};

type RetriableRequest = InternalAxiosRequestConfig & { _adminRetried?: boolean };

let tokenHandlers: TokenHandlers | undefined;

const adminApi = axios.create({ validateStatus: () => true });

adminApi.interceptors.response.use(async (response) => {
  response.data = normalizePageResponse(response.data);
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

type PageMetadata = {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

function normalizePageResponse(data: unknown) {
  if (!data || typeof data !== "object" || !("page" in data)) return data;

  const pageResponse = data as { page?: unknown } & Record<string, unknown>;
  const metadata = pageResponse.page;
  if (!isPageMetadata(metadata)) return data;

  const { page: _page, ...content } = pageResponse;
  return { ...content, ...metadata };
}

function isPageMetadata(value: unknown): value is PageMetadata {
  if (!value || typeof value !== "object") return false;
  const metadata = value as Partial<PageMetadata>;
  return (
    typeof metadata.number === "number" &&
    typeof metadata.size === "number" &&
    typeof metadata.totalElements === "number" &&
    typeof metadata.totalPages === "number"
  );
}

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
