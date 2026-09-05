import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";

import { MfaSettings } from "./MfaSettings";

const clearLocalSession = jest.fn();

jest.mock("./AccountAuthProvider", () => ({
  useAccountAuth: () => ({ accessToken: "access-token", clearLocalSession }),
}));

const fetchMock = jest.fn();

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: jest.fn().mockResolvedValue(body),
  } as unknown as Response;
}

describe("MFA account settings", () => {
  beforeEach(() => {
    clearLocalSession.mockReset();
    fetchMock.mockReset();
    Object.defineProperty(globalThis, "fetch", { configurable: true, value: fetchMock });
  });

  it("shows invalid codes on the field, permits retry, and clears invalidated login state", async () => {
    const serverMessage = "The authenticator code is invalid.";
    fetchMock
      .mockResolvedValueOnce(
        response(200, {
          enabled: false,
          available: true,
          required: false,
          issuer: "Authorization Server",
          digits: 6,
        }),
      )
      .mockResolvedValueOnce(
        response(200, {
          secret: "SECRET",
          otpauthUri: "otpauth://totp/example",
          digits: 6,
        }),
      )
      .mockResolvedValueOnce(response(400, { detail: serverMessage }))
      .mockResolvedValueOnce(response(204));

    render(<MfaSettings dictionary={dictionary} />);

    fireEvent.click(
      await screen.findByRole("button", { name: dictionary.account.security.mfa.setup }),
    );
    const input = await screen.findByPlaceholderText(dictionary.account.security.mfa.code);
    fireEvent.change(input, { target: { value: "000000" } });
    fireEvent.click(screen.getByRole("button", { name: dictionary.account.security.mfa.enable }));

    expect(await screen.findByText(serverMessage)).toBeVisible();
    expect(input).toHaveClass("is-invalid");

    fireEvent.change(input, { target: { value: "123456" } });
    expect(screen.queryByText(serverMessage)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: dictionary.account.security.mfa.enable }));

    await waitFor(() => expect(clearLocalSession).toHaveBeenCalledTimes(1));
  });

  it("checks the configured code length before sending an enable request", async () => {
    fetchMock
      .mockResolvedValueOnce(
        response(200, {
          enabled: false,
          available: true,
          required: false,
          issuer: "Authorization Server",
          digits: 8,
        }),
      )
      .mockResolvedValueOnce(
        response(200, {
          secret: "SECRET",
          otpauthUri: "otpauth://totp/example",
          digits: 8,
        }),
      );

    render(<MfaSettings dictionary={dictionary} />);

    fireEvent.click(
      await screen.findByRole("button", { name: dictionary.account.security.mfa.setup }),
    );
    const input = await screen.findByPlaceholderText(dictionary.account.security.mfa.code);
    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(screen.getByRole("button", { name: dictionary.account.security.mfa.enable }));

    expect(await screen.findByText(dictionary.account.security.mfa.invalidCode)).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
