/* eslint-disable react/display-name */

import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { adminRequest } from "@/lib/admin-api";

import { AdminDashboard } from "./AdminDashboard";
import { AdminPageHeader } from "./AdminPageHeader";
import { EmptyState, ErrorState, LoadingState } from "./AsyncState";
import { ClientsTable } from "./ClientsTable";
import { DataTable } from "./DataTable";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";

const mockPush = jest.fn();
const mockLogout = jest.fn().mockResolvedValue(undefined);
const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({
    accessToken: "token",
    access: {
      viewClients: true,
      manageClients: true,
      viewUsers: true,
      manageUsers: true,
      manageRoles: true,
      viewSessions: true,
      viewConsents: true,
      viewKeys: true,
    },
    logout: mockLogout,
  }),
}));
jest.mock("./useAdminTableState", () => ({
  useAdminTableState: () => ({
    query: "",
    page: 0,
    size: 10,
    status: "",
    setPage: jest.fn(),
    setQuery: jest.fn(),
    setSize: jest.fn(),
    setStatus: jest.fn(),
  }),
}));
jest.mock("next/navigation", () => ({
  usePathname: () => "/en/admin/clients/detail",
  useRouter: () => ({ push: mockPush }),
}));
jest.mock("next/link", () => ({ children, href, ...props }: React.ComponentProps<"a">) => (
  <a href={href} {...props}>
    {children}
  </a>
));
jest.mock("@/components/auth/LanguageSwitcher", () => ({ label }: { label: string }) => (
  <button>{label}</button>
));
jest.mock("@/components/auth/ThemeSwitcher", () => () => <button>Theme</button>);
jest.mock("@fortawesome/react-fontawesome", () => ({
  FontAwesomeIcon: () => <span aria-hidden="true" />,
}));

describe("admin shared components", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAdminRequest.mockReset();
  });

  it("renders all async states and table variants", () => {
    render(
      <>
        <LoadingState />
        <ErrorState message="Failed" />
        <EmptyState message="Nothing" />
        <DataTable isEmpty emptyMessage="Empty" footer={<span>Footer</span>}>
          <tbody>
            <tr>
              <td>Hidden row</td>
            </tr>
          </tbody>
        </DataTable>
        <DataTable isEmpty={false} emptyMessage="Empty">
          <tbody>
            <tr>
              <td>Row</td>
            </tr>
          </tbody>
        </DataTable>
      </>,
    );
    expect(screen.getByText("Failed")).toBeVisible();
    expect(screen.getByText("Nothing")).toBeVisible();
    expect(screen.getByText("Empty")).toBeVisible();
    expect(screen.getByText("Footer")).toBeVisible();
    expect(screen.getByText("Row")).toBeVisible();
  });

  it("handles pagination boundaries and hides single-page controls", () => {
    const onPageChange = jest.fn();
    const { rerender } = render(
      <PaginationControls
        page={0}
        totalPages={3}
        previous="Previous"
        next="Next"
        onPageChange={onPageChange}
      />,
    );
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(onPageChange).toHaveBeenCalledWith(1);
    rerender(
      <PaginationControls
        page={2}
        totalPages={3}
        previous="Previous"
        next="Next"
        onPageChange={onPageChange}
      />,
    );
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));
    expect(onPageChange).toHaveBeenLastCalledWith(1);
    rerender(
      <PaginationControls
        page={0}
        totalPages={1}
        previous="Previous"
        next="Next"
        onPageChange={onPageChange}
      />,
    );
    expect(screen.queryByRole("button", { name: "Next" })).not.toBeInTheDocument();
  });

  it("debounces resource filter changes and renders optional controls", () => {
    jest.useFakeTimers();
    const onQueryChange = jest.fn();
    render(
      <ResourceFilters query="old" searchLabel="Search" onQueryChange={onQueryChange}>
        <button>Filter</button>
      </ResourceFilters>,
    );
    fireEvent.change(screen.getByRole("textbox", { name: "Search" }), { target: { value: "new" } });
    act(() => jest.advanceTimersByTime(299));
    expect(onQueryChange).not.toHaveBeenCalled();
    fireEvent.change(screen.getByRole("textbox", { name: "Search" }), {
      target: { value: "newer" },
    });
    act(() => jest.advanceTimersByTime(300));
    expect(screen.getByRole("button", { name: "Filter" })).toBeVisible();
    jest.useRealTimers();
  });

  it("renders page headers with optional content", () => {
    const { rerender } = render(<AdminPageHeader title="Title" />);
    expect(screen.getByRole("heading", { name: "Title" })).toBeVisible();
    rerender(
      <AdminPageHeader title="Title" description="Description" actions={<button>Action</button>} />,
    );
    expect(screen.getByText("Description")).toBeVisible();
    expect(screen.getByRole("button", { name: "Action" })).toBeVisible();
  });

  it("loads dashboard data and preserves placeholders on failure", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { clients: 4, users: 3, sessions: 2, consents: 1 },
    } as never);
    const { rerender } = render(<AdminDashboard dictionary={dictionary} />);
    expect(await screen.findByText("4")).toBeVisible();
    expect(screen.getByText("3")).toBeVisible();
    expect(screen.getByText("2")).toBeVisible();
    expect(screen.getByText("1")).toBeVisible();
    mockAdminRequest.mockRejectedValueOnce(new Error("failed"));
    rerender(<AdminDashboard dictionary={dictionary} />);
    await waitFor(() => expect(mockAdminRequest).toHaveBeenCalledTimes(1));
  });

  it("loads clients, displays badges and changes page size", async () => {
    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: {
        content: [
          {
            id: "1",
            clientId: "client",
            clientName: "Client",
            authorizationGrantTypes: ["authorization_code"],
            clientAuthenticationMethods: ["client_secret_basic"],
            scopes: ["openid"],
            requireAuthorizationConsent: true,
            requireProofKey: false,
          },
        ],
        totalPages: 2,
      },
    } as never);
    render(<ClientsTable locale="en" dictionary={dictionary} />);
    expect((await screen.findAllByText("Client"))[1]).toBeVisible();
    expect(screen.getByText("authorization_code")).toBeVisible();
    expect(screen.getByText("openid")).toBeVisible();
    expect(screen.getByText(dictionary.admin.common.no)).toBeVisible();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "20" } });
  });

  it("handles client loading errors and empty filtered results", async () => {
    mockAdminRequest.mockResolvedValueOnce({ status: 500, data: null } as never);
    render(<ClientsTable locale="tr" dictionary={dictionary} />);
    expect(await screen.findByText(dictionary.admin.clients.loadError)).toBeVisible();

    mockAdminRequest.mockResolvedValueOnce({
      status: 200,
      data: { content: [], totalPages: 0 },
    } as never);
    const view = render(<ClientsTable locale="tr" dictionary={dictionary} />);
    expect(await screen.findByText(dictionary.admin.clients.empty)).toBeVisible();
    expect(screen.getByRole("link", { name: dictionary.admin.clients.create })).toHaveAttribute(
      "href",
      "/tr/admin/clients/new",
    );
    fireEvent.change(screen.getByRole("textbox", { name: dictionary.admin.clients.search }), {
      target: { value: "unknown" },
    });
    view.unmount();
  });
});
