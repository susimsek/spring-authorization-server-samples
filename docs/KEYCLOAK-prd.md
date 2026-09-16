# Keycloak PRD

## Purpose

This product requirements document defines the administration-console information architecture that this sample should follow when it presents Keycloak-like realm administration capabilities. It translates Keycloak's documented console structure, event-auditing model, REST surface, and administrative role model into a screen-level plan for this application.

The target is a single-issuer Spring Authorization Server sample. The application should adopt the parts of Keycloak's navigation and interaction model that make administration predictable, while keeping its own resource names, API contracts, role constants, and existing visual system. A Keycloak installation can manage several isolated realms; this application currently exposes one configured issuer and does not expose a realm selector. That difference is intentional and is recorded as a scope boundary below.

## Executive findings

1. Keycloak separates **configuration** from **history**. Realm Settings is the home for realm-wide configuration, while Events is the home for stored event records and filters.[^1]
2. Event auditing has two distinct streams: user events and administrative events. Their settings, stored representations, filters, and clear actions are separate in the Keycloak console.[^2]
3. Event settings belong under the Settings navigation in this project. The event history belongs at `/admin/events`, and the configuration belongs at `/admin/settings/events`.
4. Keycloak uses `view-events` and `manage-events` as separate administrative permissions. Read access must not imply the ability to change configuration or delete history.[^1]
5. The save action should follow the existing Settings pattern: a single vertical form inside the shared settings card, with a primary Save button in the bottom action row, an inline progress spinner, and localized success or error feedback.
6. The current application already has an administrative event stream, filtering, retention settings, clear-history behavior, and the `EVENT_VIEWER`/`EVENT_MANAGER`/`ADMIN` authorization split. The remaining Keycloak parity gaps are primarily the separate user-event stream, event-type selection, listener configuration, and representation-level event details.

## Product scope

### In scope

- A stable admin-console information architecture for users, groups, clients, client scopes, sessions, settings, and events.
- A settings destination that contains realm-wide application controls and an Events settings section.
- A read-only event-history screen with filtering, pagination, event details, and a permission-gated clear action.
- Event recording, administrative-event detail capture, retention, and event listener extension points.
- Explicit view/manage authorization for every administrative area.
- Screen states for loading, empty results, validation, save success, save failure, forbidden access, and destructive-action confirmation.
- API and persistence requirements that allow the UI to remain consistent with the existing Spring, JPA, Liquibase, and cache conventions.

### Out of scope for the current sample

- A multi-realm selector or master-realm administration. Keycloak treats realms as isolated management spaces and allows a master realm to administer other realms; this sample has one configured issuer instead.[^1]
- Keycloak's full fine-grained permission-management UI, Organizations, identity-provider administration, authorization services, and server-level metrics.
- A requirement to reproduce Keycloak's exact CSS, wording, or internal REST paths.
- Replacing the application's existing role names with Keycloak client-role names.

## Keycloak information architecture

### Global shell

The Keycloak Admin Console is scoped to a current realm. The shell exposes the current realm selector, a top-right account/logout menu, and the navigation for the selected realm. The documented first-use flow asks the administrator to select or create a realm and then open **Realm settings** for that realm.[^1]

The equivalent shell in this application is:

| Keycloak concept | Application placement | Requirement |
| --- | --- | --- |
| Current realm selector | Not present | Keep one configured issuer; do not present a misleading realm switcher. |
| Realm administration navigation | Admin sidebar | Keep resource links grouped by resource type and permission. |
| Account/logout menu | Existing admin shell | Preserve the current account and logout behavior. |
| Realm Settings | **Settings** | Use Settings as the home for server and realm-wide configuration. |
| Events history | **Events** | Keep history separate from configuration. |

The sidebar must hide a resource when the user cannot query or view it, and the API remains authoritative even when a navigation item is hidden. A direct URL must produce the same authorization result as a sidebar click.

### Primary navigation model

The following model is the recommended subset of Keycloak's realm administration layout:

| Area | Keycloak responsibility | Application screen | Access intent |
| --- | --- | --- | --- |
| Dashboard | Realm overview and operational entry point | Existing admin landing page | Authenticated admin |
| Users | Search, create, inspect, edit, disable, and manage membership | `/admin/users` and user detail screens | `ROLE_USER_VIEWER` or `ROLE_USER_MANAGER` |
| Groups | Search, create, inspect, and manage membership/role mappings | `/admin/groups` and group detail screens | `ROLE_USER_VIEWER` or `ROLE_USER_MANAGER` |
| Clients | Register and manage OAuth/OIDC clients | `/admin/clients` and client detail screens | `ROLE_CLIENT_VIEWER` or `ROLE_CLIENT_MANAGER` |
| Client scopes | Reusable scopes and protocol claims | Existing client-scope screens | Admin capability where implemented |
| Sessions | Active browser/session visibility and invalidation | Existing session screens | Admin capability where implemented |
| Events | Stored event history and filters | `/admin/events` | `ROLE_EVENT_VIEWER`/`ROLE_EVENT_MANAGER` equivalent |
| Settings | Realm-wide login, password, OTP, brute-force, session, email, and event configuration | `/admin/settings/*` | Admin, plus event-specific access for Events settings |

