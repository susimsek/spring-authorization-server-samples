describe("account console", () => {
  it("restores the browser SSO session after an immediate reload", () => {
    cy.visit("/account");
    cy.env(["adminUsername", "adminPassword"], { log: false }).then(
      ({ adminUsername, adminPassword }) => {
        cy.get('input[name="username"]', { timeout: 15_000 })
          .should("be.visible")
          .type(String(adminUsername));
        cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      },
    );
    cy.get('button[type="submit"]').click();
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");

    cy.reload();

    cy.location("pathname", { timeout: 20_000 }).should(
      "match",
      /^\/(en|tr)\/account(?:\/personal-info)?\/?$/,
    );
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
  });

  it("signs in from /account, validates forms and keeps the callback URL clean", () => {
    cy.intercept("POST", "/oauth2/token").as("token");
    cy.intercept("GET", "/api/account/profile").as("profile");
    cy.visit("/account");
    cy.env(["adminUsername", "adminPassword"], { log: false }).then(
      ({ adminUsername, adminPassword }) => {
        cy.get('input[name="username"]', { timeout: 15_000 })
          .should("be.visible")
          .type(String(adminUsername));
        cy.get('input[name="password"]').type(String(adminPassword), { log: false });
      },
    );
    cy.get('button[type="submit"]').click();

    cy.wait("@token", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);
    cy.wait("@profile", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/account\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
    cy.get("#account-username").should("have.value", "admin");

    cy.get("#account-email").clear().type("not-an-email").blur();
    cy.get("#account-email").should("have.class", "is-invalid");
    cy.get("#account-email").clear().type("admin@example.test");
    cy.get("#account-first-name").clear().type("Admin");
    cy.get("#account-last-name").clear().type("User");
    cy.get('[data-cy="save-profile"]').click();
    cy.get('[data-cy="save-profile"]').should("be.disabled");

    cy.contains("a", /Security|Güvenlik/).click();
    cy.get("#account-new-password").type("short").blur();
    cy.get("#account-new-password").should("have.class", "is-invalid");
    cy.get("#account-new-password").clear().type("temporary-password-1");
    cy.get("#account-confirm-password").type("different-password").blur();
    cy.get("#account-confirm-password").should("have.class", "is-invalid");

    cy.contains("a", /Device activity|Cihaz etkinliği/).click();
    cy.get('[data-cy="session-row"]', { timeout: 10_000 }).should("have.length.at.least", 1);

    cy.contains("a", /Applications|Uygulamalar/).click();
    cy.location("pathname").should("match", /^\/(en|tr)\/account\/applications\/?$/);

    cy.get("@token.all").then((exchangesBeforeReload) => {
      cy.wrap(exchangesBeforeReload.length).as("exchangesBeforeReload");
    });
    cy.reload();
    cy.wait("@profile", { timeout: 20_000 }).its("response.statusCode").should("eq", 200);
    cy.get("@token.all").then((exchangesAfterReload) => {
      cy.get<number>("@exchangesBeforeReload").should("eq", exchangesAfterReload.length);
    });
    cy.location("search", { timeout: 20_000 }).should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
  });
});
