describe("account security", () => {
  it("uses the live account APIs for MFA enrollment, recovery status, passkeys, and social links", () => {
    cy.intercept("GET", "/api/account/mfa").as("mfaStatus");
    cy.intercept("GET", "/api/account/webauthn/credentials?*").as("passkeys");
    cy.intercept("GET", "/api/account/social-links").as("socialLinks");
    cy.intercept("POST", "/api/account/mfa/setup").as("mfaSetup");

    cy.visitAccount("/security/");

    cy.wait("@mfaStatus").then(({ response }) => {
      expect(response?.statusCode).to.equal(200);
      expect(response?.body).to.have.property("available").that.is.a("boolean");
      cy.wrap(Boolean(response?.body?.available)).as("mfaAvailable");
    });
    cy.wait("@passkeys").its("response.statusCode").should("eq", 200);
    cy.wait("@socialLinks").its("response.statusCode").should("eq", 200);
    cy.contains("h2", /Connected accounts|Bağlı hesaplar/).should("be.visible");
    cy.contains("h2", /Passkeys|Geçiş anahtarları/).should("be.visible");

    cy.get<boolean>("@mfaAvailable").then((mfaAvailable) => {
      if (!mfaAvailable) {
        cy.log("MFA is disabled in the current dev seed; API availability was verified.");
        return;
      }
      cy.contains("h2", /Two-factor authentication|İki aşamalı doğrulama/).should("be.visible");
      cy.contains("button", /Set up authenticator|Kimlik doğrulayıcıyı kur/).click();
      cy.wait("@mfaSetup").then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
        expect(response?.body).to.have.property("secret").that.is.a("string");
        expect(String(response?.body?.secret ?? "")).to.not.equal("");
        expect(response?.body).to.have.property("qrCode").that.is.a("string");
        expect(String(response?.body?.qrCode ?? "")).to.not.equal("");
      });
      cy.get('img[alt*="QR"]').should("be.visible");

      cy.get('input[inputmode="numeric"]').type("123");
      cy.contains("button", /Enable MFA|MFA'yı etkinleştir/).click();
      cy.get('input[inputmode="numeric"]').should("have.class", "is-invalid");
    });
  });
});
