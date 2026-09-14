import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import en from "@/locales/en/common.json";
import tr from "@/locales/tr/common.json";
import { adminRequest } from "@/lib/admin-api";
import { GroupDetail } from "./GroupDetail";

let mockGroupIsAdmin = false;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({
    accessToken: "token",
    access: { manageUsers: true, isAdmin: mockGroupIsAdmin },
  }),
}));
jest.mock("@/components/auth/ConsoleAlerts", () => ({
  useConsoleAlerts: () => ({ addAlert: jest.fn(), addError: jest.fn() }),
}));
jest.mock("@/routing/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => "/admin/groups/7",
}));

it.each([en, tr])(
  "localizes group name length validation and prevents the update",
  async (dictionary) => {
    mockGroupIsAdmin = false;
    const request = jest.mocked(adminRequest);
    request.mockReset();
    request.mockImplementation(
      async (_token, config) =>
        ({
          status: 200,
          data:
            config.url === "/api/admin/groups/7"
              ? {
                  id: 7,
                  name: "Operators",
                  path: "/Operators",
                  parentId: null,
                  roles: [],
                  userCount: 0,
                  attributes: { department: ["finance"] },
                  defaultGroup: true,
                }
              : { content: [], totalPages: 0, totalElements: 0 },
        }) as never,
    );
    render(<GroupDetail id="7" locale="en" dictionary={dictionary} />);
    const name = await screen.findByRole("textbox", { name: dictionary.admin.groups.name });
    fireEvent.change(name, { target: { value: "a".repeat(101) } });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));
    expect(await screen.findByText(dictionary.admin.common.validation.max100)).toBeVisible();
    expect(request.mock.calls.some(([, config]) => config.method === "PUT")).toBe(false);
  },
);

it("edits group attributes and the default-group flag", async () => {
  mockGroupIsAdmin = true;
  const request = jest.mocked(adminRequest);
  request.mockReset();
  request.mockImplementation(
    async (_token, config) =>
      ({
        status: 200,
        data:
          config.method === "PUT"
            ? {
                id: 7,
                name: "Operators",
                path: "/Operators",
                parentId: null,
                roles: [],
                userCount: 0,
                attributes: { department: ["platform"] },
                defaultGroup: false,
              }
            : config.url === "/api/admin/groups/7"
              ? {
                  id: 7,
                  name: "Operators",
                  path: "/Operators",
                  parentId: null,
                  roles: [],
                  userCount: 0,
                  attributes: { department: ["finance"] },
                  defaultGroup: true,
                }
              : { content: [], totalPages: 0, totalElements: 0 },
      }) as never,
  );
  render(<GroupDetail id="7" locale="en" dictionary={en} />);

  const attributes = await screen.findByRole("textbox", { name: en.admin.groups.attributes });
  expect(screen.getByRole("heading", { name: en.admin.groups.permissions })).toBeVisible();
  expect(screen.getByRole("button", { name: en.admin.groups.addPermission })).toBeVisible();
  fireEvent.change(attributes, { target: { value: '{"department":["platform"]}' } });
  fireEvent.click(screen.getByRole("checkbox", { name: en.admin.groups.defaultGroup }));
  fireEvent.click(screen.getByRole("button", { name: en.admin.common.save }));

  await waitFor(() =>
    expect(request).toHaveBeenCalledWith("token", {
      url: "/api/admin/groups/7",
      method: "PUT",
      data: {
        name: "Operators",
        parentId: null,
        attributes: { department: ["platform"] },
        defaultGroup: false,
      },
    }),
  );
});
