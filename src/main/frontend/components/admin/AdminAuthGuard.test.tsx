import { render, screen, waitFor } from "@testing-library/react";

import { adminRequest, registerAdminTokenHandlers } from "@/lib/admin-api";

import { AdminAuthGuard } from "./AdminAuthGuard";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;
const mockRegisterAdminTokenHandlers = registerAdminTokenHandlers as jest.MockedFunction<
  typeof registerAdminTokenHandlers
>;
const mockBeginAuthorization = jest.fn().mockResolvedValue(undefined);
const mockRefreshAccessToken = jest.fn().mockResolvedValue("new-token");
const mockSetAccess = jest.fn();
let pathname = "/en/admin";
let auth = {
  accessToken: "token" as string | null,
  beginAuthorization: mockBeginAuthorization,
  expiresAt: null as number | null,
  isLoggingOut: false,
  refreshAccessToken: mockRefreshAccessToken,
  setAccess: mockSetAccess,
};

jest.mock("@/lib/admin-api", () => ({
  adminRequest: jest.fn(),
  registerAdminTokenHandlers: jest.fn(),
}));
jest.mock("next/navigation", () => ({ usePathname: () => pathname }));
jest.mock("./AdminAuthProvider", () => ({ useAdminAuth: () => auth }));

describe("AdminAuthGuard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    pathname = "/en/admin";
    auth = {
      accessToken: "token",
      beginAuthorization: mockBeginAuthorization,
      expiresAt: null,
      isLoggingOut: false,
      refreshAccessToken: mockRefreshAccessToken,
      setAccess: mockSetAccess,
    };
  });

  it("bypasses authorization on the callback route", () => {
    pathname = "/en/admin/callback/";
    render(<AdminAuthGuard locale="en">Callback</AdminAuthGuard>);
    expect(screen.getByText("Callback")).toBeVisible();
    expect(mockAdminRequest).not.toHaveBeenCalled();
  });

  it("loads whoami, registers token handlers and renders authorized content", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { username: "admin", authorities: ["ROLE_ADMIN"], access: { viewClients: true } },
    } as never);
    render(<AdminAuthGuard locale="en">Dashboard</AdminAuthGuard>);
    expect(screen.getByRole("status")).toBeVisible();
    expect(await screen.findByText("Dashboard")).toBeVisible();
    expect(mockSetAccess).toHaveBeenCalledWith({ viewClients: true });
    expect(registerAdminTokenHandlers).toHaveBeenCalledWith({
      refresh: mockRefreshAccessToken,
      unauthorized: expect.any(Function),
    });
  });

  it("reauthorizes through the token handler and cleans it up on unmount", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { username: "admin", authorities: [], access: { viewClients: true } },
    } as never);
    const view = render(<AdminAuthGuard locale="tr">Dashboard</AdminAuthGuard>);
    await screen.findByText("Dashboard");
    const registration = mockRegisterAdminTokenHandlers.mock.calls.find(([value]) => value);
    expect(registration?.[0]).toEqual({
      refresh: mockRefreshAccessToken,
      unauthorized: expect.any(Function),
    });
    await registration?.[0]?.unauthorized();
    expect(mockBeginAuthorization).toHaveBeenCalledWith("tr", "/");
    view.unmount();
    expect(mockRegisterAdminTokenHandlers).toHaveBeenLastCalledWith(undefined);
  });

  it("starts authorization when no access token is present", async () => {
    auth = { ...auth, accessToken: null };
    render(<AdminAuthGuard locale="tr">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockBeginAuthorization).toHaveBeenCalledWith("tr", "/"));
    expect(screen.queryByText("Protected")).not.toBeInTheDocument();
  });

  it("reauthorizes after a 401 response", async () => {
    mockAdminRequest.mockResolvedValueOnce({ status: 401, data: null } as never);
    render(<AdminAuthGuard locale="en">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockBeginAuthorization).toHaveBeenCalledWith("en", "/"));
  });

  it("renews an expiring token", async () => {
    jest.useFakeTimers();
    auth = { ...auth, expiresAt: Date.now() - 1 };
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { username: "admin", authorities: [], access: { viewClients: true } },
    } as never);
    render(<AdminAuthGuard locale="en">Dashboard</AdminAuthGuard>);
    await jest.advanceTimersByTimeAsync(1);
    await waitFor(() => expect(mockRefreshAccessToken).toHaveBeenCalled());
    jest.useRealTimers();
  });

  it("starts authorization when token renewal cannot refresh", async () => {
    jest.useFakeTimers();
    mockRefreshAccessToken.mockResolvedValueOnce(null);
    auth = { ...auth, expiresAt: Date.now() - 1 };
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { username: "admin", authorities: [], access: { viewClients: true } },
    } as never);
    render(<AdminAuthGuard locale="tr">Dashboard</AdminAuthGuard>);
    await jest.advanceTimersByTimeAsync(1);
    await waitFor(() => expect(mockBeginAuthorization).toHaveBeenCalledWith("tr", "/"));
    jest.useRealTimers();
  });

  it("redirects forbidden and inaccessible users", async () => {
    mockAdminRequest.mockResolvedValueOnce({ status: 403, data: null } as never);
    const first = render(<AdminAuthGuard locale="en">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockAdminRequest).toHaveBeenCalledTimes(1));
    first.unmount();

    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { username: "user", authorities: [], access: {} },
    } as never);
    render(<AdminAuthGuard locale="en">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockSetAccess).not.toHaveBeenCalled());
  });

  it("redirects on server errors and ignores abort errors", async () => {
    mockAdminRequest.mockRejectedValueOnce(new Error("failed"));
    render(<AdminAuthGuard locale="en">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockAdminRequest).toHaveBeenCalledTimes(1));

    mockAdminRequest.mockRejectedValueOnce(new DOMException("aborted", "AbortError"));
    const view = render(<AdminAuthGuard locale="en">Protected</AdminAuthGuard>);
    await waitFor(() => expect(mockAdminRequest).toHaveBeenCalled());
    view.unmount();
    expect(registerAdminTokenHandlers).toHaveBeenCalledWith(undefined);
  });
});
