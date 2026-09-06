import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import i18next from "i18next";

import dictionary from "@/locales/en/common.json";
import { accountRequest } from "@/lib/account-api";
import { StoreProvider } from "@/store/StoreProvider";

import { AccountApplications } from "./AccountApplications";
import { AccountProfileForm } from "./AccountProfileForm";
import { AccountSessions } from "./AccountSessions";

const mockAccountRequest = accountRequest as jest.MockedFunction<typeof accountRequest>;
const mockLogout = jest.fn();
jest.mock("@/routing/navigation", () => ({
  usePathname: () => window.location.pathname,
}));

function renderWithStore(component: React.ReactNode) {
  return render(<StoreProvider>{component}</StoreProvider>);
}

jest.mock("@/lib/account-api", () => {
  const request = jest.fn();
  return {
    accountRequest: request,
    requestAccount: async (...args: Parameters<typeof request>) => {
      const response = await request(...args);
      if (response.status >= 300) throw { status: response.status, data: response.data };
      return response.data;
    },
  };
});
jest.mock("./AccountAuthProvider", () => ({
  useAccountAuth: () => ({
    accessToken: "token",
    logout: mockLogout,
  }),
}));
jest.mock("@/components/auth/ConsoleAlerts", () => ({
  useConsoleAlerts: () => ({
    addAlert: jest.fn(),
    addError: jest.fn(),
  }),
}));

describe("Account console resources", () => {
  it.each(["profile", "applications", "sessions"])(
    "updates %s timestamps when the UI locale changes",
    async (resource) => {
      const timestamp = "2026-09-03T13:45:00Z";
      const data = {
        username: "admin",
        firstName: "Admin",
        lastName: "User",
        email: "admin@example.test",
        createdAt: timestamp,
        updatedAt: timestamp,
        content: [
          {
            id: "session",
            clientId: "console",
            clientName: "Console",
            scopes: ["openid"],
            clients: [],
            current: true,
            createdAt: timestamp,
            updatedAt: timestamp,
            lastAccessedAt: timestamp,
            expiresAt: timestamp,
          },
        ],
        totalElements: 1,
        totalPages: 1,
      };
      mockAccountRequest.mockResolvedValue({ status: 200, data } as never);
      renderWithStore(
        resource === "profile" ? (
          <AccountProfileForm dictionary={dictionary} />
        ) : resource === "applications" ? (
          <AccountApplications dictionary={dictionary} />
        ) : (
          <AccountSessions dictionary={dictionary} />
        ),
      );
      expect(
        (await screen.findAllByText(new Date(timestamp).toLocaleString("en"), { exact: false }))
          .length,
      ).toBeGreaterThan(0);
      await act(() => i18next.changeLanguage("tr"));
      expect(
        screen.getAllByText(new Date(timestamp).toLocaleString("tr"), { exact: false }).length,
      ).toBeGreaterThan(0);
      expect(
        screen.queryByText(new Date(timestamp).toLocaleString("en"), { exact: false }),
      ).not.toBeInTheDocument();
    },
  );
  beforeEach(() => {
    jest.clearAllMocks();
    document.documentElement.lang = "en";
  });

  it("shows current and remote sessions with client context and confirms sign out", async () => {
    mockAccountRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: {
            content: [
              {
                id: "sid-current",
                createdAt: "2026-08-23T10:00:00Z",
                lastAccessedAt: "2026-08-23T10:30:00Z",
                expiresAt: "2026-08-23T11:00:00Z",
                current: true,
                clients: [{ clientId: "account-console", clientName: "Account Console" }],
              },
              {
                id: "sid-other",
                createdAt: "2026-08-22T10:00:00Z",
                lastAccessedAt: "2026-08-22T10:30:00Z",
                expiresAt: "2026-08-24T11:00:00Z",
                current: false,
                clients: [{ clientId: "mobile", clientName: "Mobile App" }],
              },
            ],
            totalPages: 1,
            totalElements: 2,
          },
        } as never;
      }
      return { status: 204, data: null } as never;
    });

    renderWithStore(<AccountSessions dictionary={dictionary} />);

    expect(await screen.findByText("Account Console")).toBeVisible();
    expect(screen.getByText("Mobile App")).toBeVisible();
    expect(screen.getAllByText(dictionary.account.sessions.current)).toHaveLength(2);

    fireEvent.click(screen.getByRole("button", { name: dictionary.account.sessions.signOut }));
    expect(screen.getByText(dictionary.account.sessions.signOutConfirm)).toBeVisible();
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: dictionary.account.sessions.signOut,
      }),
    );

    await waitFor(() =>
      expect(mockAccountRequest).toHaveBeenCalledWith("token", {
        method: "DELETE",
        url: "/api/account/sessions/sid-other",
      }),
    );
  });

  it("confirms application consent revocation and renders grant metadata", async () => {
    mockAccountRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: {
            content: [
              {
                clientId: "web-client",
                clientName: "Web Client",
                scopes: ["SCOPE_openid", "SCOPE_profile"],
                createdAt: "2026-08-20T10:00:00Z",
                updatedAt: "2026-08-21T10:00:00Z",
              },
            ],
            totalPages: 1,
            totalElements: 1,
          },
        } as never;
      }
      return { status: 204, data: null } as never;
    });

    renderWithStore(<AccountApplications dictionary={dictionary} />);

    expect(await screen.findByText("Web Client")).toBeVisible();
    expect(screen.getByText("openid")).toBeVisible();
    expect(screen.getByText("profile")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: dictionary.account.applications.revoke }));
    expect(screen.getByText(dictionary.account.applications.revokeConfirm)).toBeVisible();
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: dictionary.account.applications.revoke,
      }),
    );

    await waitFor(() =>
      expect(mockAccountRequest).toHaveBeenCalledWith("token", {
        method: "DELETE",
        url: "/api/account/applications/web-client",
      }),
    );
  });
});
