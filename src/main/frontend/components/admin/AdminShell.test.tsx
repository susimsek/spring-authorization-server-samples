/* eslint-disable react/display-name */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";

import { AdminShell } from "./AdminShell";

const mockPush = jest.fn();
const mockLogout = jest.fn().mockResolvedValue(undefined);

jest.mock("next/navigation", () => ({
  usePathname: () => "/en/admin/clients/detail",
  useRouter: () => ({ push: mockPush }),
}));
jest.mock("next/link", () => ({ children, href, ...props }: React.ComponentProps<"a">) => (
  <a href={href} {...props}>
    {children}
  </a>
));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({
    access: {
      viewClients: true,
      viewUsers: true,
      manageRoles: true,
      viewSessions: true,
      viewConsents: true,
      viewKeys: true,
    },
    logout: mockLogout,
  }),
}));
jest.mock("@/components/auth/LanguageSwitcher", () => ({
  LanguageSwitcher: ({ label }: { label: string }) => <button>{label}</button>,
}));
jest.mock("@/components/auth/ThemeSwitcher", () => ({
  ThemeSwitcher: () => <button>Theme</button>,
}));
jest.mock("@fortawesome/react-fontawesome", () => ({
  FontAwesomeIcon: () => <span aria-hidden="true" />,
}));
jest.mock("react-bootstrap", () => {
  const MockNavbar = ({
    children,
    fluid: _fluid,
    ...props
  }: React.ComponentProps<"nav"> & { fluid?: boolean }) => {
    void _fluid;
    return <nav {...props}>{children}</nav>;
  };
  const MockNavbarBrand = ({
    children,
    as: _as,
    ...props
  }: React.ComponentProps<"a"> & { as?: unknown }) => {
    void _as;
    return <a {...props}>{children}</a>;
  };
  const MockNav = ({ children, ...props }: React.ComponentProps<"nav">) => (
    <nav {...props}>{children}</nav>
  );
  const MockNavLink = ({
    children,
    as: _as,
    active: _active,
    href,
    ...props
  }: React.ComponentProps<"a"> & { as?: unknown; active?: boolean }) => {
    void _as;
    void _active;
    return (
      <a href={href} {...props}>
        {children}
      </a>
    );
  };
  return {
    __esModule: true,
    Button: ({ children, ...props }: React.ComponentProps<"button">) => (
      <button {...props}>{children}</button>
    ),
    Container: ({
      children,
      fluid: _fluid,
      ...props
    }: React.ComponentProps<"div"> & { fluid?: boolean }) => {
      void _fluid;
      return <div {...props}>{children}</div>;
    },
    Nav: Object.assign(MockNav, { Link: MockNavLink }),
    Navbar: Object.assign(MockNavbar, { Brand: MockNavbarBrand }),
  };
});

describe("AdminShell", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders permitted navigation and redirects after logout", async () => {
    render(
      <AdminShell locale="en" dictionary={dictionary}>
        <div>Content</div>
      </AdminShell>,
    );
    expect(screen.getByText(dictionary.admin.product)).toBeVisible();
    expect(screen.getByText(dictionary.admin.nav.clients)).toBeVisible();
    expect(screen.getByText(dictionary.admin.nav.users)).toBeVisible();
    expect(screen.queryByText(dictionary.admin.nav.dashboard)).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.logout }));
    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/en/login"));
    expect(mockLogout).toHaveBeenCalled();
  });
});
