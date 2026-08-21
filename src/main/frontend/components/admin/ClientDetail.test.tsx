/* eslint-disable react/display-name */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { adminRequest } from "@/lib/admin-api";

import { ClientDetail } from "./ClientDetail";

const mockPush = jest.fn();
const mockRefresh = jest.fn();
const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token" }),
}));
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, refresh: mockRefresh }),
}));
jest.mock("next/link", () => ({ children, href, ...props }: React.ComponentProps<"a">) => (
  <a href={href} {...props}>
    {children}
  </a>
));

const client = {
  clientId: "web-client",
  clientName: "Web client",
  redirectUris: ["https://app.example/callback"],
  postLogoutRedirectUris: [],
  clientIdIssuedAt: null,
  clientSecretExpiresAt: null,
  authorizationCodeTimeToLive: "PT5M",
  accessTokenTimeToLive: "PT5M",
  refreshTokenTimeToLive: "PT1H",
  clientAuthenticationMethods: ["client_secret_basic"],
  authorizationGrantTypes: ["authorization_code"],
  scopes: ["openid"],
  requireProofKey: true,
  requireAuthorizationConsent: false,
};

describe("ClientDetail", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("shows a not-found state without an id", () => {
    render(<ClientDetail dictionary={dictionary} id={null} locale="en" />);

    expect(screen.getByText(dictionary.admin.clients.notFound)).toBeInTheDocument();
  });

  it("loads a client, regenerates its secret, and deletes it after confirmation", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/clients/client-1") {
        return { status: config.method === "DELETE" ? 204 : 200, data: client } as never;
      }
      return { status: 200, data: { clientSecret: "new-secret" } } as never;
    });

    render(<ClientDetail dictionary={dictionary} id="client-1" locale="en" />);

    expect(await screen.findByRole("heading", { name: "Web client" })).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: dictionary.admin.clients.regenerateSecret }),
    );
    expect(await screen.findByText("new-secret")).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.common.close })[1]);

    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.clients.delete })[0]);
    expect(screen.getByText(dictionary.admin.clients.deleteConfirm)).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.clients.delete })[1]);

    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/en/admin/clients"));
    expect(mockRefresh).toHaveBeenCalled();
  });

  it("shows secret and delete errors and renders empty groups", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      const url = config.url ?? "";
      if (url === "/api/admin/clients/client-2") {
        return {
          status: 200,
          data: { ...client, id: "client-2", postLogoutRedirectUris: [], scopes: [] },
        } as never;
      }
      if (url.endsWith("/secret")) return { status: 500, data: null } as never;
      return { status: 500, data: null } as never;
    });
    render(<ClientDetail dictionary={dictionary} id="client-2" locale="en" />);
    expect(await screen.findByRole("heading", { name: "Web client" })).toBeVisible();
    expect(screen.getAllByText("—").length).toBeGreaterThan(0);
    fireEvent.click(
      screen.getByRole("button", { name: dictionary.admin.clients.regenerateSecret }),
    );
    expect(await screen.findByText(dictionary.admin.clients.secretError)).toBeVisible();
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.clients.delete })[0]);
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.cancel }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.clients.delete })[0]);
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.clients.delete })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/clients/client-2",
        method: "DELETE",
      }),
    );
  });
});
