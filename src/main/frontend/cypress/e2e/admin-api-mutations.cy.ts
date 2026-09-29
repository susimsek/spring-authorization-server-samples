describe("admin mutation API flows", () => {
  type StoredTokens = { accessToken: string };

  const adminToken = () =>
    cy.window().then((window) => {
      const raw = window.localStorage.getItem("AUTH_CONSOLE_TOKEN:admin");
      expect(raw, "admin console token").to.be.a("string");
      return (JSON.parse(raw ?? "{}") as StoredTokens).accessToken;
    });

  it("executes isolated client, role, provider, mapper, localization, and key mutations", () => {
    cy.loginAdmin();
    const suffix = `${Date.now()}-${Cypress._.random(1000, 9999)}`;
    const clientId = `cypress-client-${suffix}`;
    const roleName = `ROLE_CYPRESS_${suffix.replace(/-/g, "_").toUpperCase()}`;
    const registrationId = `cypress${suffix.replace(/-/g, "").slice(0, 18)}`;
    const messageKey = `cypress.test.${suffix}`;
    let clientInternalId = "";
    let providerId = "";
    let mapperId = "";
    let localizationId = 0;

    adminToken().then((accessToken) => {
      const headers = { Authorization: `Bearer ${accessToken}` };
      const clientRequest = {
        clientId,
        clientName: "Cypress mutation client",
        clientAuthenticationMethods: ["client_secret_basic"],
        authorizationGrantTypes: ["client_credentials"],
        redirectUris: [],
        postLogoutRedirectUris: [],
        scopes: ["openid"],
        requireAuthorizationConsent: false,
        requireProofKey: false,
        requireDpop: false,
        requireDpopJkt: false,
        dpopRefreshTokenOnly: false,
        dpopSigningAlgorithms: ["RS256", "ES256"],
        authorizationCodeTimeToLive: "PT5M",
        accessTokenTimeToLive: "PT5M",
        refreshTokenTimeToLive: "PT1H",
      };

      cy.request({ method: "POST", url: "/api/admin/clients", headers, body: clientRequest })
        .then((response) => {
          expect(response.status).to.eq(201);
          clientInternalId = response.body.client.id;
          expect(response.body.client.clientId).to.eq(clientId);
          expect(response.body.clientSecret).to.be.a("string");
          expect(response.body.clientSecret.length).to.be.greaterThan(0);
          return cy.request({
            method: "PUT",
            url: `/api/admin/clients/${clientInternalId}`,
            headers,
            body: { ...clientRequest, clientName: "Updated Cypress mutation client" },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.clientName).to.eq("Updated Cypress mutation client");
          return cy.request({
            method: "POST",
            url: `/api/admin/clients/${clientInternalId}/secret`,
            headers,
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.clientSecret).to.be.a("string");
          expect(response.body.clientSecret.length).to.be.greaterThan(0);
          return cy.request({
            method: "POST",
            url: "/api/admin/roles",
            headers,
            body: { name: roleName, description: "Cypress role" },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(201);
          return cy.request({
            method: "PUT",
            url: `/api/admin/roles/${encodeURIComponent(roleName)}`,
            headers,
            body: { name: roleName, description: "Updated Cypress role" },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.description).to.eq("Updated Cypress role");
          return cy.request({
            method: "POST",
            url: "/api/admin/identity-providers",
            headers,
            body: {
              registrationId,
              providerType: "oidc",
              displayName: "Cypress OIDC",
              alias: registrationId,
              iconKey: "generic",
              shortStateParameter: false,
              caseSensitiveUsername: false,
              enabled: false,
              clientId: "cypress-idp-client",
              clientSecret: "cypress-idp-secret",
              hideOnLogin: false,
              accountLinkingOnly: false,
              trustEmail: false,
              mfaRequired: false,
              requiredClaims: "sub",
              storeTokens: false,
              storedTokensReadable: false,
              guiOrder: 99,
              showInAccountConsole: "always",
              syncMode: "import",
              authorizationUri: "https://idp.example.test/authorize",
              tokenUri: "https://idp.example.test/token",
              userInfoUri: null,
              jwkSetUri: null,
              issuerUri: null,
              clientAuthenticationMethod: "client_secret_basic",
              scopes: "openid,profile",
              userNameAttribute: "sub",
            },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(201);
          providerId = response.body.id;
          return cy.request({
            method: "POST",
            url: `/api/admin/identity-providers/${providerId}/mappers`,
            headers,
            body: {
              name: "cypress-email",
              sourceClaim: "email",
              target: "email",
              mapperType: "user-attribute",
              syncMode: "inherit",
              addToIdToken: true,
              addToAccessToken: true,
            },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          mapperId = response.body.id;
          return cy.request({
            method: "PUT",
            url: `/api/admin/identity-providers/${providerId}/mappers/${mapperId}`,
            headers,
            body: {
              name: "cypress-email-updated",
              sourceClaim: "email",
              target: "email",
              mapperType: "user-attribute",
              syncMode: "inherit",
              addToIdToken: true,
              addToAccessToken: true,
            },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          return cy.request({
            method: "POST",
            url: "/api/admin/settings/localization/messages",
            headers,
            body: {
              locale: "en",
              bundle: "admin",
              messageKey,
              messageValue: "Cypress message",
            },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(201);
          localizationId = response.body.id;
          return cy.request({
            method: "PUT",
            url: `/api/admin/settings/localization/messages/${localizationId}`,
            headers,
            body: {
              locale: "en",
              bundle: "admin",
              messageKey,
              messageValue: "Updated Cypress message",
            },
          });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.messageValue).to.eq("Updated Cypress message");
          return cy.request({ method: "POST", url: "/api/admin/keys/rotate", headers });
        })
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.active).to.eq(true);
          expect(response.body).to.include({ type: "RSA", use: "sig", algorithm: "RS256" });
        })
        .then(() => {
          const cleanups: Cypress.Chainable[] = [];
          if (mapperId && providerId) {
            cleanups.push(
              cy.request({
                method: "DELETE",
                url: `/api/admin/identity-providers/${providerId}/mappers/${mapperId}`,
                headers,
              }),
            );
          }
          if (providerId) {
            cleanups.push(
              cy.request({
                method: "DELETE",
                url: `/api/admin/identity-providers/${providerId}`,
                headers,
              }),
            );
          }
          if (localizationId) {
            cleanups.push(
              cy.request({
                method: "DELETE",
                url: `/api/admin/settings/localization/messages/${localizationId}`,
                headers,
              }),
            );
          }
          cleanups.push(
            cy.request({
              method: "DELETE",
              url: `/api/admin/roles/${encodeURIComponent(roleName)}`,
              headers,
            }),
          );
          if (clientInternalId) {
            cleanups.push(
              cy.request({
                method: "DELETE",
                url: `/api/admin/clients/${clientInternalId}`,
                headers,
              }),
            );
          }
          return cy.wrap(cleanups);
        })
        .then((cleanups) => {
          cleanups.forEach((cleanup) => cleanup.its("status").should("eq", 204));
        });
    });
  });
});
