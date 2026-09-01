describe("admin login", () => {
  const sessionCount = () =>
    cy.window().then((window) => {
      const stored = window.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin");
      if (!stored) throw new Error("Expected a persisted admin token");
      const accessToken = (JSON.parse(stored ?? "{}") as { accessToken?: string }).accessToken;
      if (!accessToken) throw new Error("Expected a persisted admin access token");
      return cy
        .request({
          headers: { Authorization: `Bearer ${accessToken}` },
          url: "/api/admin/sessions?size=100",
        })
        .its("body.page.totalElements");
    });

  it("signs in from /admin and keeps the URL clean after reload", () => {
    cy.intercept("GET", "/account/avatar").as("privateAvatar");
    cy.intercept("POST", "/oauth2/token").as("initialToken");
    cy.intercept("GET", "/api/admin/whoami").as("whoami");
    cy.visit("/admin");

    cy.env(["adminUsername", "adminPassword"], { log: false }).then(
      ({ adminUsername, adminPassword }) => {
        cy.get('input[name="username"]', { timeout: 15_000 })
          .should("be.visible")
          .type(String(adminUsername));
        cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      },
    );
    cy.get('button[type="submit"]').click();

    cy.wait("@initialToken", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);
    cy.wait("@whoami", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/admin\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");
    cy.get(".console-user-avatar-image", { timeout: 20_000 })
      .should("be.visible")
      .and("have.attr", "src")
      .and("include", "/avatars/");
    sessionCount().as("sessionsBeforeReload");

    cy.reload();

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/admin\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");
    cy.get("@privateAvatar.all").should("have.length", 0);
    sessionCount().then((sessionsAfterReload) => {
      cy.get<number>("@sessionsBeforeReload").should("eq", sessionsAfterReload);
    });
  });

  it("reuses one browser session for Admin and Account, then clears both token sets on logout", () => {
    cy.intercept("POST", "/oauth2/token").as("tokenExchange");
    cy.visit("/en/admin/");

    cy.env(["adminUsername", "adminPassword"], { log: false }).then(
      ({ adminUsername, adminPassword }) => {
        cy.get('input[name="username"]', { timeout: 15_000 })
          .should("be.visible")
          .type(String(adminUsername));
        cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      },
    );
    cy.get('button[type="submit"]').click();

    cy.wait("@tokenExchange", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);
    sessionCount().as("sessionsAfterAdminLogin");
    cy.window().then((window) => {
      const stored = window.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin");
      const refreshToken = JSON.parse(stored ?? "{}").refreshToken;
      expect(refreshToken).to.be.a("string");
      expect(refreshToken).to.have.length.greaterThan(0);
    });

    cy.get(".console-user-toggle").click();
    cy.contains(".console-user-menu a", /Account Console|Hesap Konsolu/).click();

    cy.wait("@tokenExchange", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);
    cy.location("pathname", { timeout: 20_000 }).should(
      "match",
      /^\/(en|tr)\/account\/personal-info\/?$/,
    );
    cy.get('input[name="username"]').should("not.exist");
    cy.window().then((window) => {
      const admin = JSON.parse(window.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin") ?? "{}");
      const account = JSON.parse(window.localStorage.getItem("AUTH_CONSOLE_TOKEN:account") ?? "{}");
      expect(admin.refreshToken).to.be.a("string");
      expect(admin.refreshToken).to.have.length.greaterThan(0);
      expect(account.refreshToken).to.be.a("string");
      expect(account.refreshToken).to.have.length.greaterThan(0);
      expect(account.refreshToken).not.to.equal(admin.refreshToken);
    });
    sessionCount().then((sessionsAfterAccountLogin) => {
      cy.get<number>("@sessionsAfterAdminLogin").should("eq", sessionsAfterAccountLogin);
    });

    cy.get(".console-user-toggle").click();
    cy.contains(".console-user-menu .dropdown-item", /Sign out|Çıkış/).click();
    cy.get('input[name="username"]', { timeout: 20_000 }).should("be.visible");
    cy.window().then((window) => {
      expect(window.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin")).to.equal(null);
      expect(window.localStorage.getItem("AUTH_CONSOLE_TOKEN:account")).to.equal(null);
    });
    cy.request({ url: "/oidc/session-status", failOnStatusCode: false })
      .its("status")
      .should("eq", 401);
  });

  it("rejects a callback without a saved authorization transaction", () => {
    cy.visit("/en/admin/callback#code=stale-code&state=stale-state");

    cy.location("pathname").should("eq", "/en/admin/callback");
    cy.location("hash").should("eq", "");
    cy.contains("The administration session could not be established.").should("be.visible");
  });
});