Keycloak also has areas such as Identity Providers, Organizations, Authorization, and realm-management roles. They should remain explicit roadmap items until the corresponding server behavior, API, persistence, and access tests exist; a navigation placeholder must not imply that the feature is available.

### Resource detail layout

Keycloak uses resource-specific detail views with tabs or sections for related concerns. The project should retain that pattern:

- **User detail**: profile, credentials/required actions, group membership, role mappings, sessions, consents, and events where implemented.
- **Group detail**: group profile, child groups, members, and role mappings.
- **Client detail**: general settings, capabilities, login settings, credentials, scopes, sessions, consents, and events.
- **Settings**: a stable tab strip for general, login, email, password policy, OTP policy, brute-force, sessions, and events.

Tabs must not be used as a substitute for authorization. A tab that is not available to the current authority should be omitted, and a direct request for its API must return the centralized forbidden response.

## Events information architecture

### Why configuration and history are separate

Keycloak's event documentation presents configuration under **Realm settings → Events**, while stored records are opened from the top-level **Events** menu. This separation lets a read-only auditor inspect history without granting configuration or deletion rights.[^2]

The application follows the same model:

```text
Settings
└── Events
    ├── Event recording
    ├── Administrative event details
    └── Retention

Events
├── Search and filters
├── Event table
├── Event detail
└── Clear all events (manager only)
```

### Event Settings screen

Route: `/admin/settings/events`

The screen must use the same `AdminPageHeader`, settings navigation, shared `admin-panel-card`, vertical form spacing, and `admin-form-actions` row as the other Settings screens. The form contains:

| Control | Behavior | Keycloak relationship |
| --- | --- | --- |
| Enable event recording | Enables or disables persistence of supported events | Corresponds to saving events in the User events/Admin events settings. |
| Enable administrative events | Enables the administrative audit stream | Corresponds to Keycloak's Admin events **Save events** switch. |
| Include event details | Stores request/representation details when supported | Corresponds to **Include representation** for admin events. Keycloak warns that this can increase stored data.[^2] |
| Retention (days) | `0` means no expiry; positive values purge older records | Maps the product's day-based setting to its persistence policy. Keycloak's provider representation calls this `eventsExpiration` and expresses it as provider configuration.[^3] |

Interaction requirements:

- Load the current values before enabling Save.
- Keep controls in one vertical column; do not introduce a side-by-side form or a floating action area.
- Place the primary Save button at the bottom of the card in the existing action row. Preserve the button label while showing an inline spinner and disable repeated submission.
- Reset the form to the server response after a successful save.
- Show a localized success alert after save and a localized error alert after load or save failure.
- A user with event view access can inspect the form but cannot edit controls or submit it.
- A user without event view access receives the normal forbidden behavior and does not get a partially rendered settings page.
- Validation must reject non-integer and out-of-range retention values at the form boundary and again at the API boundary.

### Events history screen

Route: `/admin/events`

The history screen contains:

1. A page header with the total record count.
2. A Search/filter control that can narrow by action, target type, target identifier, and date range.
3. A stable, paginated table sorted by newest event first.
4. An event detail view that exposes actor, action, target, timestamp, and stored details when available.
5. A manager-only **Clear all events** action placed in the existing action row, using a destructive confirmation dialog.

The page must distinguish an empty result from a failed request. Clearing history must close the confirmation dialog only after a successful response, refresh the table, and show a localized success message. It must not silently clear records on a GET or on navigation.

Keycloak's Admin REST API supports event-history filtering by client, user, IP address, date range, event type, offset, maximum result count, and sort direction. Its administrative-event API additionally supports operation type, authenticated realm/client/user/IP, resource path/type, date range, offset, maximum result count, and direction.[^3][^4] The application can expose its existing resource-oriented filters without copying every Keycloak parameter; each supported filter must have a documented DTO and an indexed query path.

### Two event streams

Keycloak documents both streams explicitly:

- **User events** cover actions affecting users, such as successful or failed login and account changes. They have saved event types, expiration, and a clear-user-events operation.[^2]
- **Admin events** cover administrator actions performed through the Admin Console or Admin REST API. They can optionally include the JSON representation sent to the API, and they have a clear-admin-events operation.[^2]

The current application stores an administrative audit stream. The product should preserve that behavior and model user events as a separate capability if user-event parity is required. A single mixed table may be used internally only when the event type is explicit and the UI/API preserve the two-stream semantics.

### Event listeners

Keycloak exposes event listeners under **Realm settings → Events → Event listeners**. Built-in listeners can write to logs or send email, and the listener SPI allows extensions to react to events.[^2] The application should reserve a settings section for listener registration and delivery policy, but should not expose a listener toggle until the server has a durable listener registry, delivery error handling, audit events, and integration tests.

## Authorization model

### Keycloak reference roles

