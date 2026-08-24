import {
  applyConsoleToken,
  clearConsoleAuth,
  setAdminAccess,
  setConsoleUsername,
} from "./auth-slice";
import { setLocale } from "./i18n-slice";
import { makeStore } from "./store";
import { setTheme } from "./theme-slice";

describe("Redux application store", () => {
  it("manages i18n and theme centrally", () => {
    const store = makeStore();

    store.dispatch(setLocale("tr"));
    store.dispatch(setTheme("dark"));

    expect(store.getState().i18n).toEqual({ locale: "tr" });
    expect(store.getState().theme).toEqual({ value: "dark" });
  });

  it("manages admin and account authentication independently", () => {
    const store = makeStore();

    store.dispatch(
      applyConsoleToken({
        console: "admin",
        accessToken: "admin-access",
        idToken: "admin-id",
        expiresAt: 123,
        sessionId: "sid-admin",
        subject: "admin",
        tokenParsed: { sub: "admin", sid: "sid-admin" },
        idTokenParsed: { sub: "admin" },
        refreshTokenParsed: { sub: "admin" },
      }),
    );
    store.dispatch(setConsoleUsername({ console: "admin", username: "administrator" }));
    store.dispatch(
      setAdminAccess({
        viewClients: true,
        manageClients: false,
        viewUsers: true,
        manageUsers: false,
        viewRoles: true,
        manageRoles: false,
        viewSessions: true,
        manageSessions: false,
        viewConsents: true,
        manageConsents: false,
        viewKeys: true,
        manageKeys: false,
      }),
    );

    expect(store.getState().auth.admin).toMatchObject({
      accessToken: "admin-access",
      idToken: "admin-id",
      authenticated: true,
      sessionId: "sid-admin",
      username: "administrator",
      access: { viewClients: true },
    });
    expect(store.getState().auth.account.authenticated).toBe(false);

    store.dispatch(clearConsoleAuth({ console: "admin", loggingOut: true }));
    expect(store.getState().auth.admin).toMatchObject({
      accessToken: null,
      authenticated: false,
      isLoggingOut: true,
      username: null,
      access: null,
    });
  });
});
