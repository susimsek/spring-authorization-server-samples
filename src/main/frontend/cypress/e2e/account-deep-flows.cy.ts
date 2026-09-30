describe("account security and action flows", () => {
  it("completes MFA setup through the account security UI", () => {
    cy.intercept("GET", "/api/account/mfa", {
      statusCode: 200,
      body: {
        enabled: false,
        available: true,
        required: false,
        issuer: "Spring Authorization Server",
        digits: 6,
      },
    }).as("mfaStatus");
    cy.intercept("POST", "/api/account/mfa/setup", {
      statusCode: 200,
      body: {
        secret: "JBSWY3DPEHPK3PXP",
        qrCode: "data:image/png;base64,e2U=",
        algorithm: "SHA1",
        digits: 6,
        period: 30,
      },
    }).as("mfaSetup");
    cy.intercept("POST", "/api/account/mfa/enable", { statusCode: 204 }).as("mfaEnable");

    cy.visitAccount("/security/");
    cy.wait("@mfaStatus");
    cy.contains("button", /Set up authenticator|Kimlik doğrulayıcıyı kur/).click();
    cy.wait("@mfaSetup");
    cy.get('img[alt*="QR"]').should("be.visible");
    cy.get('input[inputmode="numeric"]').type("123456");
    cy.contains("button", /Enable MFA|MFA'yı etkinleştir/).click();
    cy.wait("@mfaEnable").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("123456");
    });
  });

  it("generates recovery codes and completes MFA disable", () => {
    cy.intercept("GET", "/api/account/mfa", {
      statusCode: 200,
      body: {
        enabled: true,
        available: true,
        required: false,
        issuer: "Spring Authorization Server",
        digits: 6,
        warningThreshold: 3,
      },
    }).as("mfaStatus");
    cy.intercept("GET", "/api/account/mfa/recovery-codes", {
      statusCode: 200,
      body: { remaining: 5, warningThreshold: 3 },
    }).as("recoveryStatus");
    cy.intercept("POST", "/api/account/mfa/recovery-codes", {
      statusCode: 200,
      body: { codes: ["recovery-1", "recovery-2"], remaining: 2 },
    }).as("recoveryGenerate");
    cy.intercept("POST", "/api/account/mfa/disable", { statusCode: 204 }).as("mfaDisable");

    cy.visitAccount("/security/");
    cy.wait(["@mfaStatus", "@recoveryStatus"]);
    cy.contains("button", /Generate recovery codes|Kurtarma kodları oluştur/).click();
    cy.wait("@recoveryGenerate");
    cy.contains("recovery-1").should("be.visible");
    cy.get('input[inputmode="numeric"]').type("123456");
    cy.contains("button", /Disable MFA|MFA'yı devre dışı bırak/).click();
    cy.wait("@mfaDisable").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("123456");
    });
  });

  it("registers a passkey through the browser WebAuthn contract", () => {
    cy.intercept("GET", "/api/account/webauthn/credentials?*").as("passkeys");
    cy.intercept("POST", "/webauthn/register/options", {
      statusCode: 200,
      body: {
        challenge: "Y2hhbGxlbmdl",
        user: { id: "dXNlcg", name: "admin", displayName: "admin" },
      },
    }).as("passkeyOptions");
    cy.intercept("POST", "/webauthn/register", { statusCode: 201 }).as("passkeyRegister");

    cy.loginAccount();
    cy.visit("/account/security/", {
      onBeforeLoad(win) {
        Object.defineProperty(win, "PublicKeyCredential", {
          configurable: true,
          value: class PublicKeyCredential {},
        });
        const buffer = new Uint8Array([1, 2, 3]).buffer;
        Object.defineProperty(win.navigator, "credentials", {
          configurable: true,
          value: {
            create: () =>
              Promise.resolve({
                id: "cypress-passkey",
                rawId: buffer,
                type: "public-key",
                response: {
                  attestationObject: buffer,
                  clientDataJSON: buffer,
                  getTransports: () => ["internal"],
                },
                getClientExtensionResults: () => ({}),
                authenticatorAttachment: "platform",
              }),
          },
        });
      },
    });
    cy.get(".account-sidebar", { timeout: 20_000 }).should("be.visible");
    cy.wait("@passkeys");
    cy.get("#passkey-label").type("Cypress passkey");
    cy.contains("button", /Add passkey|Geçiş anahtarı ekle/).click();
    cy.wait("@passkeyOptions");
    cy.wait("@passkeyRegister").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("Cypress passkey");
    });
  });
});

describe("public account actions", () => {
  it("verifies an email action token", () => {
    cy.intercept("POST", "/api/auth/verify-email", { statusCode: 204 }).as("verifyEmail");
    cy.visit("/verify-email?token=cypress-email-token");
    cy.contains("button", /Verify email|E-postayı doğrula/).click();
    cy.wait("@verifyEmail").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("cypress-email-token");
    });
    cy.get('[role="alert"]').should("be.visible");
  });

  it("submits the required password action", () => {
    cy.intercept("GET", "/api/required-actions", {
      statusCode: 200,
      body: [
        {
          key: "UPDATE_PASSWORD",
          displayName: "Update password",
          description: "Choose a new password",
          version: 1,
        },
      ],
    }).as("requiredActions");
    cy.intercept("POST", "/api/required-actions/UPDATE_PASSWORD", { statusCode: 204 }).as(
      "updatePassword",
    );

    cy.visit("/required-actions?return_to=/account");
    cy.wait("@requiredActions");
    cy.get("#required-action-new-password").type("Required-test12!");
    cy.get("#required-action-confirm-password").type("Required-test12!");
    cy.contains("button", /Continue|Devam/).click();
    cy.wait("@updatePassword").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("Required-test12!");
    });
  });

  it("submits account deletion without deleting the seeded user", () => {
    cy.intercept("DELETE", "/api/account", { statusCode: 204 }).as("deleteAccount");
    cy.visitAccount("/personal-info/");
    cy.get("#delete-account-password").type("admin");
    cy.get("#delete-account-password").closest("form").find('button[type="submit"]').click();
    cy.wait("@deleteAccount").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("admin");
    });
  });
});
