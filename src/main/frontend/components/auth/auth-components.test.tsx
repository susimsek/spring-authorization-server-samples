import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import axios from "axios";

import dictionary from "@/i18n/dictionaries/en.json";

const navigation = {
  pathname: "/en/login",
  push: jest.fn(),
  searchParams: new URLSearchParams(),
};

jest.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ push: navigation.push }),
  useSearchParams: () => navigation.searchParams,
}));

jest.mock("axios", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    isCancel: jest.fn(),
  },
}));

import { AuthLayout } from "./AuthLayout";
import { ConsentForm } from "./ConsentForm";
import { ErrorView } from "./ErrorView";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { LoginForm } from "./LoginForm";
import { THEME_STORAGE_KEY } from "./theme";
import { ThemeSwitcher } from "./ThemeSwitcher";

const mockedAxios = axios as unknown as { get: jest.Mock; isCancel: jest.Mock };

const consent = {
  clientId: "console-client",
  state: "request-state",
  scopes: ["openid", "custom.scope"],
  previouslyApprovedScopes: ["profile"],
  principalName: "admin",
  userCode: "user-code",
  requestUri: "/oauth2/authorize",
};

function installMatchMedia(matches = false) {
  const listeners = new Set<(event: MediaQueryListEvent) => void>();
  const mediaQuery = {
    matches,
    addEventListener: jest.fn((_: string, listener: (event: MediaQueryListEvent) => void) => {
      listeners.add(listener);
    }),
    removeEventListener: jest.fn((_: string, listener: (event: MediaQueryListEvent) => void) => {
      listeners.delete(listener);
    }),
    dispatch(nextMatches: boolean) {
      Object.assign(mediaQuery, { matches: nextMatches });
      listeners.forEach((listener) => listener({ matches: nextMatches } as MediaQueryListEvent));
    },
  };
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: jest.fn(() => mediaQuery),
  });
  return mediaQuery;
}

