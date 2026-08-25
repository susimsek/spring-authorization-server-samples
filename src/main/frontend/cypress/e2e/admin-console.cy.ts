const signInAdmin = () => {
  cy.visit("/en/admin/");

  cy.env(["adminUsername", "adminPassword"], { log: false }).then(
    ({ adminUsername, adminPassword }) => {
      cy.get('input[name="username"]', { timeout: 15_000 })
        .should("be.visible")
        .type(String(adminUsername));
      cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      cy.get('button[type="submit"]').click();
    },
  );

  cy.location("pathname", { timeout: 20_000 }).should("match", /^\/en\/admin\/?$/);
  cy.contains("h1", "Dashboard", { timeout: 20_000 }).should("be.visible");
  cy.get(".admin-sidebar").should("exist");
};

describe("admin console", () => {
  it("signs in from /admin, keeps the callback URL clean and survives reload", () => {
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

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/admin\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");

    cy.reload();

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/admin\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");
  });

  it("opens every existing admin console resource page", () => {
    const pages = [
      ["/clients", "Clients"],
      ["/users", "Users"],
      ["/roles", "Roles"],
      ["/groups", "Groups"],
      ["/sessions", "Sessions"],
      ["/consents", "Consents"],
      ["/keys", "Keys"],
      ["/events", "Events"],
    ] as const;

    signInAdmin();
    cy.contains("h1", "Dashboard").should("be.visible");

    pages.forEach(([path, label]) => {
      cy.contains(".admin-sidebar a", label).click();
      cy.location("pathname", { timeout: 15_000 }).should(
        "match",
        new RegExp(`^/en/admin${path}/?$`),
      );
      cy.contains("h1", label, { timeout: 15_000 }).should("be.visible");
    });
  });

  it("opens the create forms and validates required fields", () => {
    signInAdmin();

    cy.contains(".admin-sidebar a", "Clients").click();
    cy.contains("a", "Create client").click();
    cy.location("pathname").should("match", /^\/en\/admin\/clients\/new\/?$/);
    cy.get('button[type="submit"]').click();
    cy.get(".invalid-feedback:visible").should("have.length.greaterThan", 0);

    cy.contains(".admin-sidebar a", "Users").click();
    cy.contains("a", "Create user").click();
    cy.location("pathname").should("match", /^\/en\/admin\/users\/new\/?$/);
    cy.get('input[name="username"]').clear();
    cy.get('input[name="password"]').clear();
    cy.get('button[type="submit"]').click();
    cy.get(".invalid-feedback:visible").should("have.length.greaterThan", 0);
  });

  it("opens existing client and user detail/edit pages", () => {
    signInAdmin();

    cy.contains(".admin-sidebar a", "Clients").click();
    cy.get("tbody tr", { timeout: 15_000 }).first().find("td").first().find("a").click();
    cy.location("pathname").should("match", /^\/en\/admin\/clients\/[^/]+\/settings\/?$/);

    cy.contains(".admin-sidebar a", "Users").click();
    cy.get("tbody tr", { timeout: 15_000 }).first().contains("a", "Edit").click();
    cy.location("pathname").should("match", /^\/en\/admin\/users\/[^/]+\/details\/?$/);
    cy.contains("Created at", { timeout: 15_000 }).should("be.visible");
    cy.contains("Updated at", { timeout: 15_000 }).should("be.visible");
  });

  it("recovers from a callback without a saved authorization transaction", () => {
    cy.visit("/en/admin/callback#code=stale-code&state=stale-state");

    cy.location("pathname", { timeout: 20_000 }).should("not.eq", "/en/admin/callback");
    cy.contains("The administration session could not be established.").should("not.exist");
  });

  it("opens and closes the responsive navigation", () => {
    cy.viewport(390, 844);
    signInAdmin();

    cy.get('button[aria-label="Toggle navigation"]').click();
    cy.get(".admin-sidebar").should("have.class", "is-open");
    cy.get(".admin-sidebar-backdrop").should("be.visible").click({ force: true });
    cy.get(".admin-sidebar").should("not.have.class", "is-open");
  });
});
