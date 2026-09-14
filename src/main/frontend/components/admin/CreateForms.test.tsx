import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import { ClientScopeCreateForm } from "./ClientScopeCreateForm";
import { GroupCreateForm } from "./GroupCreateForm";
import { RoleCreateForm } from "./RoleCreateForm";
import { UserProfileAttributeForm } from "./UserProfileAttributeForm";

const mockPush = jest.fn();
const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({
    accessToken: "token",
    access: { isAdmin: true, manageClients: true, manageRoles: true, manageUsers: true },
  }),
}));
jest.mock("@/components/auth/ConsoleAlerts", () => ({
  useConsoleAlerts: () => ({ addError: jest.fn(), addAlert: jest.fn() }),
}));
jest.mock("@/routing/navigation", () => ({
  useParams: () => ({ lang: "en" }),
  useRouter: () => ({ push: mockPush }),
}));

describe("dedicated administration creation forms", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAdminRequest.mockReset();
  });

  it("creates a group and opens its detail page", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/groups?page=0&size=100") {
        return { status: 200, data: { content: [] } } as never;
      }
      return { status: 201, data: { id: 7, name: "finance-operators" } } as never;
    });
    render(<GroupCreateForm dictionary={dictionary} locale="en" />);

    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.groups.name }), {
      target: { value: "finance-operators" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.groups.attributes }), {
      target: { value: '{"department":["finance"]}' },
    });
    fireEvent.click(screen.getByRole("checkbox", { name: dictionary.admin.groups.defaultGroup }));
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.groups.create }));

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/groups",
        method: "POST",
        data: {
          name: "finance-operators",
          parentId: null,
          attributes: { department: ["finance"] },
          defaultGroup: true,
        },
      }),
    );
    expect(mockPush).toHaveBeenCalledWith("/admin/groups/7");
  });

  it("validates and creates a role from its dedicated page", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 201,
      data: { name: "ROLE_AUDITOR" },
    } as never);
    render(<RoleCreateForm dictionary={dictionary} locale="en" />);

    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.create }));
    expect(await screen.findByText(dictionary.admin.common.validation.roleFormat)).toBeVisible();
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.roles.name }), {
      target: { value: "ROLE_AUDITOR" },
    });
    await waitFor(() =>
      expect(screen.getByRole("button", { name: dictionary.admin.roles.create })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.create }));

    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/admin/roles/ROLE_AUDITOR"));
  });

  it("creates a client scope and returns to the catalogue", async () => {
    mockAdminRequest.mockResolvedValueOnce({ status: 201, data: { id: "scope-1" } } as never);
    render(<ClientScopeCreateForm dictionary={dictionary} locale="en" />);

    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.clientScopes.name }), {
      target: { value: "invoice.read" },
    });
    fireEvent.change(
      screen.getByRole("textbox", { name: dictionary.admin.clientScopes.groupClaimName }),
      { target: { value: "roles.groups" } },
    );
    fireEvent.click(
      screen.getByRole("checkbox", { name: dictionary.admin.clientScopes.groupMapperEnabled }),
    );
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.clientScopes.create }));

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/client-scopes",
        method: "POST",
        data: {
          name: "invoice.read",
          displayName: "",
          description: "",
          groupMapperEnabled: true,
          groupClaimName: "roles.groups",
          groupMapperFullPath: true,
        },
      }),
    );
    expect(mockPush).toHaveBeenCalledWith("/admin/client-scopes");
  });

  it("creates a user profile attribute from its dedicated page", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 201,
      data: { id: 3, name: "department" },
    } as never);
    render(<UserProfileAttributeForm dictionary={dictionary} />);

    fireEvent.change(
      screen.getByRole("textbox", { name: dictionary.admin.userProfileSettings.name }),
      {
        target: { value: "department" },
      },
    );
    fireEvent.change(
      screen.getByRole("textbox", { name: dictionary.admin.userProfileSettings.displayName }),
      { target: { value: "Department" } },
    );
    fireEvent.click(
      screen.getByRole("button", { name: dictionary.admin.userProfileSettings.create }),
    );

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/settings/user-profile",
        method: "POST",
        data: {
          name: "department",
          displayName: "Department",
          description: "",
          type: "STRING",
          required: false,
          multivalued: false,
          minLength: null,
          maxLength: null,
          pattern: "",
          enabled: true,
          displayOrder: 0,
        },
      }),
    );
    expect(mockPush).toHaveBeenCalledWith("/admin/settings/user-profile");
  });
});
