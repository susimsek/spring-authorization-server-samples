# Frontend AI Agent Guidelines

These instructions apply to `src/main/frontend/**` and supplement the repository-root `AGENTS.md`. The root instructions remain applicable unless this file provides a more specific frontend rule.

## Table of Contents

1.  [Quick Reference](#quick-reference)
2.  [Project Structure](#project-structure)
3.  [Code Style and Quality Gates](#code-style-and-quality-gates)
4.  [Testing Guidelines](#testing-guidelines)
5.  [Authentication](#authentication)
6.  [Development Guidelines](#development-guidelines)
7.  [Common Mistakes to Avoid](#common-mistakes-to-avoid)

## Quick Reference

| Action               | Command                                   |
| -------------------- | ----------------------------------------- |
| Install dependencies | `corepack pnpm install --frozen-lockfile` |
| Format check         | `pnpm format:check`                       |
| Type check           | `pnpm typecheck`                          |
| Lint                 | `pnpm lint`                               |
| Unit/component tests | `pnpm test:unit`                          |
| Production build     | `pnpm build`                              |
| Cypress E2E tests    | `pnpm test:e2e`                           |

## Project Structure

- `app`: static SPA entry and explicit OAuth callback entries. `routing/AppRoutes.tsx` owns browser-only Administration, Account and public routes; no locale prefixes or build-time dynamic parameters.
- `components/admin`: Administration Console UI and shared administrative list/detail/form components.
- `components/account`: Account Console UI and account-management components.
- `components/auth`: login, consent, locale, theme, and shared authentication UI.
- `lib/console-auth.ts`: shared browser OIDC Authorization Code + PKCE, refresh-token, and logout adapter.
- `lib/admin-api.ts` and `lib/account-api.ts`: authenticated Administration and Account Console API clients.
- `locales/en` and `locales/tr`: English and Turkish user-facing messages.
- `cypress`: browser E2E specifications and support commands.

## Code Style and Quality Gates

- Use TypeScript for application code. Do not weaken type safety with unnecessary `any`, unchecked casts, or ignored type errors.
- Follow `.editorconfig`: LF line endings, final newline, and no trailing whitespace. Do not edit generated build output.
- Use the repository's configured formatter and ESLint rules. Run `pnpm format:check`, `pnpm typecheck`, and `pnpm lint` after frontend changes.
- Keep components focused. Put API requests in `lib`, reusable UI primitives in `components`, and route composition in `routing`.
- Keep English and Turkish dictionaries aligned when adding or changing user-visible text. Do not hard-code user-facing strings in components when a dictionary key is appropriate.

## Testing Guidelines

- Keep Jest tests close to the component or library behavior they cover and use the existing test naming conventions.
- Test observable behavior: loading, successful state, empty state, validation, failed requests, authorization failures, and mutation feedback where relevant.
- New or materially changed console flows require component coverage and Cypress E2E coverage for the affected login, refresh/reload, direct deep-link, mutation, error, and logout paths.
- Keep Cypress route assertions aligned with the unprefixed route contract.
- Run `pnpm build` after changes to routes, static-export configuration, or production rendering behavior.

## Authentication

- The frontend is a Next.js static export served by Spring Boot. Spring Security continues to process `POST /login`; CSRF is intentionally disabled in this sample.
- Keep console authentication in `lib/console-auth.ts` aligned with the project's Keycloak-style model: Authorization Code + PKCE, namespaced console token records, single-flight refresh near expiry, one retry after a 401, and OIDC logout with an ID-token hint and registered post-logout URI.
- Persist each console's access, ID, and refresh token set only in its namespaced `localStorage` record. Browser SSO remains supplied by the server-side Spring Session; never copy a token set from one console to the other.
- Use `adminRequest` for Administration Console APIs and `accountRequest` for Account Console APIs. Do not add parallel Axios clients, custom bearer-token handling, or a second console authentication flow.
- Console routes use `/admin/` and `/account/`. Callback routes must remain aligned with registered-client redirect URIs. `next-i18next` manages client locale with `localeInPath: false`: `locale` cookie, browser languages, then English.

## Development Guidelines

### Forms and Validation

- All user-editable create and update forms must use React Hook Form with a Zod schema and `zodResolver`.
- Do not manage submitted field values or validation errors with component-local state. Keep only transient UI state local, such as search input, selected table row, modal visibility, and loading state.
- Form validation messages must come from the localized dictionary. Map backend `ProblemDetail` violations to the matching React Hook Form fields where applicable.
- Disable submission while a mutation is pending, show localized success/error feedback, and reset form values only after a successful server response.

### Lists and API Responses

- Use `DataTable`, `ResourceFilters`, `PaginationControls`, and `useAdminTableState` for pageable console lists.
- Send `page`, `size`, `sort`, and filters to the server. Do not fetch a full pageable resource with `size=100` merely to render a table.
- When a screen contains multiple independent pageable lists, namespace their URL query parameters so one list cannot overwrite another list's pagination or filters.
- Use `whoami` access flags for navigation and action visibility, but handle backend 401/403 responses gracefully because backend authorization is authoritative.

### Routes and Static Export

- Every new static-export route must update route validation and include SPA fallback test coverage.
- Do not generate placeholder routes or use `generateStaticParams`. React Router resolves dynamic identifiers at runtime; Spring forwards frontend GET/HEAD HTML navigation to `/index.html`.
- Keep the login, Administration Console, and Account Console UI in `src/main/frontend`. Do not move application authentication behavior into Next.js.

## Common Mistakes to Avoid

- Replacing the shared Keycloak-style OIDC adapter with a custom token flow.
- Sharing or copying Admin and Account token records, or storing tokens outside the shared console-auth adapter.
- Adding a second HTTP client or manually attaching bearer tokens outside `adminRequest`, `accountRequest`, and the shared adapter.
- Using local component state instead of React Hook Form + Zod for editable form models.
- Fetching complete pageable resources to implement client-side pagination.
- Adding a new frontend route without static-export and fallback coverage.
- Hiding an action in the UI and treating that as an authorization control.
