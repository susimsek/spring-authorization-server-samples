import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { I18nProvider } from "next-i18next/client";

import dictionary from "@/locales/en/common.json";
import config from "@/i18n.config";

import { PasskeySettings } from "./PasskeySettings";

const mockRequestAccount = jest.fn();

jest.mock("@/components/account/AccountAuthProvider", () => ({
  useAccountAuth: () => ({ accessToken: "access-token" }),
}));

jest.mock("@/lib/account-api", () => ({
  accountRequest: jest.fn(),
  requestAccount: (...args: unknown[]) => mockRequestAccount(...args),
}));

jest.mock("@/lib/webauthn", () => ({
  registerPasskey: jest.fn(),
}));

function renderSettings() {
  return render(
    <I18nProvider
      defaultNS={config.defaultNS}
      fallbackLng={config.fallbackLng}
      language="en"
      resources={config.resources}
      supportedLngs={config.supportedLngs}
    >
      <PasskeySettings dictionary={dictionary} />
    </I18nProvider>,
  );
}

describe("passkey account settings", () => {
  beforeEach(() => {
    mockRequestAccount.mockReset();
  });

  it("lists a credential and removes it after confirmation", async () => {
    mockRequestAccount.mockResolvedValue({
      content: [
        {
          credentialId: "credential-id",
          label: "Office laptop",
          createdAt: "2026-01-01T00:00:00Z",
          lastUsedAt: "2026-01-02T00:00:00Z",
          transports: ["internal"],
          backupEligible: true,
          backupState: true,
        },
      ],
      page: { number: 0, size: 20, totalElements: 1, totalPages: 1 },
    });

    renderSettings();

    expect(await screen.findByText("Office laptop")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: /Remove: Office laptop/ }));
    fireEvent.click(
      await screen.findByRole("button", {
        name: new RegExp(`^${dictionary.account.security.passkeys.remove}$`),
      }),
    );

    await waitFor(() =>
      expect(mockRequestAccount).toHaveBeenCalledWith("access-token", {
        method: "DELETE",
        url: "/api/account/webauthn/credentials/credential-id",
      }),
    );
  });

  it("shows the empty state when no credentials are registered", async () => {
    mockRequestAccount.mockResolvedValue({
      content: [],
      page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
    });

    renderSettings();

    expect(await screen.findByText(dictionary.account.security.passkeys.empty)).toBeVisible();
  });
});
