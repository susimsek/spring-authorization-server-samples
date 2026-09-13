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

const settings = {
  eventsEnabled: true,
  adminEventsEnabled: true,
  adminEventsDetailsEnabled: true,
  eventsExpirationDays: 0,
};

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
      if (config.method === "PUT") {
        return Promise.resolve({ status: 200, data: settings }) as never;
      }
      if (config.method === "DELETE") {
        return Promise.resolve({ status: 204, data: null }) as never;
      }
      return Promise.resolve({
        status: 200,
        data: config.url?.includes("/config")
          ? settings
          : { content: [], totalElements: 0, totalPages: 0 },
      }) as never;
    });
  });

  it("saves event settings and clears all events", async () => {
    render(<AdminEventsPage />);

    const saveButton = await screen.findByRole("button", {
      name: dictionary.admin.events.saveSettings,
    });
    await waitFor(() => expect(saveButton).toBeEnabled());
    fireEvent.click(saveButton);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        method: "PUT",
        url: "/api/admin/events/config",
        data: settings,
      }),
    );
    expect(screen.getByText(dictionary.admin.events.settingsSaved)).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.events.clearAll }));
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.events.clearAll })[1]);
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        method: "DELETE",
        url: "/api/admin/events",
      }),
    );
    expect(screen.getByText(dictionary.admin.events.clearAllSuccess)).toBeVisible();
  });

  it("shows an error when saving event settings fails", async () => {
    mockAdminRequest.mockImplementation((_token, config) => {
      if (config.method === "PUT") {
        return Promise.resolve({ status: 500, data: null }) as never;
      }
      return Promise.resolve({
        status: 200,
        data: config.url?.includes("/config")
          ? settings
          : { content: [], totalElements: 0, totalPages: 0 },
      }) as never;
    });
    render(<AdminEventsPage />);
    const saveButton = await screen.findByRole("button", {
      name: dictionary.admin.events.saveSettings,
    });
    await waitFor(() => expect(saveButton).toBeEnabled());
    fireEvent.click(saveButton);
    expect(await screen.findByText(dictionary.admin.events.settingsError)).toBeVisible();
  });
});
