import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import AdminSettings from "./AdminSettings";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token" }),
}));
jest.mock("./LoginSettings", () => ({
  __esModule: true,
  default: ({ focusSection }: { focusSection?: string }) => (
    <div data-testid="login-settings">{focusSection}</div>
  ),
}));
jest.mock("./EmailSettings", () => ({
  __esModule: true,
  default: () => <div data-testid="email-settings" />,
}));
jest.unmock("@/routing/Link");

function Location() {
  return <output data-testid="location">{useLocation().pathname}</output>;
}

describe("AdminSettings", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAdminRequest.mockResolvedValue({
      status: 200,
      data: { issuer: "https://issuer.test" },
    } as never);
  });

  it("opens the settings navigation and routes to each settings section", async () => {
    render(
      <MemoryRouter initialEntries={["/admin/settings"]}>
        <Routes>
          <Route path="/admin/settings/:section?" element={<AdminSettings />} />
        </Routes>
        <Location />
      </MemoryRouter>,
    );

    expect(screen.getByText(dictionary.admin.settings.sections.general)).toBeVisible();
    expect(
      screen.getByRole("heading", { name: dictionary.admin.settings.generalTitle }),
    ).toBeVisible();

    for (const [key, label] of Object.entries(dictionary.admin.settings.sections)) {
      expect(screen.getByRole("link", { name: label })).toHaveAttribute(
        "href",
        key === "general"
          ? "/admin/settings"
          : `/admin/settings/${key === "passwordPolicy" ? "password-policy" : key === "otpPolicy" ? "otp-policy" : key === "bruteForce" ? "brute-force" : key}`,
      );
    }

    fireEvent.click(
      screen.getByRole("link", { name: dictionary.admin.settings.sections.otpPolicy }),
    );

    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent("/admin/settings/otp-policy"),
    );
    expect(screen.getByTestId("login-settings")).toHaveTextContent("otp-policy");
    expect(
      screen.getByRole("link", { name: dictionary.admin.settings.sections.otpPolicy }),
    ).toHaveClass("active");
  });
});
