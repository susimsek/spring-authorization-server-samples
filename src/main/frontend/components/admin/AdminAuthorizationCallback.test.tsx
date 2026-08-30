import { render, screen, waitFor } from "@testing-library/react";
import { useRouter } from "next/navigation";

import { AdminAuthorizationCallback } from "./AdminAuthorizationCallback";
import { useAdminAuth } from "./AdminAuthProvider";

jest.mock("next/navigation", () => ({ useRouter: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({ useAdminAuth: jest.fn() }));

const mockRouter = useRouter as jest.Mock;
const mockUseAdminAuth = useAdminAuth as jest.Mock;

describe("AdminAuthorizationCallback", () => {
  const replace = jest.fn();
  const completeAuthorization = jest.fn();

  beforeEach(() => {
    replace.mockReset();
    completeAuthorization.mockReset();
    window.history.replaceState({}, "", "/en/admin/callback");
    mockRouter.mockReturnValue({ replace });
    mockUseAdminAuth.mockReturnValue({ completeAuthorization });
  });

  it("shows an error for an incomplete response instead of starting another transaction", async () => {
    render(<AdminAuthorizationCallback locale="en" />);

    expect(
      await screen.findByText("The administration session could not be established."),
    ).toBeVisible();
    expect(completeAuthorization).not.toHaveBeenCalled();
  });

  it("completes a valid response and redirects to the saved location", async () => {
    window.history.replaceState({}, "", "/en/admin/callback#code=code&state=state");
    completeAuthorization.mockResolvedValue("/en/admin/clients");
    render(<AdminAuthorizationCallback locale="en" />);

    await waitFor(() => expect(completeAuthorization).toHaveBeenCalledWith("en", "code", "state"));
    expect(replace).toHaveBeenCalledWith("/en/admin/clients");
  });

  it("shows an error when the token exchange fails", async () => {
    window.history.replaceState({}, "", "/tr/admin/callback#code=code&state=state");
    completeAuthorization.mockRejectedValue(new Error("invalid token"));
    render(<AdminAuthorizationCallback locale="tr" />);

    expect(await screen.findByText("Yönetim oturumu oluşturulamadı.")).toBeVisible();
  });
});
