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
  const beginAuthorization = jest.fn();
  const completeAuthorization = jest.fn();

  beforeEach(() => {
    replace.mockReset();
    beginAuthorization.mockReset();
    completeAuthorization.mockReset();
    window.history.replaceState({}, "", "/en/admin/callback");
    mockRouter.mockReturnValue({ replace });
    mockUseAdminAuth.mockReturnValue({ beginAuthorization, completeAuthorization });
  });

  it("restarts authorization for an incomplete response", async () => {
    beginAuthorization.mockResolvedValue(undefined);
    render(<AdminAuthorizationCallback locale="en" />);

    await waitFor(() =>
      expect(beginAuthorization).toHaveBeenCalledWith("en", "/en/admin", { prompt: "none" }),
    );
    expect(completeAuthorization).not.toHaveBeenCalled();
  });

  it("completes a valid response and redirects to the saved location", async () => {
    window.history.replaceState({}, "", "/en/admin/callback#code=code&state=state");
    completeAuthorization.mockResolvedValue("/en/admin/clients");
    render(<AdminAuthorizationCallback locale="en" />);

    await waitFor(() => expect(completeAuthorization).toHaveBeenCalledWith("en", "code", "state"));
    expect(replace).toHaveBeenCalledWith("/en/admin/clients");
  });

  it("shows an error when recovery fails", async () => {
    window.history.replaceState({}, "", "/tr/admin/callback#code=code&state=state");
    completeAuthorization.mockRejectedValue(new Error("invalid token"));
    beginAuthorization.mockRejectedValue(new Error("authorization failed"));
    render(<AdminAuthorizationCallback locale="tr" />);

    expect(
      await screen.findByText("The administration session could not be established."),
    ).toBeVisible();
  });
});
