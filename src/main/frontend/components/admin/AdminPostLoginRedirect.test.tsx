import { render } from "@testing-library/react";

import { AdminPostLoginRedirect } from "./AdminPostLoginRedirect";

describe("AdminPostLoginRedirect", () => {
  beforeEach(() => {
    sessionStorage.clear();
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  it("redirects to a stored internal location", () => {
    sessionStorage.setItem("AUTH_ADMIN_RETURN_TO", "/en/admin/clients");
    render(<AdminPostLoginRedirect />);

    expect(sessionStorage.getItem("AUTH_ADMIN_RETURN_TO")).toBeNull();
  });

  it.each([null, "https://example.com/admin"])(
    "does not redirect to an absent or external location",
    (returnTo) => {
      if (returnTo) sessionStorage.setItem("AUTH_ADMIN_RETURN_TO", returnTo);
      render(<AdminPostLoginRedirect />);

      expect(sessionStorage.getItem("AUTH_ADMIN_RETURN_TO")).toBe(returnTo);
    },
  );
});