Keycloak's documented realm-management roles include `manage-events` and `view-events`, alongside `manage-users`, `query-users`, `query-groups`, `query-clients`, `manage-clients`, `view-users`, `view-clients`, `manage-realm`, `view-realm`, and other resource-specific roles.[^1] Query permissions allow the console to find resources; view permissions allow resource details; manage permissions allow mutation. The exact availability of a menu therefore depends on the combination of query, view, and manage roles.

### Application roles

The application uses stable application authorities rather than exposing Keycloak client-role names directly:

| Application authority | Capability | Event Settings | Events history | Clear history |
| --- | --- | --- | --- | --- |
| `ROLE_ADMIN` | Full administration | Read/write | Read | Yes |
| `ROLE_EVENT_VIEWER` | Inspect event configuration and history | Read-only | Read | No |
| `ROLE_EVENT_MANAGER` | Manage event settings and history | Read/write | Read | Yes |
| `ROLE_USER_VIEWER` | Inspect users and groups where implemented | No | No | No |
| `ROLE_USER_MANAGER` | Manage users, groups, and memberships where implemented | No | No | No |
| `ROLE_CLIENT_VIEWER` | Inspect clients where implemented | No | No | No |
| `ROLE_CLIENT_MANAGER` | Manage clients where implemented | No | No | No |

The role-to-screen mapping must be defined once in the authority constants and reused by backend security rules, service checks, UI access flags, and tests. `ROLE_EVENT_VIEWER` is the application counterpart of Keycloak's `view-events`; `ROLE_EVENT_MANAGER` is the counterpart of `manage-events`. If the product keeps the existing `EVENT_VIEWER` and `EVENT_MANAGER` constants internally, the API and documentation should still expose one consistent naming convention to clients.

### Authorization acceptance rules

- A viewer can load settings and history but cannot update settings or delete records.
- A manager can update settings and clear records.
- An administrator inherits manager behavior.
- Every mutation checks the authority on the server and records an audit event.
- Mutations that change effective permissions, settings, sessions, or authorization state invalidate affected sessions and OAuth2 authorizations according to the application security policy.
- Hiding a button is a usability feature only; it is not the authorization boundary.

## Current feature parity with Keycloak

The following matrix compares the behavior currently implemented in this repository with the corresponding Keycloak capability. “Implemented” means the application exposes a working, tested product behavior. “Partial” means a related behavior exists but the scope, data model, token contract, or administration model differs. “Missing” means there is no product behavior yet. A missing feature remains a roadmap item and must not be implied by a navigation link.

