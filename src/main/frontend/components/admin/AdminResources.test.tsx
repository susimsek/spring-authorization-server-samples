/* eslint-disable @next/next/no-img-element, react/display-name */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { adminRequest } from "@/lib/admin-api";

import { AdminResources } from "./AdminResources";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({
    accessToken: "token",
    access: {
      manageUsers: true,
      manageSessions: true,
      manageConsents: true,
      manageKeys: true,
    },
  }),
}));
jest.mock("next/link", () => ({ children, href, ...props }: React.ComponentProps<"a">) => (
  <a href={href} {...props}>
    {children}
  </a>
));
jest.mock("next/image", () => ({ src, alt }: { src: string; alt: string }) => (
  <img alt={alt} src={src} />
));

const page = (content: unknown[]) => ({
  content,
  number: 0,
  totalPages: 1,
  totalElements: content.length,
});

describe("AdminResources", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("lists users and applies enable and delete actions after confirmation", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: page([
            { id: 1, username: "ada", enabled: true, avatarUrl: null, authorities: ["ROLE_USER"] },
          ]),
        } as never;
      }
      return { status: 204, data: null } as never;
    });

    render(<AdminResources copy={dictionary.admin.resources} locale="en" resource="users" />);
    expect(await screen.findByText("ada")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: dictionary.admin.resources.createUser }),
    ).toHaveAttribute("href", "/en/admin/users/new");

    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.disable }));
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith(
        "token",
        expect.objectContaining({ method: "PUT", url: "/api/admin/users/1/enabled" }),
      ),
    );

    expect(
      await screen.findByRole("button", { name: dictionary.admin.resources.delete }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.delete }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.resources.delete })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith(
        "token",
        expect.objectContaining({ method: "DELETE", url: "/api/admin/users/1" }),
      ),
    );
  });

  it("manages session, consent, and key resource actions", async () => {
    const resources = {
      sessions: {
        item: {
          id: "session/1",
          username: "ada@example.com",
          createdAt: "2026-01-01T12:00:00Z",
          lastAccessedAt: "2026-01-01T12:30:00Z",
          expiresAt: "2026-01-01T13:00:00Z",
          authorizationCount: 2,
        },
        button: dictionary.admin.resources.signOutAll,
        confirm: dictionary.admin.resources.signOutAll,
        url: "/api/admin/users/ada%40example.com/sessions",
      },
      consents: {
        item: {
          clientId: "client/1",
          clientName: "Web client",
          principalName: "ada@example.com",
          authorities: ["SCOPE_openid"],
        },
        button: dictionary.admin.resources.revoke,
        confirm: dictionary.admin.resources.revoke,
        url: "/api/admin/consents/client%2F1/ada%40example.com",
      },
    } as const;

    for (const [resource, expected] of Object.entries(resources)) {
      mockAdminRequest.mockImplementation(async (_token, config) => {
        if (!config.method) return { status: 200, data: page([expected.item]) } as never;
        return { status: 204, data: null } as never;
      });
      const view = render(
        <AdminResources
          copy={dictionary.admin.resources}
          locale="en"
          resource={resource as "sessions" | "consents"}
        />,
      );
      expect(await screen.findByRole("button", { name: expected.button })).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: expected.button }));
      fireEvent.click(screen.getAllByRole("button", { name: expected.confirm })[1]);
      await waitFor(() =>
        expect(mockAdminRequest).toHaveBeenCalledWith(
          "token",
          expect.objectContaining({ method: "DELETE", url: expected.url }),
        ),
      );
      view.unmount();
      jest.clearAllMocks();
    }

    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: page([
            {
              id: "key-1",
              kid: "key-id",
              type: "RSA",
              algorithm: "RS256",
              use: "sig",
              active: true,
              createdAt: "2026-01-01T12:00:00Z",
            },
          ]),
        } as never;
      }
      return { status: 201, data: { kid: "rotated-key" } } as never;
    });
    render(<AdminResources copy={dictionary.admin.resources} resource="keys" />);
    expect(await screen.findByText("key-id")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.rotateKey }));
    expect(await screen.findByText("rotated-key")).toBeInTheDocument();
  });

  it("shows the loading error when the initial resource request fails", async () => {
    mockAdminRequest.mockResolvedValue({ status: 500, data: null } as never);
    render(<AdminResources copy={dictionary.admin.resources} resource="users" />);
    expect(await screen.findByText(dictionary.admin.resources.operationError)).toBeVisible();
  });

  it("covers unmanaged users, avatar rendering, failed operations, and anonymous sessions", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: page([
            { id: 2, username: "bob", enabled: false, avatarUrl: "/bob.png", authorities: [] },
          ]),
        } as never;
      }
      return { status: 500, data: null } as never;
    });
    render(<AdminResources copy={dictionary.admin.resources} resource="users" locale="en" />);
    expect(await screen.findByAltText("")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.enable }));
    expect(await screen.findByText(dictionary.admin.resources.operationError)).toBeVisible();

    mockAdminRequest.mockResolvedValue({
      status: 200,
      data: page([
        {
          id: "anonymous",
          username: null,
          createdAt: "2026-01-01T12:00:00Z",
          lastAccessedAt: "2026-01-01T12:30:00Z",
          expiresAt: "2026-01-01T13:00:00Z",
          authorizationCount: 0,
        },
      ]),
    } as never);
    const view = render(<AdminResources copy={dictionary.admin.resources} resource="sessions" />);
    expect(await screen.findByText("-")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: dictionary.admin.resources.signOutAll }),
    ).not.toBeInTheDocument();
    view.unmount();
  });

  it("shows an error when key rotation fails", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) =>
      config.method
        ? ({ status: 500, data: null } as never)
        : ({ status: 200, data: page([]) } as never),
    );
    render(<AdminResources copy={dictionary.admin.resources} resource="keys" />);
    await screen.findByText(dictionary.admin.resources.empty);
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.rotateKey }));
    expect(await screen.findByText(dictionary.admin.resources.operationError)).toBeVisible();
  });

  it("changes resource filters and signs out an individual session", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (!config.method) {
        return {
          status: 200,
          data: {
            content: [
              {
                id: "session-1",
                username: "ada",
                createdAt: "2026-01-01T12:00:00Z",
                lastAccessedAt: "2026-01-01T12:30:00Z",
                expiresAt: "2026-01-01T13:00:00Z",
                authorizationCount: 1,
              },
            ],
            number: 0,
            totalPages: 2,
            totalElements: 1,
          },
        } as never;
      }
      return { status: 204, data: null } as never;
    });
    const view = render(<AdminResources copy={dictionary.admin.resources} resource="sessions" />);
    expect(await screen.findByText("ada")).toBeVisible();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "50" } });
    expect(await screen.findByText("ada")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.signOut }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.resources.signOut })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/sessions/session-1",
        method: "DELETE",
      }),
    );
    expect(await screen.findByText("ada")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.next }));
    view.unmount();
  });

  it("exercises users status/search and confirmation cancel callbacks", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (!config.method) return { status: 200, data: page([]) } as never;
      return { status: 204, data: null } as never;
    });
    const users = render(
      <AdminResources copy={dictionary.admin.resources} resource="users" locale="en" />,
    );
    await screen.findByText(dictionary.admin.resources.empty);
    const selects = screen.getAllByRole("combobox");
    fireEvent.change(selects[0], { target: { value: "true" } });
    expect(await screen.findByText(dictionary.admin.resources.empty)).toBeVisible();
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.resources.search }), {
      target: { value: "ada" },
    });
    users.unmount();

    mockAdminRequest.mockResolvedValue({
      status: 200,
      data: page([{ id: 1, username: "ada", enabled: true, avatarUrl: null, authorities: [] }]),
    } as never);
    render(<AdminResources copy={dictionary.admin.resources} resource="users" locale="en" />);
    await screen.findByText("ada");
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.delete }));
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.cancel }));
  });
});
