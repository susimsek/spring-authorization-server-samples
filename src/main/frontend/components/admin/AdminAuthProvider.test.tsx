/* eslint-disable react-hooks/globals */

import { act, render, waitFor } from "@testing-library/react";
import axios from "axios";

import { type AdminAccess, AdminAuthProvider, useAdminAuth } from "./AdminAuthProvider";

const mockPost = jest.fn();
const completeAccess: AdminAccess = {
  viewClients: true,
  manageClients: false,
  viewUsers: false,
  manageUsers: false,
  viewRoles: false,
  manageRoles: false,
  viewSessions: false,
  manageSessions: false,
  viewConsents: false,
  manageConsents: false,
  viewKeys: false,
  manageKeys: false,
};

let auth: ReturnType<typeof useAdminAuth>;

function Consumer() {
  auth = useAdminAuth();
  return null;
}

function renderProvider() {
  return render(
    <AdminAuthProvider>
      <Consumer />
    </AdminAuthProvider>,
  );
}

describe("AdminAuthProvider", () => {
  beforeEach(() => {
    mockPost.mockReset();
    Object.assign(axios, { post: mockPost });
    sessionStorage.clear();
    Object.defineProperty(globalThis, "crypto", {
      configurable: true,
      value: {
        getRandomValues: (bytes: Uint8Array) => bytes.fill(7),
        subtle: { digest: jest.fn().mockResolvedValue(new Uint8Array([1, 2, 3]).buffer) },
      },
    });
  });

  it("requires its provider", () => {
    expect(() => render(<Consumer />)).toThrow("AdminAuthProvider is required");
  });

  it("does not refresh when no refresh token is available", async () => {
    renderProvider();

    await expect(auth.refreshAccessToken()).resolves.toBeNull();
    expect(mockPost).not.toHaveBeenCalled();
  });

  it("starts an authorization request only once and stores its transaction", async () => {
    jest.spyOn(console, "error").mockImplementation(() => {});
    renderProvider();

    await act(async () => {
      await Promise.all([
        auth.beginAuthorization("tr", "/tr/admin"),
        auth.beginAuthorization("tr", "/tr/admin"),
      ]);
    });

    expect(sessionStorage.getItem("AUTH_ADMIN_RETURN_TO")).toBe("/tr/admin");
    expect(JSON.parse(sessionStorage.getItem("ADMIN_OIDC_TRANSACTION") ?? "{}")).toMatchObject({
      returnTo: "/tr/admin",
    });
  });

  it("rejects missing and invalid authorization transactions", async () => {
    renderProvider();

    await expect(auth.completeAuthorization("en", "code", "state")).rejects.toThrow(
      "Missing authorization transaction",
    );
    sessionStorage.setItem(
      "ADMIN_OIDC_TRANSACTION",
      JSON.stringify({ codeVerifier: "verifier", returnTo: "/en/admin", state: "expected" }),
    );

    await expect(auth.completeAuthorization("en", "code", "unexpected")).rejects.toThrow(
      "Invalid authorization state",
    );
  });

  it("exchanges a code, stores token state, and refreshes it", async () => {
    renderProvider();
    sessionStorage.setItem(
      "ADMIN_OIDC_TRANSACTION",
      JSON.stringify({ codeVerifier: "verifier", returnTo: "/en/admin", state: "state" }),
    );
    sessionStorage.setItem("AUTH_ADMIN_RETURN_TO", "/en/admin");
    mockPost
      .mockResolvedValueOnce({
        data: { access_token: "first", expires_in: 60, refresh_token: "refresh" },
      })
      .mockResolvedValueOnce({ data: { access_token: "second", expires_in: 60 } });

    await act(async () => {
      await expect(auth.completeAuthorization("en", "code", "state")).resolves.toBe("/en/admin");
    });
    expect(auth.accessToken).toBe("first");
    expect(auth.expiresAt).toEqual(expect.any(Number));
    expect(sessionStorage.getItem("AUTH_ADMIN_RETURN_TO")).toBeNull();

    await act(async () => {
      await expect(auth.refreshAccessToken()).resolves.toBe("second");
    });
    expect(auth.accessToken).toBe("second");
    expect(mockPost).toHaveBeenCalledTimes(2);
  });

  it("returns null after a failed refresh and revokes the refresh token on logout", async () => {
    renderProvider();
    sessionStorage.setItem(
      "ADMIN_OIDC_TRANSACTION",
      JSON.stringify({ codeVerifier: "verifier", returnTo: "/en/admin", state: "state" }),
    );
    mockPost
      .mockResolvedValueOnce({
        data: { access_token: "first", expires_in: 60, refresh_token: "refresh" },
      })
      .mockRejectedValueOnce(new Error("expired"))
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({});

    await act(async () => auth.completeAuthorization("en", "code", "state"));
    await act(async () => expect(auth.refreshAccessToken()).resolves.toBeNull());
    await act(async () => auth.logout());

    expect(auth.accessToken).toBeNull();
    expect(auth.isLoggingOut).toBe(true);
    expect(mockPost).toHaveBeenLastCalledWith("/logout");
  });

  it("allows access state to be updated by guards", async () => {
    renderProvider();

    await act(async () => auth.setAccess(completeAccess));
    await waitFor(() => expect(auth.access).toEqual(completeAccess));
  });
});
