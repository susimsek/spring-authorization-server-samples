describe("admin login", () => {
  it("signs in from /admin and keeps the URL clean after reload", () => {
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
  it("recovers from a callback without a saved authorization transaction", () => {
    cy.visit("/en/admin/callback#code=stale-code&state=stale-state");

    cy.location("pathname", { timeout: 20_000 }).should("not.eq", "/en/admin/callback");
    cy.contains("The administration session could not be established.").should("not.exist");
  });
});
