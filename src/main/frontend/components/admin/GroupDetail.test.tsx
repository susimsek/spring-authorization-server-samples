import { fireEvent, render, screen } from "@testing-library/react";
import en from "@/locales/en/common.json";
import tr from "@/locales/tr/common.json";
import { adminRequest } from "@/lib/admin-api";
import { GroupDetail } from "./GroupDetail";

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token", access: { manageUsers: true } }),
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
