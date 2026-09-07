import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";
import { adminRequest } from "@/lib/admin-api";

import EmailSettings from "./EmailSettings";

const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token" }),
}));

const settings = {
  enabled: true,
  fromAddress: "no-reply@example.com",
  baseUrl: "https://example.com",
  host: "smtp.example.com",
  port: 587,
  username: "smtp-user",
  passwordConfigured: true,
  smtpAuth: true,
  starttls: true,
  ssl: false,
};

describe("EmailSettings", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("tests the current SMTP settings and shows success feedback", async () => {
    mockAdminRequest
      .mockResolvedValueOnce({ status: 200, data: settings } as never)
      .mockResolvedValueOnce({ status: 204, data: null } as never);

    render(<EmailSettings embedded />);

    const testButton = await screen.findByRole("button", {
      name: dictionary.admin.emailSettings.testConnection,
    });
    fireEvent.click(testButton);

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenNthCalledWith(2, "token", {
        method: "POST",
        url: "/api/admin/settings/email/test",
        data: {
          enabled: true,
          fromAddress: "no-reply@example.com",
          baseUrl: "https://example.com",
          host: "smtp.example.com",
          port: 587,
          username: "smtp-user",
          password: "",
          smtpAuth: true,
          starttls: true,
          ssl: false,
        },
      }),
    );
    expect(screen.getByText(dictionary.admin.emailSettings.testConnectionSucceeded)).toBeVisible();
  });
});
