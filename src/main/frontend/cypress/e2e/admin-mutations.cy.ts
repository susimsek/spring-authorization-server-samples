describe("admin mutation flows", () => {
  it("creates a user from the browser form and persists profile attributes", () => {
    const username = `cypress-${Date.now()}`;
    cy.intercept("GET", "/api/admin/roles?page=0&size=100", {
      statusCode: 200,
      body: { content: [{ name: "ROLE_USER" }], page: { size: 100, totalElements: 1 } },
    }).as("roles");
    cy.intercept("GET", "/api/admin/profile-attributes", {
      statusCode: 200,
      body: [
        {
          id: 1,
          name: "username",
          displayName: "Username",
          description: null,
          type: "STRING",
          required: true,
          multivalued: false,
          minLength: null,
          maxLength: 100,
          pattern: null,
          enabled: true,
          displayOrder: 10,
          builtIn: true,
        },
        {
          id: 2,
          name: "firstName",
          displayName: "First name",
          description: null,
          type: "STRING",
          required: false,
          multivalued: false,
          minLength: null,
          maxLength: 100,
          pattern: null,
          enabled: true,
          displayOrder: 30,
          builtIn: true,
        },
        {
          id: 3,
          name: "lastName",
          displayName: "Last name",
          description: null,
          type: "STRING",
          required: false,
          multivalued: false,
          minLength: null,
          maxLength: 100,
          pattern: null,
          enabled: true,
          displayOrder: 40,
          builtIn: true,
        },
        {
          id: 4,
          name: "email",
          displayName: "Email",
          description: null,
          type: "EMAIL",
          required: false,
          multivalued: false,
          minLength: null,
          maxLength: 200,
          pattern: null,
          enabled: true,
          displayOrder: 20,
          builtIn: true,
        },
        {
          id: 5,
          name: "department",
          displayName: "Department",
          description: "The user's department.",
          type: "STRING",
          required: false,
          multivalued: false,
          minLength: null,
          maxLength: 100,
          pattern: null,
          enabled: true,
          displayOrder: 50,
          builtIn: false,
        },
      ],
    }).as("profileDefinitions");
    cy.intercept("POST", "/api/admin/users", (request) => {
      expect(request.body.username).to.equal(username);
      expect(request.body.password).to.equal("Cypress-test12!");
      expect(request.body.roles).to.include("ROLE_USER");
      request.reply({
        statusCode: 201,
        body: {
          id: 9901,
          username,
          firstName: "",
          lastName: "",
          email: null,
          emailVerified: false,
          enabled: true,
          locked: false,
          lockedUntil: null,
          failedLoginCount: 0,
          mustChangePassword: false,
          temporaryPassword: true,
          totpEnabled: false,
          avatarUrl: null,
          authorities: ["ROLE_USER"],
          assignedRoles: ["ROLE_USER"],
          groupMappings: [],
          inheritedRoles: [],
          effectiveRoles: ["ROLE_USER"],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      });
    }).as("createUser");
    cy.intercept("PUT", "/api/admin/users/9901/profile-attributes", (request) => {
      expect(request.body.attributes).to.deep.equal({});
      request.reply({ statusCode: 200, body: { definitions: [], attributes: {} } });
    }).as("saveProfileAttributes");

    cy.visitAdmin("/users/new");
    cy.wait(["@roles", "@profileDefinitions"]);
    cy.wait(200);
    cy.get('input[name="username"]').type(username);
    cy.get('input[type="password"]').type("Cypress-test12!");
    cy.contains("button", /Save|Kaydet/).click();
    cy.wait("@createUser");
    cy.wait("@saveProfileAttributes");
    cy.location("pathname").should("eq", "/admin/users");
  });

  it("toggles a user's enabled state through the detail UI", () => {
    cy.intercept("GET", "/api/admin/users/2", {
      statusCode: 200,
      body: {
        id: 2,
        username: "user",
        firstName: null,
        lastName: null,
        email: "user@example.test",
        emailVerified: true,
        enabled: true,
        locked: false,
        lockedUntil: null,
        failedLoginCount: 0,
        mustChangePassword: false,
        temporaryPassword: false,
        totpEnabled: false,
        avatarUrl: null,
        authorities: ["ROLE_USER"],
        assignedRoles: ["ROLE_USER"],
        groupMappings: [],
        inheritedRoles: [],
        effectiveRoles: ["ROLE_USER"],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    }).as("userDetails");
    cy.intercept("GET", "/api/admin/users/2/profile-attributes", {
      statusCode: 200,
      body: { definitions: [], attributes: {} },
    }).as("userProfile");
    cy.intercept("GET", "/api/admin/roles?page=0&size=100", {
      statusCode: 200,
      body: { content: [{ name: "ROLE_USER" }], page: { size: 100, totalElements: 1 } },
    }).as("roles");
    cy.intercept("PUT", "/api/admin/users/2/enabled", { statusCode: 204 }).as("toggleEnabled");

    cy.visitAdmin("/users/2/details");
    cy.wait(["@userDetails", "@userProfile", "@roles"]);
    cy.contains("button", /Disable|Devre dışı bırak/).click();
    cy.wait("@toggleEnabled").then(({ request }) => {
      expect(JSON.stringify(request.body)).to.include("false");
    });
  });
});
