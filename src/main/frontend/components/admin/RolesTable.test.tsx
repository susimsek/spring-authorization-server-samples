import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { adminRequest } from "@/lib/admin-api";

import { RolesTable } from "./RolesTable";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token" }),
}));
jest.mock("./useAdminTableState", () => ({
  useAdminTableState: () => ({
    page: 0,
    query: "",
    size: 10,
    setPage: jest.fn(),
    setQuery: jest.fn(),
    setSize: jest.fn(),
  }),
}));

describe("RolesTable", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAdminRequest.mockReset();
  });

  it("loads roles, protects built-in roles and filters roles", async () => {
    mockAdminRequest.mockResolvedValue({
      status: 200,
      data: [{ name: "ROLE_ADMIN" }, { name: "ROLE_USER" }, { name: "ROLE_AUDITOR" }],
    } as never);
    render(<RolesTable dictionary={dictionary} />);
    expect(await screen.findByText("ROLE_AUDITOR")).toBeVisible();
    expect(
      screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[0],
    ).toBeDisabled();
    expect(
      screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[2],
    ).not.toBeDisabled();
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.resources.search }), {
      target: { value: "auditor" },
    });
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "20" } });
    await waitFor(() => expect(screen.getByText("ROLE_AUDITOR")).toBeVisible());
  });

  it("validates and creates a role, then refreshes the list", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: [] } as never)
      .mockResolvedValueOnce({ status: 201, data: { name: "ROLE_AUDITOR" } } as never)
      .mockResolvedValueOnce({ status: 200, data: [{ name: "ROLE_AUDITOR" }] } as never);
    render(<RolesTable dictionary={dictionary} />);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", { url: "/api/admin/roles" }),
    );
    await waitFor(() =>
      expect(screen.getByRole("button", { name: dictionary.admin.roles.create })).toBeVisible(),
    );
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.create }));
    expect(await screen.findByText(dictionary.admin.common.validation.roleFormat)).toBeVisible();
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.roles.name }), {
      target: { value: "ROLE_AUDITOR" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.create }));
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/roles",
        method: "POST",
        data: { name: "ROLE_AUDITOR" },
      }),
    );
    expect(await screen.findByText("ROLE_AUDITOR")).toBeVisible();
  });

  it("maps duplicate role errors and deletes a role after confirmation", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: [{ name: "ROLE_AUDITOR" }] } as never)
      .mockResolvedValueOnce({
        status: 400,
        data: {
          errorCode: "admin_role_duplicate_name",
          violations: [{ field: "name" }],
        },
      } as never);
    render(<RolesTable dictionary={dictionary} />);
    expect(await screen.findByText("ROLE_AUDITOR")).toBeVisible();
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.roles.name }), {
      target: { value: "ROLE_AUDITOR" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.create }));
    expect(await screen.findByText(dictionary.admin.common.validation.roleDuplicate)).toBeVisible();

    mockAdminRequest.mockResolvedValueOnce({ status: 204, data: null } as never);
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[0]);
    expect(await screen.findByText(dictionary.admin.roles.deleteConfirm)).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.cancel }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[0]);
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/roles/ROLE_AUDITOR",
        method: "DELETE",
      }),
    );
  });

  it("shows loading and operation errors", async () => {
    mockAdminRequest.mockRejectedValueOnce(new Error("load failed"));
    render(<RolesTable dictionary={dictionary} />);
    expect(await screen.findByText(dictionary.admin.roles.operationError)).toBeVisible();
  });

  it("shows an operation error when deleting a role fails", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: [{ name: "ROLE_AUDITOR" }] } as never)
      .mockResolvedValueOnce({ status: 500, data: null } as never);
    render(<RolesTable dictionary={dictionary} />);
    expect(await screen.findByText("ROLE_AUDITOR")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.roles.delete }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.roles.delete })[1]);
    expect(await screen.findByText(dictionary.admin.roles.operationError)).toBeVisible();
  });
});