| Capability | Current application | Keycloak reference behavior | Parity | Product implication |
| --- | --- | --- | --- | --- |
| Realm boundary | One configured issuer and one shared data space | Isolated realms contain users, clients, groups, roles, sessions, and realm settings; a master realm can administer other realms[^1] | Missing | Add realm/tenant IDs before claiming multi-tenant support. |
| Realm settings | Login, password, OTP, brute-force, email, session, and event settings | Realm Settings is the central configuration area with separate tabs | Partial | Keep the Settings route and add realm ownership when multi-tenancy is approved. |
| User CRUD | Admin API and UI | User list/detail, credentials, required actions, groups, roles, sessions, and consents | Partial | Credential inventory and individual lifecycle operations are covered; bulk lifecycle operations remain. |
| Dynamic user profile | Application-wide configurable attributes with validation, requiredness, type checks, pattern/length rules, and optional multi-value support; managed under Settings and rendered in admin/account profile forms | Configurable user-profile attributes with validation and requiredness | Implemented for the single-issuer application | Keep the schema application-wide while realms are intentionally out of scope; add localized labels, token mappers, and per-realm ownership only if realm support is introduced. |
| Password and account recovery | Password policy, history, expiry, email verification, reset tokens | Password credentials and required actions configured per realm | Implemented / Partial | Preserve current flows; align required-action metadata with realm settings. |
| TOTP and recovery codes | TOTP enrollment/verification, required TOTP, hashed recovery codes, WebAuthn/passkey enrollment and credential verification, and required passkeys | OTP/WebAuthn credentials and configurable required actions | Implemented | TOTP and passkeys are implemented, including primary and conditional passkey sign-in, OTP fallback, account/admin credential inventory, device metadata, label management, deletion, and both standard and passwordless required actions. |
| User impersonation | Admin-only, single-use ticket flow | Admin impersonation permission and console action | Partial | Keep the flow and add resource-scoped permission plus audit detail. |
| Groups | CRUD, membership, hierarchy, parent role inheritance, multi-valued attributes, default groups, configurable group claims, and group-scoped permissions | Hierarchical groups, attributes, role mappings, default groups, membership permissions[^6] | Implemented / Partial | Preserve the single-issuer boundary; add broader Keycloak protocol mapper types and realm boundaries later. |
| Group claims | Configurable group membership mapper on client scopes with claim name and full-path options | Group membership mapper is configurable per client/client scope | Implemented / Partial | Add the remaining protocol mapper types and token-preview tooling. |
| Realm roles | Global application authorities | Realm-level roles with direct, group, and composite assignment | Partial | Add realm ownership and composite-role relationships. |
| Client roles | No client namespace in the role model | Client roles are scoped to a client and appear in `resource_access` | Missing | Add client-scoped roles and token claim filtering. |
| Composite roles | No composite role graph | Roles can include other roles with cycle protection | Missing | Add a role graph and effective-role resolver. |
| Query permissions | Resource roles combine discovery and access | `query-users`, `query-groups`, and `query-clients` control console discovery | Partial | Split navigation/query permissions from view/manage permissions where needed. |
| Event permissions | Event viewer/manager authorities protect event APIs | `view-events` and `manage-events` are separate realm-management roles[^1] | Implemented / Partial | Preserve the split; add user-event/provider scope later. |
| Client CRUD | Registered OAuth/OIDC clients, redirect URIs, secrets, grants, scopes | Clients have settings, credentials, client scopes, roles, sessions, and service accounts | Partial | Add client roles, service accounts, mapper configuration, and richer credentials. |
| Client scopes | Scope CRUD and client assignment | Default/optional scopes with protocol mappers and role scope mappings | Partial | Add mapper and scope-evaluation screens. |
| Protocol mappers | No mapper entity or UI; a few claims are generated in code | Mappers turn user, group, role, and client data into OIDC/SAML claims[^11] | Missing | Implement allow-listed mapper types and token preview. |
| Authorization Code + PKCE | Browser clients use Authorization Code + S256 PKCE | Supported and standard for browser applications | Implemented | Keep as the default console flow. |
| Refresh tokens | Rotation, persistence, browser namespacing, logout handling | Refresh/offline token policies with realm/client controls | Partial | Add offline sessions only if product requirements justify them. |
| Client credentials | Seeded client and integration coverage | Service accounts and client credentials | Partial | Add a service-account user model and role assignment. |
| PAR, Device Authorization, introspection, revocation | Implemented and tested | Supported protocol endpoints | Implemented | Preserve endpoint metadata and integration tests. |
| OIDC discovery, UserInfo, JWKS, logout | Implemented | Standard OIDC provider endpoints and session/logout behavior | Implemented | Keep issuer and metadata stable for clients. |
| Token exchange | No standard token-exchange grant | Standard and administrator token exchange options[^12] | Missing | Add only with explicit audience, impersonation, and policy controls. |
| CIBA, DPoP, resource indicators | Not implemented | Available as advanced protocol capabilities in Keycloak distributions | Missing | Treat as separate protocol epics, not UI-only work. |
| SAML | No SAML IdP/SP | SAML client and identity-provider support | Missing | Separate protocol product decision. |
| Authentication flows | Fixed Spring Security flow with custom MFA filter and required actions | Configurable browser, registration, reset-credential, first-broker-login, and conditional flows[^8] | Partial | Introduce a flow graph only when administrators need reordering/conditions. |
| WebAuthn/passkeys | Spring Security WebAuthn registration/authentication ceremonies, account/admin credential inventory, labels, deletion, and required-action enrollment | WebAuthn credential and passkey authenticators | Implemented | Registration, persistence, primary sign-in, conditional UI, OTP step-up, account/admin inventory with signature and verification metadata, label management, deletion, configurable policy properties, and standard/passwordless required actions are implemented. |
| Identity brokering | Local JPA users only | OIDC/SAML/social providers, mappers, account linking | Missing | Add provider registry and first-login policy before adding UI. |
| LDAP/Active Directory federation | Not implemented | Federated user stores with sync and mapper policies[^9] | Missing | Requires provider lifecycle, sync jobs, and failure handling. |
| Sessions | JPA browser sessions, admin/account views, revoke | Online and offline sessions, client sessions, revocation and not-before policies | Partial | Add offline session model and realm/client/user not-before policy if required. |
| Consent | OAuth authorization and consent persistence/revoke | User consents and client-specific consent management | Implemented / Partial | Preserve current screens; add client-scope and realm boundaries later. |
| Admin events | Stored audit stream, filters, retention, clear action | Admin event history, filters, representation capture, event provider settings[^2][^3] | Partial | Current event settings parity is in place; add representation redaction/limits. |
| User events | No separate persisted user-event history | User event settings, saved event types, expiration, clear action[^2] | Missing | Add separate event type/storage and a User events tab. |
| Event listeners | No listener registry or provider SPI | Event listeners configured under Realm Settings → Events[^2] | Missing | Add only with delivery status, retries, and auditability. |
| Fine-grained admin permissions | Global role checks and endpoint rules | Resource/scope permissions for users, groups, clients, roles, organizations, and events | Partial | Add resource-scoped permission records and a policy evaluator. |
| Organizations | No organization model | Organization members, domains, invitations, IdPs, groups, and claims[^10] | Missing | Keep out of the single-issuer demo until B2B requirements exist. |
| Authorization Services / UMA | Spring Security authorities protect endpoints | Resources, scopes, policies, permissions, permission tickets, and RPTs[^7] | Missing | Separate resource-server authorization product from admin RBAC. |
| Admin REST | Documented custom `/api/admin` contract with OpenAPI | Keycloak Admin REST under `/admin/realms/{realm}` | Partial | Keep the application contract; map semantics explicitly rather than copying paths. |
| Account Console | Custom account pages for profile, security, sessions, consents, and configurable profile attributes | Keycloak Account Console and account REST capabilities | Partial | Preserve the current user experience and add credential inventory and device views. |
| Themes and localization | Next.js theme switcher and English/Turkish dictionaries | Themes, message bundles, and realm localization settings | Partial | Keep shared components and expose realm localization only with realm support. |

