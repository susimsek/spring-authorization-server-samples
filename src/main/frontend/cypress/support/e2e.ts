/// <reference types="cypress" />

import "./commands";

Cypress.on("uncaught:exception", (error) => {
  // Browser cancellation during OAuth redirects can surface as an aborted request.
  if (/AbortError|aborted|cancelled/i.test(error.message)) return false;
  return undefined;
});
