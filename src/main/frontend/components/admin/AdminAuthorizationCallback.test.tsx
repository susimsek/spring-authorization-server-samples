import { render, screen, waitFor } from "@testing-library/react";
import { useRouter, useSearchParams } from "next/navigation";

import { AdminAuthorizationCallback } from "./AdminAuthorizationCallback";
import { useAdminAuth } from "./AdminAuthProvider";

jest.mock("next/navigation", () => ({ useRouter: jest.fn(), useSearchParams: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({ useAdminAuth: jest.fn() }));

const mockRouter = useRouter as jest.Mock;
const mockSearchParams = useSearchParams as jest.Mock;
const mockUseAdminAuth = useAdminAuth as jest.Mock;

describe("AdminAuthorizationCallback", () => {
  const replace = jest.fn();
  const completeAuthorization = jest.fn();

  beforeEach(() => {
    replace.mockReset();
    completeAuthorization.mockReset();
    mockRouter.mockReturnValue({ replace });
    mockUseAdminAuth.mockReturnValue({ completeAuthorization });
    mockSearchParams.mockReturnValue(new URLSearchParams());
  });

  it("shows an error for an incomplete response", () => {
    render(<AdminAuthorizationCallback locale="en" />);

    expect(screen.getByText("The administration session could not be established.")).toBeVisible();
    expect(completeAuthorization).not.toHaveBeenCalled();
  });

  it("completes a valid response and redirects to the saved location", async () => {
    mockSearchParams.mockReturnValue(new URLSearchParams("code=code&state=state"));
    completeAuthorization.mockResolvedValue("/en/admin/clients");
    render(<AdminAuthorizationCallback locale="en" />);

    await waitFor(() => expect(completeAuthorization).toHaveBeenCalledWith("en", "code", "state"));
    expect(replace).toHaveBeenCalledWith("/en/admin/clients");
  });

  it("shows an error when the token exchange fails", async () => {
    mockSearchParams.mockReturnValue(new URLSearchParams("code=code&state=state"));
    completeAuthorization.mockRejectedValue(new Error("invalid token"));
    render(<AdminAuthorizationCallback locale="tr" />);

    expect(
      await screen.findByText("The administration session could not be established."),
    ).toBeVisible();
  });
});
