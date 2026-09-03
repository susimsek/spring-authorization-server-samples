/* eslint-disable @typescript-eslint/no-namespace */
/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      loginAdmin(locale?: "en" | "tr"): Chainable<void>;
      visitAdmin(path?: string, locale?: "en" | "tr"): Chainable<void>;
    }
  }
}

Cypress.Commands.add("loginAdmin", (locale: "en" | "tr" = "en") => {
  cy.env(["adminUsername", "adminPassword"], { log: false }).then(
    ({ adminUsername, adminPassword }) => {
      const username = String(adminUsername);
      const password = String(adminPassword);

      cy.session([username, locale], () => {
        cy.setCookie("locale", locale);
        cy.visit(`/admin/`);
        cy.get('input[name="username"]', { timeout: 15_000 }).should("be.visible").type(username);
        cy.get('input[name="password"]').type(password, { log: false });
        cy.get('button[type="submit"]').click();
        cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");
        cy.url().should("include", `/admin`);
      });
    },
  );
});

Cypress.Commands.add("visitAdmin", (path = "", locale: "en" | "tr" = "en") => {
  cy.loginAdmin(locale);
  cy.visit(`/admin${path}`);
  cy.get(".admin-sidebar", { timeout: 20_000 }).should("be.visible");
});

export {};

declare global {
  namespace Cypress {
    interface Chainable {
      loginAccount(locale?: "en" | "tr"): Chainable<void>;
      visitAccount(path?: string, locale?: "en" | "tr"): Chainable<void>;
    }
  }
}

Cypress.Commands.add("loginAccount", (locale: "en" | "tr" = "en") => {
  cy.env(["adminUsername", "adminPassword"], { log: false }).then(
    ({ adminUsername, adminPassword }) => {
      cy.setCookie("locale", locale);
      cy.visit(`/account/`);
      cy.get('input[name="username"]', { timeout: 15_000 }).then(($input) => {
        if ($input.length) {
          cy.wrap($input).type(String(adminUsername));
          cy.get('input[name="password"]').type(String(adminPassword), { log: false });
          cy.get('button[type="submit"]').click();
        }
      });
      cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
    },
  );
});

Cypress.Commands.add("visitAccount", (path = "", locale: "en" | "tr" = "en") => {
  cy.loginAccount(locale);
  cy.visit(`/account${path}`);
  cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
});
