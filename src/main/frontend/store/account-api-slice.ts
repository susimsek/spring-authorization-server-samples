import { createApi, type BaseQueryFn } from "@reduxjs/toolkit/query/react";
import type { AxiosRequestConfig } from "axios";

import { accountRequest } from "@/lib/account-api";
import type { PageResponse } from "@/lib/api-types";

type AccountApiRequest = {
  accessToken: string;
  config: AxiosRequestConfig;
};

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

type Authenticated = { accessToken: string };
type PageQuery = Authenticated & { page: number; size: number };

const accountBaseQuery: BaseQueryFn<AccountApiRequest, unknown, AccountApiError> = async ({
  accessToken,
  config,
}) => {
  try {
    const response = await accountRequest<unknown>(accessToken, config);
    if (response.status < 300) return { data: response.data };
    return { error: { status: response.status, data: response.data } };
  } catch (error) {
    return { error: { status: 0, data: error } };
  }
};

export const accountApi = createApi({
  reducerPath: "accountApi",
  baseQuery: accountBaseQuery,
  tagTypes: ["AccountProfile", "AccountApplications", "AccountSessions"],
  endpoints: (builder) => ({
    getAccountProfile: builder.query<AccountProfile, Authenticated>({
      query: ({ accessToken }) => ({
        accessToken,
        config: { url: "/api/account/profile" },
      }),
      providesTags: ["AccountProfile"],
    }),
    updateAccountProfile: builder.mutation<
      AccountProfile,
      Authenticated & { firstName: string; lastName: string; email: string }
    >({
      query: ({ accessToken, ...data }) => ({
        accessToken,
        config: { method: "PUT", url: "/api/account/profile", data },
      }),
      invalidatesTags: ["AccountProfile"],
    }),
    updateAccountPassword: builder.mutation<
      void,
      Authenticated & { currentPassword: string; newPassword: string }
    >({
      query: ({ accessToken, ...data }) => ({
        accessToken,
        config: { method: "PUT", url: "/api/account/password", data },
      }),
    }),
    sendAccountVerificationEmail: builder.mutation<void, Authenticated>({
      query: ({ accessToken }) => ({
        accessToken,
        config: { method: "POST", url: "/api/account/send-verify-email" },
      }),
    }),
    getAccountApplications: builder.query<PageResponse<AccountApplication>, PageQuery>({
      query: ({ accessToken, page, size }) => ({
        accessToken,
        config: { url: `/api/account/applications?page=${page}&size=${size}` },
      }),
      providesTags: ["AccountApplications"],
    }),
    revokeAccountApplication: builder.mutation<void, Authenticated & { clientId: string }>({
      query: ({ accessToken, clientId }) => ({
        accessToken,
        config: {
          method: "DELETE",
          url: `/api/account/applications/${encodeURIComponent(clientId)}`,
        },
      }),
      invalidatesTags: ["AccountApplications", "AccountSessions"],
    }),
    getAccountSessions: builder.query<PageResponse<AccountSession>, PageQuery>({
      query: ({ accessToken, page, size }) => ({
        accessToken,
        config: { url: `/api/account/sessions?page=${page}&size=${size}` },
      }),
      providesTags: ["AccountSessions"],
    }),
    removeAccountSession: builder.mutation<void, Authenticated & { id: string }>({
      query: ({ accessToken, id }) => ({
        accessToken,
        config: { method: "DELETE", url: `/api/account/sessions/${encodeURIComponent(id)}` },
      }),
      invalidatesTags: ["AccountSessions"],
    }),
    removeOtherAccountSessions: builder.mutation<void, Authenticated>({
      query: ({ accessToken }) => ({
        accessToken,
        config: { method: "DELETE", url: "/api/account/sessions/others" },
      }),
      invalidatesTags: ["AccountSessions"],
    }),
  }),
});

export const {
  useGetAccountApplicationsQuery,
  useGetAccountProfileQuery,
  useGetAccountSessionsQuery,
  useRemoveAccountSessionMutation,
  useRemoveOtherAccountSessionsMutation,
  useRevokeAccountApplicationMutation,
  useSendAccountVerificationEmailMutation,
  useUpdateAccountPasswordMutation,
  useUpdateAccountProfileMutation,
} = accountApi;