### Existing roles and Keycloak equivalents

The application's role constants are intentionally application-facing. They should not be renamed to Keycloak's internal `realm-management` client roles, but their scope must be documented and eventually mapped to resource-scoped permissions.

| Application authority | Current scope | Closest Keycloak concept | Difference to resolve |
| --- | --- | --- | --- |
| `ROLE_ADMIN` | Full application administration | `admin` or `realm-admin` | Current role is global and single-issuer. |
| `ROLE_USER_VIEWER` | Read user resources | `view-users` | Does not distinguish query from detail view. |
| `ROLE_USER_MANAGER` | Mutate users and related membership | `manage-users` | Lacks fine-grained group/member and role-mapping scopes. |
| `ROLE_CLIENT_VIEWER` | Read client resources | `view-clients` | Does not expose client-scope/resource boundaries. |
| `ROLE_CLIENT_MANAGER` | Mutate clients | `manage-clients` | Does not include service-account, mapper, or client-role policy. |
| `ROLE_EVENT_VIEWER` | Read event settings/history | `view-events` | Current stream is administrative only. |
| `ROLE_EVENT_MANAGER` | Update event settings and clear history | `manage-events` | Does not yet manage listener providers or user-event configuration. |

`query-groups` and `query-clients` are discovery permissions in Keycloak; they are not substitutes for viewing or managing the returned resource. The application should introduce separate query authorities only when a screen needs that distinction. Keycloak does not define a standalone `manage-groups` realm-management role in the current role list; group operations are represented through user/group permissions and fine-grained scopes.[^1][^6]

## Screen-by-screen layout specification

The visual system remains the existing React-Bootstrap admin console. Keycloak is used as an information-architecture reference, not as a reason to introduce a second component library or a second page grammar.

### Shared shell

Every authenticated administration screen uses the same structure:

```text
┌─────────────────────────────────────────────────────────────┐
│ Admin Console | language | theme | signed-in user | sign out │
├───────────────┬─────────────────────────────────────────────┤
│ Dashboard     │ Page header: title, description, status      │
│ Clients       │                                                │
│ Client scopes │ Optional detail/settings tab row               │
│ Users         │                                                │
│ Roles         │ Main content: cards, tables, forms             │
│ Groups        │                                                │
│ Sessions      │                                                │
│ Consents      │                                                │
│ Keys          │                                                │
│ Events        │                                                │
│ Settings      │                                                │
└───────────────┴─────────────────────────────────────────────┘
```

Requirements:

- Use the existing `AdminPageHeader`/`ViewHeader`, `DetailTabs`, `admin-panel-card`, `DataTable`, `ResourceFilters`, and `admin-form-actions` primitives.
- Keep the sidebar order stable: overview, resources, security/session areas, events, then settings.
- On narrow screens the sidebar becomes the existing off-canvas navigation; content cards remain single-column.
- Page titles and descriptions are localized. Route changes do not change the shell or button grammar.
- Loading uses a status region, empty state uses a distinct message, and request failures use the shared alert pattern.
- Any hidden navigation item must also be protected by an API authorization rule.

### Dashboard

Keycloak presents the selected realm as the context for administration. The application dashboard should show the single configured issuer, active signing-key status, recent administrative events, session summary, and links to the primary resource areas. It should not show a realm selector until all resources are realm-scoped.

### Users

List layout:

- page header with user count and Create user action;
- search and filters for username, email, enabled state, and required action;
- paginated table with username, email, enabled state, MFA state, groups, and last activity;
- row menu for View/Edit, disable/enable, impersonate where authorized, and delete where allowed.

Detail layout:

- profile card;
- credentials and required actions card;
- group membership card;
- realm/client role mappings card;
- sessions and consents cards;
- user events card when the user-event stream exists.

Mutating actions remain inside the relevant card and invalidate affected sessions/authorizations in the same transaction.

### Groups

List layout:

- page header with Create group action;
- search field and stable path/name sort;
- tree/path-aware table with group name, parent path, member count, and inherited-role indicator;
- row action for View/Edit and membership management.

Detail layout:

- group name/path card;
- attributes card for multi-valued key/value attributes;
- child groups card;
- members card with add/remove actions;
- direct role mappings and effective inherited role mappings shown separately.

The project supports hierarchical groups and parent role inheritance, multi-valued attributes, default-group assignment for newly created users, configurable client-scope group claims, and inherited group-scoped permissions. Broader Keycloak protocol-mapper types and realm boundaries remain outside this single-issuer sample.

### Roles

The role list should distinguish realm roles and client roles once the role model is expanded. The detail screen should show description, composite children, users, groups, and client scope mappings. A role deletion or composite update requires cycle validation, audit logging, cache eviction, and access invalidation.

