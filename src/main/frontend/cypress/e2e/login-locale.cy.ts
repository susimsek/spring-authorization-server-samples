describe("locale selected during form login", () => {
  const names = { en: "English", tr: "Türkçe" };
  (["account", "admin"] as const).forEach((console) => {
    (["en", "tr"] as const).forEach((selected) => {
      it(`preserves ${selected} selected during ${console} login, including reload`, () => {
        const initial = selected === "en" ? "tr" : "en";
        const path = console === "account" ? "/account/personal-info" : "/admin/users";
        cy.setCookie("locale", initial);
        cy.intercept("GET", "/oauth2/authorize?*").as("authorize");
        cy.visit(path);
        cy.get('input[name="username"]').should("be.visible");
        cy.wait("@authorize").its("request.query.ui_locales").should("eq", initial);
        cy.contains(".dropdown-toggle", names[initial]).click();
        cy.contains(".dropdown-item", names[selected]).click();
        cy.getCookie("locale").its("value").should("eq", selected);
        cy.env(["adminUsername", "adminPassword"], { log: false }).then(
          ({ adminUsername, adminPassword }) => {
            cy.get('input[name="username"]').type(String(adminUsername));
            cy.get('input[name="password"]').type(String(adminPassword), { log: false });
          },
        );
        cy.get('button[type="submit"]').click();
        // Spring resumes the original request: its old hint must not replace the new cookie.
        cy.wait("@authorize").its("request.query.ui_locales").should("eq", initial);
        cy.get(`.${console}-sidebar`, { timeout: 20_000 }).should("be.visible");
        cy.getCookie("locale").its("value").should("eq", selected);
        cy.get("html").should("have.attr", "lang", selected);
        cy.contains(".dropdown-toggle", names[selected]).should("be.visible");
        cy.location("pathname").should("eq", path);
        cy.reload();
        cy.get(`.${console}-sidebar`, { timeout: 20_000 }).should("be.visible");
        cy.get("html").should("have.attr", "lang", selected);
        cy.getCookie("locale").its("value").should("eq", selected);
        cy.location("pathname").should("eq", path);
      });
    });
  });
});
