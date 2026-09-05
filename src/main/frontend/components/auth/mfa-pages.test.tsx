import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/locales/en/common.json";

import { MfaChallengePage } from "./MfaChallengePage";
import { RequiredActionsPage } from "./RequiredActionsPage";

const navigation = { searchParams: new URLSearchParams() };

jest.mock("@/routing/navigation", () => ({
  useSearchParams: () => navigation.searchParams,
}));

const fetchMock = jest.fn();

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: jest.fn().mockResolvedValue(body),
  } as unknown as Response;
}

describe("MFA authentication pages", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    Object.defineProperty(globalThis, "fetch", { configurable: true, value: fetchMock });
    navigation.searchParams = new URLSearchParams();
  });

  it("rejects a malformed setup code without calling the completion endpoint", async () => {
    fetchMock
      .mockResolvedValueOnce(
        response(200, [
          {
            key: "CONFIGURE_TOTP",
            displayName: "Configure authenticator app",
            description: "Register an authenticator app and verify a code.",
            version: 1,
          },
        ]),
      )
      .mockResolvedValueOnce(
        response(200, {
          secret: "SECRET",
          otpauthUri: "otpauth://totp/example",
          algorithm: "SHA1",
          digits: 6,
          periodSeconds: 30,
        }),
      );

    render(<RequiredActionsPage dictionary={dictionary} />);

    const input = await screen.findByLabelText(dictionary.requiredActions.totpCode);
    fireEvent.change(input, { target: { value: "12345" } });
    fireEvent.submit(input.closest("form")!);

    expect(await screen.findByText(dictionary.requiredActions.invalidCode)).toBeVisible();
    expect(input).toHaveClass("is-invalid");
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("shows a rejected setup code on the field and keeps the form available for retry", async () => {
    const serverMessage = "Authenticator code is invalid.";
    fetchMock
      .mockResolvedValueOnce(
        response(200, [
          {
            key: "CONFIGURE_TOTP",
            displayName: "Configure authenticator app",
            description: "Register an authenticator app and verify a code.",
            version: 1,
          },
        ]),
      )
      .mockResolvedValueOnce(
        response(200, {
          secret: "SECRET",
          otpauthUri: "otpauth://totp/example",
          algorithm: "SHA1",
          digits: 6,
          periodSeconds: 30,
        }),
      )
      .mockResolvedValueOnce(response(400, { detail: serverMessage }));

    render(<RequiredActionsPage dictionary={dictionary} />);

    const input = await screen.findByLabelText(dictionary.requiredActions.totpCode);
    fireEvent.change(input, { target: { value: "000000" } });
    fireEvent.submit(input.closest("form")!);

    expect(await screen.findByText(serverMessage)).toBeVisible();
    expect(input).toHaveClass("is-invalid");
    expect(input).toHaveValue("000000");
    expect(screen.getByRole("button", { name: dictionary.requiredActions.continue })).toBeEnabled();
  });

  it("rejects a malformed login challenge code before sending it", async () => {
    render(<MfaChallengePage dictionary={dictionary} />);

    const input = screen.getByLabelText(dictionary.mfa.code);
    fireEvent.change(input, { target: { value: "12ab" } });
    fireEvent.click(screen.getByRole("button", { name: dictionary.mfa.verify }));

    expect(await screen.findByText(dictionary.mfa.invalidCode)).toBeVisible();
    expect(input).toHaveClass("is-invalid");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows a rejected login challenge code on the field and permits another attempt", async () => {
    const serverMessage = "The authenticator code is invalid.";
    fetchMock.mockResolvedValueOnce(response(400, { detail: serverMessage }));
    render(<MfaChallengePage dictionary={dictionary} />);

    const input = screen.getByLabelText(dictionary.mfa.code);
    fireEvent.change(input, { target: { value: "000000" } });
    fireEvent.click(screen.getByRole("button", { name: dictionary.mfa.verify }));

    expect(await screen.findByText(serverMessage)).toBeVisible();
    expect(input).toHaveClass("is-invalid");
    expect(input).toHaveValue("000000");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: dictionary.mfa.verify })).toBeEnabled(),
    );
  });
});