### Clients

List layout:

- page header with Register client action;
- search, enabled/protocol filters, and stable client ID sort;
- table with client ID, protocol, access type, enabled state, and authentication method.

Detail tabs:

1. **General**: client ID, name, description, protocol, enabled state.
2. **Capabilities**: grant types, PKCE, consent, device/PAR options.
3. **Login settings**: redirect URIs, post-logout URIs, origins, and login theme.
4. **Credentials**: secret status, rotation, service account, and authentication method.
5. **Client scopes**: default and optional scope assignments.
6. **Roles**: client-scoped roles and composite relationships.
7. **Mappers**: protocol mapper list, create/edit drawer or card, and token preview.
8. **Sessions/consents/events**: client-specific operational views where supported.

The current project already has a multi-step client create/edit flow, redirect URI validation, secret rotation, scopes, sessions, consents, and events. The missing layout sections should be added only when their backend contracts exist.

### Settings

Settings is a single page family with a stable tab strip:

```text
Settings
├── General
├── Login
├── Email
├── Password policy
├── OTP policy
├── Brute force
├── Sessions
├── Events
└── User profile
```

Each tab uses the same card and form grammar:

- a card title and description;
- controls in one vertical column with shared spacing;
- field-level validation under the owning control;
- Save at the bottom in `admin-form-actions`;
- inline spinner without changing the button label;
- localized success/error alert outside or above the card;
- viewer access renders disabled controls and no Save action.

### User Profile Settings

The User Profile tab provides the single-issuer equivalent of Keycloak's configurable user-profile schema. It follows the same page shell, tab strip, card width, spacing, validation placement, and bottom action row as Login, Email, and Event Settings. There is no realm selector because this application has one issuer and one shared data space.

```text
Settings / User profile
┌────────────────────────────────────────────────────────────┐
│ User profile                                                │
│ Define custom attributes shown on user and account forms.   │
│                                                             │
│ Add attribute                                               │
│ Name [department]  Display name [Department]                │
│ Type [String]      Required [ ]  Multi-valued [ ]           │
│ Min length [ ]     Max length [ ]  Pattern [                 ]│
│ Description [                                             ] │
│                                                             │
│ Configured attributes                                      │
│ Name | Type | Required | Multi-valued | Enabled | Delete     │
│ ...                                                         │
└────────────────────────────────────────────────────────────┘
```

Supported definition controls are `STRING`, `EMAIL`, `INTEGER`, and `BOOLEAN`; requiredness, single/multi-valued behavior, minimum/maximum length, regular-expression validation, enabled state, display order, and a localized description are stored with each definition. Built-in fields such as username, first name, last name, and email remain fixed fields and cannot be redefined as custom attributes.

The admin API is `GET/POST /api/admin/settings/user-profile` for definitions and `PUT/DELETE /api/admin/settings/user-profile/{id}` for changes. User values are exposed through `GET/PUT /api/admin/users/{id}/profile-attributes`; the Account Console uses `GET/PUT /api/account/profile/attributes`. Every mutation validates the submitted values, records an audit event, evicts affected cached data, and invalidates the user's browser sessions and OAuth authorizations when a value changes. The form must show an inline spinner while saving, keep the Save label, disable duplicate submissions, and place field errors directly below the affected control.

### Event Settings

The Event Settings screen is deliberately separate from the history table:

```text
Settings / Events
┌───────────────────────────────────────────────┐
│ Event settings                                │
│ Control event recording, details and retention │
│                                               │
│ [switch] Enable event recording               │
│ [switch] Enable administrative events         │
│ [switch] Include event details                │
│ Retention (days) [                    ]        │
│ Use 0 to retain events indefinitely.          │
│                                               │
│                         [ Save event settings ]│
└───────────────────────────────────────────────┘
```

This placement matches the existing Login and Email Settings screens. The Save action must not float into the card header or sit beside controls. The screen loads through `GET /api/admin/events/config`, saves through `PUT /api/admin/events/config`, and uses the manager authority for mutation.

### Events history

```text
Events
┌────────────────────────────────────────────────────────┐
│ Events                         [count]                  │
│ Audit history for administration and account actions.   │
│                                                         │
│ [Clear all events]                                      │
│ [Search] [filters] [sort]                               │
│                                                         │
│ Time | Action | Actor | Target | Details                │
│ ...                                                     │
│ Pagination                                              │
└────────────────────────────────────────────────────────┘
```

The history table is read-focused. Clear is a danger action in the shared action row, manager-only, and protected by a confirmation dialog. When user events are added, use two tabs or an explicit stream selector so administrative audit records and user authentication events remain distinguishable.

### Sessions and consents

Session screens list active sessions with device/IP/time metadata, support row-level revoke, and provide a manager-only bulk revoke where the product requires it. Consent screens list client, scopes, grant time, and revoke action. Both screens must preserve pagination and invalidate the matching authorization/session state after mutation.

### Account Console

