import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import AdminEventSettings from "./AdminEventSettings";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token", access: { manageEvents: true } }),
}));

const settings = {
  eventsEnabled: true,
  adminEventsEnabled: true,
  adminEventsDetailsEnabled: true,
  eventsExpirationDays: 0,
};

describe("AdminEventSettings", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAdminRequest.mockImplementation((_token, config) => {
      if (config.method === "PUT") {
        return Promise.resolve({ status: 200, data: settings }) as never;
      }
      return Promise.resolve({ status: 200, data: settings }) as never;
    });
  });

  it("loads and saves event settings", async () => {
    render(<AdminEventSettings />);
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
  });

  it("shows an error when settings cannot be saved", async () => {
    mockAdminRequest.mockImplementation((_token, config) => {
      if (config.method === "PUT") {
        return Promise.resolve({ status: 500, data: null }) as never;
      }
      return Promise.resolve({ status: 200, data: settings }) as never;
    });
    render(<AdminEventSettings />);
    const saveButton = await screen.findByRole("button", {
      name: dictionary.admin.events.saveSettings,
    });
    await waitFor(() => expect(saveButton).toBeEnabled());
    fireEvent.click(saveButton);
    expect(await screen.findByText(dictionary.admin.events.settingsError)).toBeVisible();
  });
});
