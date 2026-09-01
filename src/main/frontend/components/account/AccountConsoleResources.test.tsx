import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { accountRequest } from "@/lib/account-api";
import { StoreProvider } from "@/store/StoreProvider";

import { AccountApplications } from "./AccountApplications";
import { AccountSessions } from "./AccountSessions";

const mockAccountRequest = accountRequest as jest.MockedFunction<typeof accountRequest>;
const mockLogout = jest.fn();

function renderWithStore(component: React.ReactNode) {
  return render(<StoreProvider>{component}</StoreProvider>);
}

jest.mock("@/lib/account-api", () => ({ accountRequest: jest.fn() }));
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