The Account Console should retain a user-centered layout: profile, password, MFA/recovery codes, sessions, consents, required actions, and logout. Dynamic user-profile attributes and WebAuthn devices should appear as additional cards without changing the existing navigation or card grammar. The WebAuthn card now supports passkey registration, credential labels, inventory, and deletion; the browser's native ceremony remains responsible for authenticator verification.

## Product requirements by priority

### P0 — complete the current authorization model

- Keep the current roles and event settings behavior stable.
- Add realm/client ownership to role, client scope, group, and mapper models before multi-tenant support.
- Add client roles, composite roles, standard role claims, and scope filtering.
- Extend group attributes and add protocol mappers; dynamic user profile attributes are implemented for the single issuer.
- Keep Settings and Events visually consistent with the existing screens.
- Maintain session/token invalidation, audit events, cache eviction, OpenAPI schemas, localized messages, native hints, and tests for each mutation.

### P1 — close identity and administration gaps

- Add WebAuthn/passkeys and credential/device management. Registration, primary-factor and conditional sign-in, OTP step-up, account/admin inventory, device metadata, label management, deletion, configurable policy properties, and standard/passwordless required enrollment are covered.
- Add user event persistence and separate User events history.
- Add fine-grained resource permissions for users, groups, clients, roles, and events.
- Add service accounts and offline session/token policy if required by consuming clients.
- Add authentication-flow configuration only after a stable execution model is designed.

### P2 — optional Keycloak platform parity

- Identity brokering and LDAP/AD federation.
- SAML, token exchange, CIBA, DPoP, resource indicators.
- Organizations and organization groups.
- Authorization Services/UMA.
- Event listeners, webhooks, delivery retries, and metrics.
- SCIM, provider SPI, and custom storage extensions.

## Open decisions

1. Does the product need multiple isolated realms, or is one issuer a permanent demo constraint?
2. Should `ROLE_CLIENT_*` remain realm roles for compatibility, or become client roles with a migration bridge?
3. Which user and group attributes are allowed to enter tokens, and which must be redacted?
4. Is the first user-event release limited to authentication events, or does it include account and required-action events?
5. Which external identity providers and directory protocols justify their operational cost?
6. Is resource-server authorization with UMA needed, or are Spring Security authorities sufficient for this product?

## API and persistence requirements

### Application endpoints

| Method | Path | Purpose | Required access |
| --- | --- | --- | --- |
| `GET` | `/api/admin/events/config` | Read event settings | Event viewer or administrator |
| `PUT` | `/api/admin/events/config` | Update event settings | Event manager or administrator |
| `GET` | `/api/admin/events` | Paginated event history | Event viewer or administrator |
| `DELETE` | `/api/admin/events` | Clear event history | Event manager or administrator |

The update endpoint returns the persisted settings representation. The delete endpoint returns no content on success and a centralized Problem Detail response on failure. DTOs must use explicit OpenAPI schemas, examples, requiredness, and validation metadata. Collection endpoints must use bounded `Pageable` input, a stable default sort, and no unbounded response.

### Data model

Event settings require a durable configuration row with:

- event recording enabled;
- administrative events enabled;
- administrative representation/details enabled;
- retention in days or an explicitly documented provider unit;
- created/updated audit metadata.

Event records require an immutable identifier, actor, action/type, target type, target identifier, occurred-at timestamp, and optional details. Foreign-key and filter/order indexes must be added in the existing table changelog for bootstrap changes. New schema changes must be separate Liquibase XML files included from the master changelog, and seed rows must remain CSV-managed where appropriate.

Reference/configuration entities may use Hibernate second-level cache only when the cache region is registered in `CacheConfig` and invalidated by the same service transaction that updates the entity. Event records and high-churn history must remain uncached.

### Native image and resource behavior

Liquibase changelogs, CSV seed data, and i18n bundles must remain available to native builds through focused runtime hints. The existing agent rule already includes the changelog and data resource trees; do not add redundant wildcard registrations for those paths.

## Screen behavior and accessibility

- Every asynchronous save, clear, test, or delete action shows an inline progress spinner while retaining its normal label and disables repeated submission.
- All forms use the shared React Hook Form and Zod boundary, `noValidate`, localized validation messages, and field-level feedback.
- Alerts are announced with appropriate status semantics and remain visible long enough for a keyboard and screen-reader user to understand the result.
- Switches and number fields have associated labels, visible focus states, and usable error text.
- Destructive clear operations require confirmation and explain that the action cannot be undone.
- Tables support keyboard access to details, preserve sort/filter state across pagination, and announce an empty state distinctly from a loading state.
- Read-only viewers see values and explanatory text but do not receive enabled mutation controls.

## Acceptance criteria

### Information architecture

- [ ] The admin shell presents Settings and Events as separate destinations.
- [ ] Settings contains an Events section at `/admin/settings/events`.
- [ ] Events history is available at `/admin/events` and does not duplicate the settings form.
- [ ] Settings tabs and cards match the existing application layout at desktop and mobile widths.
- [ ] Non-admin event viewers/managers see only the Events settings section when they open Settings.

### Event Settings

