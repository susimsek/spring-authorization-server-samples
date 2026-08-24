describe("account console", () => {
  it("signs in from /account, validates forms and keeps the callback URL clean", () => {
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

    cy.location("pathname", { timeout: 20_000 }).should("match", /^\/(en|tr)\/account\/?$/);
    cy.location("search").should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
    cy.get('[data-cy="account-username"]').should("have.value", "admin");

    cy.get('[data-cy="email"]').clear().type("not-an-email").blur();
    cy.get('[data-cy="email"]').should("have.class", "is-invalid");
    cy.get('[data-cy="email"]').clear().type("admin@example.test");
    cy.get('[data-cy="first-name"]').clear().type("Admin");
    cy.get('[data-cy="last-name"]').clear().type("User");
    cy.get('[data-cy="save-profile"]').click();
    cy.get('[data-cy="profile-saved"]', { timeout: 10_000 }).should("be.visible");

    cy.contains("a", /Security|Güvenlik/).click();
    cy.get('[data-cy="new-password"]').type("short").blur();
    cy.get('[data-cy="new-password"]').should("have.class", "is-invalid");
    cy.get('[data-cy="new-password"]').clear().type("temporary-password-1");
    cy.get('[data-cy="confirm-password"]').type("different-password").blur();
    cy.get('[data-cy="confirm-password"]').should("have.class", "is-invalid");

    cy.contains("a", /Device activity|Cihaz etkinliği/).click();
    cy.get('[data-cy="session-row"]', { timeout: 10_000 }).should("have.length.at.least", 1);
    cy.contains(/Current session|Mevcut oturum/).should("be.visible");

    cy.contains("a", /Applications|Uygulamalar/).click();
    cy.location("pathname").should("match", /^\/(en|tr)\/account\/applications\/?$/);

    cy.reload();
    cy.location("search", { timeout: 20_000 }).should("eq", "");
    cy.location("hash").should("eq", "");
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
  });
});
