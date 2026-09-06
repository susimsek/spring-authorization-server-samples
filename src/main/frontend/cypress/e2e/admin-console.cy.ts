const signInAdmin = () => {
  cy.visit("/admin/");

  cy.env(["adminUsername", "adminPassword"], { log: false }).then(
    ({ adminUsername, adminPassword }) => {
      cy.get('input[name="username"]', { timeout: 15_000 })
        .should("be.visible")
        .type(String(adminUsername));
      cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      cy.get('button[type="submit"]').click();
    },
  );

  cy.location("pathname", { timeout: 20_000 }).should("match", /^\/admin\/?$/);
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

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/admin\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");

    cy.reload();

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/admin\/?$/);
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
      cy.location("pathname", { timeout: 15_000 }).should("match", new RegExp(`^/admin${path}/?$`));
      cy.contains("h1", label, { timeout: 15_000 }).should("be.visible");
    });
  });

  it("opens each settings section with its own route and save action", () => {
    const sections = [
      ["General", "/admin/settings", null],
      ["Login", "/admin/settings/login", "Save settings"],
      ["Email", "/admin/settings/email", "Save email settings"],
      ["Password policy", "/admin/settings/password-policy", "Save settings"],
      ["OTP policy", "/admin/settings/otp-policy", "Save settings"],
      ["Brute force", "/admin/settings/brute-force", "Save settings"],
      ["Sessions", "/admin/settings/sessions", "Save settings"],
    ] as const;

    signInAdmin();
    cy.contains(".admin-sidebar a", "Settings").click();
    cy.location("pathname").should("match", /^\/admin\/settings\/?$/);

    sections.forEach(([label, path, saveLabel]) => {
      cy.contains(".admin-settings-nav a", label).click();
      cy.location("pathname", { timeout: 15_000 }).should("eq", path);
      cy.get('.admin-settings-nav a[aria-current="page"]').should("contain.text", label);
      if (saveLabel) cy.contains("button", saveLabel, { timeout: 15_000 }).should("be.visible");
    });
  });

  it("opens the create forms and validates required fields", () => {
    signInAdmin();

    cy.contains(".admin-sidebar a", "Clients").click();
    cy.contains("a", "Create client").click();
    cy.location("pathname").should("match", /^\/admin\/clients\/new\/?$/);
    cy.contains("button", "Next").click();
    cy.contains("button", "Next").click();
    cy.get('button[type="submit"]').click();
    cy.get(".invalid-feedback:visible").should("have.length.greaterThan", 0);

    cy.contains(".admin-sidebar a", "Users").click();
    cy.contains("a", "Create user").click();
    cy.location("pathname").should("match", /^\/admin\/users\/new\/?$/);
    cy.get('input[name="username"]').clear();
    cy.get('input[name="password"]').clear();
    cy.get('button[type="submit"]').click();
    cy.get(".invalid-feedback:visible").should("have.length.greaterThan", 0);
  });

  it("opens existing client and user detail/edit pages", () => {
    signInAdmin();

    cy.contains(".admin-sidebar a", "Clients").click();
    cy.get("tbody tr", { timeout: 15_000 }).first().find("td").first().find("a").click();
    cy.location("pathname").should("match", /^\/admin\/clients\/[^/]+\/settings\/?$/);

    cy.contains(".admin-sidebar a", "Users").click();
    cy.get("tbody tr", { timeout: 15_000 }).first().find('button[aria-label$=" actions"]').click();
    cy.get(".dropdown-menu.show").contains("a", "Edit").click();
    cy.location("pathname").should("match", /^\/admin\/users\/[^/]+\/details\/?$/);
    cy.contains("Created at", { timeout: 15_000 }).should("be.visible");
    cy.contains("Updated at", { timeout: 15_000 }).should("be.visible");
    cy.get('input[name="firstName"]').should("exist");
    cy.get('input[name="lastName"]').should("exist");
    cy.contains("a", "Credentials").click();
    cy.contains("Required actions", { timeout: 15_000 }).should("be.visible");
    cy.contains("Temporary password").should("be.visible");
    cy.contains("a", "Sessions").click();
    cy.contains("button", "Sign out all sessions", { timeout: 15_000 }).should("be.visible");
  });

  it("rejects a callback without a saved authorization transaction", () => {
    cy.visit("/admin/callback#code=stale-code&state=stale-state");

    cy.location("pathname", { timeout: 20_000 }).should("eq", "/admin/callback");
    cy.location("hash").should("eq", "");
    cy.contains("The administration session could not be established.").should("be.visible");
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