- [ ] The settings card loads from the API before Save is enabled.
- [ ] Controls are vertically stacked and use the shared spacing and card classes.
- [ ] Save is at the bottom of the form in the shared `admin-form-actions` row.
- [ ] Save shows a spinner, disables repeated submission, resets to the response, and shows localized success/error feedback.
- [ ] Viewer access is read-only and manager access is write-enabled.
- [ ] Invalid retention values are rejected in the browser and by the API.

### Events history

- [ ] History is paginated, newest-first by default, and filterable.
- [ ] Event details are available without exposing JPA entities directly.
- [ ] Clear all requires confirmation, is manager-only, refreshes the table, and creates an audit record.
- [ ] Empty, loading, forbidden, and server-error states are distinct.

### Quality and security

- [ ] Controller, service, authorization, persistence, and UI tests cover viewer and manager decisions.
- [ ] OpenAPI documentation reflects the actual request and response contracts.
- [ ] Liquibase migrations, cache regions, native hints, localized strings, and audit behavior are verified.
- [ ] `./mvnw verify`, frontend formatting, type checking, linting, and unit tests pass.

## Current implementation mapping and gaps

The current sample implements the recommended split with `/admin/settings/events` for event configuration and `/admin/events` for history. The Settings navigation includes Events, the event form uses the shared card and action-row pattern, and the history page owns filtering and clear-history behavior. The backend exposes the four resource-oriented endpoints listed above and protects them with viewer/manager/admin rules.

The following parity items remain product decisions rather than silent assumptions:

1. **User-event storage and UI**: Keycloak has a first-class user-event stream; the current sample's stored stream is administrative. Add a separate event type and UI only when login/account event capture is required.
2. **Saved event types**: Keycloak lets administrators choose which user event types are persisted. Add an allow-list field and an accessible selection dialog when user events are introduced.
3. **Event listeners**: Keycloak exposes listener providers. Add this only with a durable provider model and delivery observability.
4. **Representation details**: The current boolean expresses the policy. Persisting request JSON requires a bounded representation column, redaction rules, and a storage-size limit before enabling it for sensitive data.
5. **Realm isolation**: The current issuer is single-realm from the administrator's perspective. A future multi-tenant design must add a tenant/realm boundary to every resource, cache key, audit record, and authorization check.

## Delivery plan

### Phase 1: Consistent administration shell

Keep the existing resource pages, Settings tab strip, Events history, shared cards, action rows, localized feedback, and role-gated navigation consistent. Complete contract tests and browser checks for viewer and manager flows.

### Phase 2: Keycloak event parity

Add user-event records, saved event types, separate user/admin history tabs, event-type filtering, and representation detail display. Preserve bounded pagination, redaction, retention, and clear-action auditing.

### Phase 3: Extensibility and realm boundaries

Add listener providers, delivery status, tenant/realm isolation, and the remaining resource areas only after their APIs, permission model, persistence, cache behavior, and tests are defined.

## Sources

[^1]: Keycloak, *Server Administration Guide*, current documentation (Admin Console layout, realms, Realm Settings, realm-management roles): <https://www.keycloak.org/docs/latest/server_admin/>
[^2]: Keycloak, *Server Administration Guide 21.0.2*, “Configuring auditing to track events” (user events, admin events, Include Representation, clear actions, event listeners): <https://www.keycloak.org/docs/21.0.2/server_admin/index.html>
[^3]: Keycloak, *Admin REST API*, current documentation (`events/config`, event history, delete-all events, filters): <https://www.keycloak.org/docs-api/latest/rest-api/index.html>
[^4]: Keycloak, *RealmAdminResource Javadocs*, current API signatures for user and admin event history and filters: <https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/services/resources/admin/RealmAdminResource.html>
[^5]: Keycloak, *RealmResource Javadocs*, event and admin-event client resource methods: <https://www.keycloak.org/docs-api/26.6.4/javadocs/org/keycloak/admin/client/resource/RealmResource.html>
[^6]: Keycloak, *Server Administration Guide*, groups and group membership administration: <https://www.keycloak.org/docs/latest/server_admin/#_groups>
[^7]: Keycloak, *Authorization Services Guide*, resources, scopes, policies, permissions, UMA, and RPT: <https://www.keycloak.org/docs/latest/authorization_services/>
[^8]: Keycloak, *Server Administration Guide*, authentication flows and required actions: <https://www.keycloak.org/docs/latest/server_admin/#_authentication-flows>
[^9]: Keycloak, *Server Administration Guide*, identity brokering and user federation: <https://www.keycloak.org/docs/latest/server_admin/#_identity_brokering>
[^10]: Keycloak, *Server Administration Guide*, Organizations: <https://www.keycloak.org/docs/latest/server_admin/#_organizations>
[^11]: Keycloak, *Server Administration Guide*, protocol mappers and client scopes: <https://www.keycloak.org/docs/latest/server_admin/#_client_scopes>
[^12]: Keycloak, *Securing Applications and Services Guide*, token exchange and advanced OAuth capabilities: <https://www.keycloak.org/docs/latest/securing_apps/#_token-exchange>