describe("authentication components", () => {
  beforeEach(() => {
    navigation.pathname = "/en/login";
    navigation.searchParams = new URLSearchParams();
    navigation.push.mockReset();
    mockedAxios.get.mockReset();
    mockedAxios.isCancel.mockReset();
    window.history.replaceState(null, "", "/en/login");
    localStorage.clear();
    document.documentElement.removeAttribute("data-bs-theme");
    installMatchMedia();
  });

  it("renders the layout, navbar, and login feedback", () => {
    navigation.searchParams = new URLSearchParams("error&logout");

    render(
      <AuthLayout locale="en" dictionary={dictionary}>
        <LoginForm dictionary={dictionary} />
      </AuthLayout>,
    );

    expect(screen.getByRole("link", { name: dictionary.brand.product })).toHaveAttribute(
      "href",
      "/en/login",
    );
    expect(screen.getByText(dictionary.login.invalidCredentials)).toBeVisible();
    expect(screen.getByText(dictionary.login.loggedOut)).toBeVisible();
    expect(screen.getByRole("button", { name: dictionary.navbar.language })).toBeVisible();
    expect(screen.getByRole("button", { name: dictionary.theme.label })).toBeVisible();
  });

  it("validates login fields and submits a valid form", async () => {
    const submit = jest
      .spyOn(HTMLFormElement.prototype, "submit")
      .mockImplementation(() => undefined);
    render(<LoginForm dictionary={dictionary} />);

    const username = screen.getByLabelText(dictionary.login.username);
    const password = screen.getByLabelText(dictionary.login.password);
    fireEvent.blur(username);
    fireEvent.blur(password);

    const requiredMessages = await screen.findAllByText(
      dictionary.admin.common.validation.required,
    );
    expect(requiredMessages).toHaveLength(2);
    requiredMessages.forEach((message) => expect(message).toBeVisible());
    expect(password).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: dictionary.login.showPassword }));
    expect(password).toHaveAttribute("type", "text");
    expect(screen.getByRole("button", { name: dictionary.login.hidePassword })).toBeVisible();

    fireEvent.change(username, { target: { value: "admin" } });
    fireEvent.change(password, { target: { value: "password" } });
    fireEvent.submit(
      screen.getByRole("button", { name: dictionary.login.submit }).closest("form")!,
    );

    await waitFor(() => expect(submit).toHaveBeenCalled());
    submit.mockRestore();
  });

  it("loads a consent request, submits selected scopes, and shows scope validation", async () => {
    const submit = jest
      .spyOn(HTMLFormElement.prototype, "submit")
      .mockImplementation(() => undefined);
    mockedAxios.get.mockResolvedValueOnce({ data: consent });

    render(<ConsentForm dictionary={dictionary} />);

    expect(screen.getByRole("status")).toBeVisible();
    expect(await screen.findByText(consent.clientId)).toBeVisible();
    expect(mockedAxios.get).toHaveBeenCalledWith("/api/authorization/consent", {
      signal: expect.any(AbortSignal),
    });
    expect(screen.getByDisplayValue("profile")).toHaveAttribute("type", "hidden");
    expect(screen.getAllByDisplayValue("user-code")).toHaveLength(2);
    expect(screen.getAllByDisplayValue("user-code")[0]).toHaveAttribute("type", "hidden");
    expect(screen.getByText(dictionary.consent.scopeDescriptions.openid)).toBeVisible();
    expect(screen.getByText(dictionary.consent.scopeDefault)).toBeVisible();

    fireEvent.click(screen.getByLabelText("openid"));
    fireEvent.click(screen.getByLabelText("custom.scope"));
    expect(await screen.findByText(dictionary.consent.scopeRequired)).toBeVisible();

    fireEvent.click(screen.getByLabelText("openid"));
    fireEvent.submit(
      screen.getByRole("button", { name: dictionary.consent.submit }).closest("form")!,
    );
    await waitFor(() => expect(submit).toHaveBeenCalled());
    submit.mockRestore();
  });

  it("handles consent request failures and ignores cancellation", async () => {
    mockedAxios.get.mockRejectedValueOnce(new Error("invalid request"));
    mockedAxios.isCancel.mockReturnValue(false);
    navigation.pathname = "/tr/consent";
    render(<ConsentForm dictionary={dictionary} />);

    expect(await screen.findByText(dictionary.consent.invalidRequest)).toBeVisible();

    mockedAxios.get.mockRejectedValueOnce(new Error("aborted"));
    mockedAxios.isCancel.mockReturnValue(true);
    const { container } = render(<ConsentForm dictionary={dictionary} />);
    await waitFor(() => expect(mockedAxios.isCancel).toHaveBeenCalled());
    expect(container).toHaveTextContent("Loading");
  });

  it("uses requested error messages and falls back to server errors", () => {
    navigation.searchParams = new URLSearchParams("type=invalid_scope");
    const { rerender } = render(<ErrorView dictionary={dictionary} />);
    expect(screen.getByText(dictionary.error.types.invalid_scope.title)).toBeVisible();

    navigation.searchParams = new URLSearchParams("type=unknown");
    rerender(<ErrorView dictionary={dictionary} />);
    expect(screen.getByText(dictionary.error.types.server_error.title)).toBeVisible();

    navigation.searchParams = new URLSearchParams();
    rerender(<ErrorView dictionary={dictionary} />);
    expect(screen.getByText(dictionary.error.types.server_error.description)).toBeVisible();
  });

  it("switches language while preserving the query string and normalizing paths", () => {
    window.history.replaceState(null, "", "/en/login?continue=%2Fconsole");
    render(<LanguageSwitcher locale="en" label={dictionary.navbar.language} />);

    fireEvent.click(screen.getByRole("button", { name: dictionary.navbar.language }));
    fireEvent.click(screen.getByRole("button", { name: "Türkçe" }));
    expect(document.cookie).toContain("AUTH_LOCALE=tr");
    expect(navigation.push).toHaveBeenCalledWith("/tr/login?continue=%2Fconsole");

    navigation.pathname = "profile";
    render(<LanguageSwitcher locale="tr" label={dictionary.navbar.language} />);
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.navbar.language })[1]);
    fireEvent.click(screen.getAllByRole("button", { name: "English" })[1]);
    expect(navigation.push).toHaveBeenLastCalledWith("/en/profile?continue=%2Fconsole");
  });

  it("persists themes, applies system preferences, and cleans up media subscriptions", () => {
    const mediaQuery = installMatchMedia(false);
    const { unmount } = render(<ThemeSwitcher dictionary={dictionary} />);
    expect(document.documentElement).toHaveAttribute("data-bs-theme", "light");

    fireEvent.click(screen.getByRole("button", { name: dictionary.theme.label }));
    fireEvent.click(screen.getByRole("button", { name: dictionary.theme.dark }));
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");
    expect(document.documentElement).toHaveAttribute("data-bs-theme", "dark");

    act(() => window.dispatchEvent(new StorageEvent("storage", { key: THEME_STORAGE_KEY })));
    fireEvent.click(screen.getByRole("button", { name: dictionary.theme.label }));
    fireEvent.click(screen.getByRole("button", { name: dictionary.theme.system }));
    act(() => mediaQuery.dispatch(true));
    expect(document.documentElement).toHaveAttribute("data-bs-theme", "dark");

    unmount();
    expect(mediaQuery.removeEventListener).toHaveBeenCalledWith("change", expect.any(Function));
  });
});
