describe("social login discovery", () => {
  beforeEach(() => {
    cy.intercept("GET", "/api/auth/social-providers", {
      statusCode: 200,
      body: [
        {
          provider: "acme-google",
          providerType: "google",
          iconKey: "google",
          configured: true,
        },
        {
          provider: "github",
          providerType: "github",
          iconKey: "github",
          configured: false,
        },
      ],
    }).as("socialProviders");
    cy.visit("/login");
    cy.wait("@socialProviders");
  });

  it("renders configured and unavailable providers with the correct redirect target", () => {
    cy.get('.social-login-button[aria-label*="Google"]').should(
      "have.attr",
      "href",
      "/oauth2/authorization/acme-google",
    );
    cy.get('.social-login-button[aria-label*="Google"]').should(
      "have.attr",
      "aria-disabled",
      "false",
    );
    cy.get('.social-login-button[aria-label*="GitHub"]').should("not.have.attr", "href");
    cy.get('.social-login-button[aria-label*="GitHub"]').should(
      "have.attr",
      "aria-disabled",
      "true",
    );
  });

  it("locks every provider and shows a spinner while redirecting", () => {
    cy.get('.social-login-button[aria-label*="Google"]').then(($button) => {
      $button[0].addEventListener("click", (event) => event.preventDefault(), {
        once: true,
      });
    });
    cy.get('.social-login-button[aria-label*="Google"]').click();
    cy.get('.social-login-button[aria-label*="Google"]')
      .should("have.attr", "aria-disabled", "true")
      .find(".spinner-border")
      .should("be.visible");
    cy.get('.social-login-button[aria-label*="GitHub"]').should(
      "have.attr",
      "aria-disabled",
      "true",
    );
  });
});

describe("social account linking", () => {
  it("removes a linked provider through the authenticated account UI", () => {
    cy.intercept("GET", "/api/account/social-links", {
      statusCode: 200,
      body: [
        {
          provider: "google",
          displayName: "Google",
          iconKey: "google",
          linked: true,
          configured: true,
          enabled: true,
        },
      ],
    }).as("socialLinks");
    cy.intercept("DELETE", "/api/account/social-links/google", { statusCode: 204 }).as("unlink");

    cy.visitAccount("/security/");
    cy.wait("@socialLinks");
    cy.contains("button", /Remove|Kaldır/).click();
    cy.contains('[role="dialog"] button', /Remove|Kaldır/).click();
    cy.wait("@unlink");
    cy.contains(/Not connected|Bağlı değil/).should("be.visible");
  });
});
