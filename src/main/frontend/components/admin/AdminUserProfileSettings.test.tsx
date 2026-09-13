import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import AdminUserProfileSettings from "./AdminUserProfileSettings";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token", access: { isAdmin: true } }),
}));
jest.mock("./useAdminTableState", () => ({
  useAdminTableState: () => ({
    clearFilters: jest.fn(),
    page: 0,
    query: "",
    setPage: jest.fn(),
    setQuery: jest.fn(),
    setSize: jest.fn(),
    setSort: jest.fn(),
    size: 10,
    sort: "displayOrder,asc",
  }),
}));

const definition = (id: number, name: string, displayOrder: number) => ({
  id,
  name,
  displayName: name,
  description: null,
  type: "STRING" as const,
  required: false,
  multivalued: false,
  minLength: null,
  maxLength: null,
  pattern: null,
  enabled: true,
  displayOrder,
  builtIn: false,
});

describe("AdminUserProfileSettings", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("persists a keyboard reorder as a display order update", async () => {
    const first = definition(1, "department", 10);
    const second = definition(2, "employeeNumber", 20);
    mockAdminRequest
      .mockResolvedValueOnce({
        status: 200,
        data: { content: [first, second], totalPages: 1, totalElements: 2 },
      } as never)
      .mockResolvedValueOnce({
        status: 200,
        data: [definition(2, "employeeNumber", 10), definition(1, "department", 11)],
      } as never);

    render(<AdminUserProfileSettings dictionary={dictionary} />);
    const handles = await screen.findAllByRole("button", {
      name: new RegExp(dictionary.admin.userProfileSettings.reorder),
    });
    fireEvent.keyDown(handles[1], { key: "ArrowUp" });

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        method: "PUT",
        url: "/api/admin/settings/user-profile/order",
        data: { ids: [2, 1] },
      }),
    );
  });
});
