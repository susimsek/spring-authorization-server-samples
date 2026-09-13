import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import AdminEventsPage from "./AdminEvents";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token", access: { manageEvents: true } }),
}));
jest.mock("@/routing/navigation", () => ({
  usePathname: () => "/admin/events",
}));

describe("AdminEvents", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(window, "matchMedia", {
      configurable: true,
      value: jest.fn(() => ({
        matches: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        addListener: jest.fn(),
        removeListener: jest.fn(),
      })),
    });
    mockAdminRequest.mockImplementation((_token, config) => {
      if (config.method === "DELETE") {
        return Promise.resolve({ status: 204, data: null }) as never;
      }
      return Promise.resolve({
        status: 200,
        data: { content: [], totalElements: 0, totalPages: 0 },
      }) as never;
    });
  });

  it("clears all events and shows success feedback", async () => {
    render(<AdminEventsPage />);

    fireEvent.click(await screen.findByRole("button", { name: dictionary.admin.events.clearAll }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.events.clearAll })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        method: "DELETE",
        url: "/api/admin/events",
      }),
    );
    expect(screen.getByText(dictionary.admin.events.clearAllSuccess)).toBeVisible();
  });
});
