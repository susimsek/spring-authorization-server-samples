import en from "../../locales/en/common.json";
import tr from "../../locales/tr/common.json";

describe("static SPA routing and cookie locale", () => {
  ["/admin/missing/page", "/account/missing", "/404.html"].forEach((path) => {
    it(`shows the custom localized 404 at ${path} and supports recovery and history`, () => {
      cy.setCookie("locale", "tr");
      cy.visit(`${path}?type=server_error#missing`);
      cy.get('[data-cy="not-found"]').should("contain", "404");
      cy.contains("h1", tr.error.types.not_found.title).should("be.visible");
      cy.window().then((win) => {
        (win as Window & { spaMarker?: string }).spaMarker = "404";
      });
      cy.contains(".dropdown-toggle", "Türkçe").click();
      cy.contains(".dropdown-item", "English").click();
      cy.contains("h1", en.error.types.not_found.title).should("be.visible");
      cy.location("pathname").should("eq", path);
      cy.location("search").should("eq", "?type=server_error");
      cy.location("hash").should("eq", "#missing");
      cy.contains("a", en.error.backToHome).click();
      cy.get('input[name="username"]').should("be.visible");
      cy.window().then((win) =>
        expect((win as Window & { spaMarker?: string }).spaMarker).to.eq("404"),
      );
      cy.go("back");
      cy.location("pathname").should("eq", path);
      cy.contains("h1", en.error.types.not_found.title).should("be.visible");
      cy.reload();
      cy.contains("h1", en.error.types.not_found.title).should("be.visible");
      cy.getCookie("locale").its("value").should("eq", "en");
    });
  });

  (["clients", "roles"] as const).forEach((resource) => {
    it(`loads runtime ${resource} details on direct navigation, reload and back/forward`, () => {
      cy.visitAdmin(`/${resource}`);
      cy.window().then((win) => {
        const tokens = JSON.parse(win.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin")!);
        cy.request<{ content: Array<{ id: string; name: string; clientId: string }> }>({
          url: `/api/admin/${resource}?size=20`,
          headers: { Authorization: `Bearer ${tokens.accessToken}` },
        }).then(({ body }) => {
          expect(body.content.length).to.be.greaterThan(0);
          const item = body.content[0];
          const id = encodeURIComponent(resource === "clients" ? item.id : item.name);
          const path = `/admin/${resource}/${id}`;
          const assertDetail = () =>
            resource === "clients"
              ? cy.get('input[name="clientId"]').should("have.value", item.clientId)
              : cy.contains("h1", item.name).should("be.visible");
          cy.intercept({ method: "GET", pathname: `/api/admin/${resource}/${id}` }).as("detail");
          cy.visit(path);
          cy.wait("@detail").its("response.statusCode").should("eq", 200);
          assertDetail();
          cy.reload();
          cy.wait("@detail").its("response.statusCode").should("eq", 200);
          cy.location("pathname").should("eq", path);
          assertDetail();
          cy.window().then((win) => {
            (win as Window & { spaMarker?: string }).spaMarker = "detail";
          });
          cy.get(`.admin-sidebar a[href="/admin/${resource}"]`).click();
          cy.location("pathname").should("eq", `/admin/${resource}`);
          cy.go("back");
          cy.location("pathname").should("eq", path);
          assertDetail();
          cy.go("forward");
          cy.location("pathname").should("eq", `/admin/${resource}`);
          cy.get("table").should("be.visible");
          cy.window().then((win) =>
            expect((win as Window & { spaMarker?: string }).spaMarker).to.eq("detail"),
          );
        });
      });
    });
  });

  it("uses the selected locale for Account dates and preserves it across navigation and refresh", () => {
    cy.intercept("GET", "/api/account/profile").as("accountProfile");
    cy.visitAccount("/personal-info");
    cy.wait("@accountProfile").then(({ response }) => {
      const createdAt = response!.body.createdAt as string;
      cy.window().then((win) => {
        const browser = win as Window & typeof globalThis;
        cy.contains(new browser.Date(createdAt).toLocaleString("en")).should("be.visible");
        cy.contains(".dropdown-toggle", "English").click();
        cy.contains(".dropdown-item", "Türkçe").click();
        cy.contains(new browser.Date(createdAt).toLocaleString("tr")).should("be.visible");
      });
    });
    cy.location("pathname").should("eq", "/account/personal-info");
    cy.intercept("GET", "/api/account/sessions?*").as("accountSessions");
    cy.get('.account-sidebar a[href="/account/sessions"]').click();
    cy.wait("@accountSessions").then(({ response }) => {
      const session = response!.body.content[0];
      cy.window().then((win) => {
        const browser = win as Window & typeof globalThis;
        [session.createdAt, session.lastAccessedAt, session.expiresAt].forEach((value: string) => {
          cy.contains(new browser.Date(value).toLocaleString("tr")).should("be.visible");
        });
      });
    });
    cy.reload();
    cy.location("pathname").should("eq", "/account/sessions");
    cy.get("html").should("have.attr", "lang", "tr");
    cy.getCookie("locale").its("value").should("eq", "tr");
    cy.get('[data-cy="session-row"]').should("be.visible");
  });
  it("changes language without navigation, preserves form input and survives reload", () => {
    cy.setCookie("locale", "tr");
    cy.visit("/login?continue=account#form");
    cy.get("html").should("have.attr", "lang", "tr");
    cy.get('input[name="username"]').type("draft-user");
    cy.window().then((win) => {
      (win as Window & { spaMarker?: string }).spaMarker = "same-page";
    });
    cy.contains(".dropdown-toggle", "Türkçe").click();
    cy.contains(".dropdown-item", "English").click();
    cy.getCookie("locale").its("value").should("eq", "en");
    cy.get("html").should("have.attr", "lang", "en");
    cy.get('input[name="username"]').should("have.value", "draft-user");
    cy.location("pathname").should("eq", "/login");
    cy.location("search").should("eq", "?continue=account");
    cy.location("hash").should("eq", "#form");
    cy.window().then((win) =>
      expect((win as Window & { spaMarker?: string }).spaMarker).to.eq("same-page"),
    );
    cy.reload();
    cy.get("html").should("have.attr", "lang", "en");
    cy.contains(".dropdown-toggle", "English").should("be.visible");
  });

  it("resolves a runtime user id, fetches the API and refreshes the same deep link", () => {
    cy.visitAdmin("/users");
    cy.window().then((win) => {
      const tokens = JSON.parse(win.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin")!);
      cy.request({
        url: "/api/admin/users?size=20",
        headers: { Authorization: `Bearer ${tokens.accessToken}` },
      }).then(({ body }) => {
        const id = String(body.content[0].id);
        cy.intercept("GET", `/api/admin/users/${id}`).as("user");
        cy.visit(`/admin/users/${id}`);
        cy.wait("@user").its("response.statusCode").should("eq", 200);
        cy.get('input[name="username"]').should("not.have.value", "");
        cy.contains(".dropdown-toggle", "English").click();
        cy.contains(".dropdown-item", "Türkçe").click();
        cy.location("pathname").should("eq", `/admin/users/${id}`);
        cy.getCookie("locale").its("value").should("eq", "tr");
        cy.reload();
        cy.wait("@user").its("response.statusCode").should("eq", 200);
        cy.location("pathname").should("eq", `/admin/users/${id}`);
        cy.get("html").should("have.attr", "lang", "tr");
      });
    });
  });

  it("serves arbitrary frontend deep links but never replaces API or assets with SPA HTML", () => {
    ["/admin/users/123", "/admin/clients/abc", "/admin/roles/42"].forEach((url) => {
      cy.request({ url, headers: { Accept: "text/html" }, followRedirect: false }).then(
        (response) => {
          expect(response.status).to.eq(200);
          expect(response.body).to.contain("/_next/");
        },
      );
    });
    cy.request({
      url: "/api/admin/users",
      failOnStatusCode: false,
      headers: { Accept: "application/json" },
    })
      .its("status")
      .should("eq", 401);
    cy.request({ url: "/admin/missing.js", failOnStatusCode: false, followRedirect: false }).then(
      (response) => expect(String(response.body)).not.to.contain("/_next/static/"),
    );
    cy.request("/.well-known/openid-configuration")
      .its("body.authorization_endpoint")
      .should("contain", "/oauth2/authorize");
  });
});
