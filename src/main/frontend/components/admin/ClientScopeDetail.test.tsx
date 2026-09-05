/* eslint-disable react/display-name */

import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import { ClientScopeDetail } from "./ClientScopeDetail";

const mockReplace = jest.fn();
const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token", access: { manageClients: true } }),
}));
jest.mock("@/routing/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }),
}));
jest.mock("@/routing/Link", () => ({ children, href, ...props }: React.ComponentProps<"a">) => (
  <a href={href} {...props}>
    {children}
  </a>
));
jest.mock("@/components/auth/ConsoleAlerts", () => ({
  useConsoleAlerts: () => ({ addAlert: jest.fn(), addError: jest.fn() }),
}));

const scope = {
  id: "scope-1",
  name: "profile",
  displayName: "Profile",
  description: "Profile claims",
  createdAt: "2026-01-01T10:00:00Z",
  updatedAt: "2026-01-02T10:00:00Z",
};

describe("ClientScopeDetail", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("loads a scope, links back to the list, and edits it inline", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: scope } as never)
      .mockResolvedValueOnce({
        status: 200,
        data: { ...scope, displayName: "Updated profile" },
      } as never);

    render(<ClientScopeDetail dictionary={dictionary} id="scope-1" locale="en" />);

    expect(await screen.findByRole("heading", { name: "profile" })).toBeVisible();
    expect(screen.queryByRole("button", { name: "profile Actions" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: dictionary.admin.clientScopes.title })).toHaveAttribute(
      "href",
      "/admin/client-scopes",
    );
    fireEvent.change(
      screen.getByRole("textbox", { name: dictionary.admin.clientScopes.displayName }),
      {
        target: { value: "Updated profile" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/client-scopes/scope-1",
        method: "PUT",
        data: { name: "profile", displayName: "Updated profile", description: "Profile claims" },
      }),
    );
  });

  it("deletes the scope after confirmation", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: scope } as never)
      .mockResolvedValueOnce({ status: 204, data: null } as never);

    render(<ClientScopeDetail dictionary={dictionary} id="scope-1" locale="en" />);
    await screen.findByRole("heading", { name: "profile" });
    expect(screen.queryByRole("button", { name: "profile Actions" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.clientScopes.delete }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: dictionary.admin.clientScopes.delete,
      }),
    );

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("/admin/client-scopes"));
  });
});
