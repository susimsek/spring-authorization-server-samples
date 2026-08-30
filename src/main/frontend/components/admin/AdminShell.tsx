"use client";

import {
  faAddressCard,
  faBars,
  faGaugeHigh,
  faClockRotateLeft,
  faCircleInfo,
  faKey,
  faLaptop,
  faLayerGroup,
  faShieldHalved,
  faUserShield,
  faUsers,
  faXmark,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Button, Container, Nav, Navbar } from "react-bootstrap";

import { LanguageSwitcher } from "@/components/auth/LanguageSwitcher";
import { ThemeSwitcher } from "@/components/auth/ThemeSwitcher";
import { ConsoleUserMenu } from "@/components/auth/ConsoleUserMenu";
import { ConsoleAlertsProvider } from "@/components/auth/ConsoleAlerts";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { useAdminAuth } from "./AdminAuthProvider";

type Props = {
  locale: Locale;
  dictionary: Dictionary;
  children: React.ReactNode;
};

export function AdminShell({ locale, dictionary, children }: Props) {
  const pathname = usePathname();
  const { access, idTokenParsed, logout, tokenParsed, username } = useAdminAuth();
  const [navigationOpen, setNavigationOpen] = useState(false);
  const items = [
    ["", dictionary.admin.nav.dashboard, faGaugeHigh, true],
    ["/clients", dictionary.admin.nav.clients, faAddressCard, access?.viewClients],
    ["/client-scopes", dictionary.admin.nav.clientScopes, faLayerGroup, access?.viewClients],
    ["/users", dictionary.admin.nav.users, faUsers, access?.viewUsers],
    ["/roles", dictionary.admin.nav.roles, faUserShield, access?.manageRoles],
    ["/groups", dictionary.admin.nav.groups, faLayerGroup, access?.manageRoles],
    ["/sessions", dictionary.admin.nav.sessions, faLaptop, access?.viewSessions],
    ["/consents", dictionary.admin.nav.consents, faShieldHalved, access?.viewConsents],
    ["/keys", dictionary.admin.nav.keys, faKey, access?.viewKeys],
    ["/events", dictionary.admin.nav.events, faClockRotateLeft, access?.viewUsers],
    ["/server-info", dictionary.admin.nav.serverInfo, faCircleInfo, access?.viewKeys],
  ] as const;

  return (
    <ConsoleAlertsProvider>
      <div className="admin-app min-vh-100 bg-body-tertiary">
        <Navbar className="admin-navbar bg-body border-bottom" sticky="top">
          <Container fluid className="admin-navbar-inner px-3 px-lg-4">
            <div className="d-flex align-items-center min-w-0">
              <Button
                variant="link"
                className="admin-menu-toggle d-lg-none me-2"
                aria-label={dictionary.admin.common.toggleNavigation}
                aria-expanded={navigationOpen}
                onClick={() => setNavigationOpen((current) => !current)}
              >
                <FontAwesomeIcon icon={navigationOpen ? faXmark : faBars} />
              </Button>
              <Navbar.Brand
                as={Link}
                href={`/${locale}/admin`}
                className="admin-brand d-flex align-items-center gap-2 fw-semibold mb-0"
              >
                <span className="admin-brand-mark">
                  <FontAwesomeIcon icon={faShieldHalved} />
                </span>
                <span className="admin-brand-copy text-truncate">{dictionary.admin.product}</span>
              </Navbar.Brand>
            </div>

            <div className="admin-navbar-actions d-flex align-items-center gap-2">
              <LanguageSwitcher locale={locale} label={dictionary.navbar.language} />
              <ThemeSwitcher dictionary={dictionary} />
              <ConsoleUserMenu
                username={username ?? "…"}
                avatarSrc={idTokenParsed?.picture ?? tokenParsed?.picture}
                accountHref={`/${locale}/account/personal-info`}
                accountLabel={dictionary.account.product}
                logoutLabel={dictionary.admin.common.logout}
                signedInAsLabel={dictionary.admin.common.signedInAs}
                onLogout={() => {
                  void logout(locale);
                }}
              />
            </div>
          </Container>
        </Navbar>

        <div className="admin-layout">
          {navigationOpen && (
            <button
              type="button"
              className="admin-sidebar-backdrop d-lg-none"
              aria-label={dictionary.admin.common.closeNavigation}
              onClick={() => setNavigationOpen(false)}
            />
          )}
          <aside className={`admin-sidebar bg-body border-end${navigationOpen ? " is-open" : ""}`}>
            <div className="admin-sidebar-header px-3 pt-4 pb-2">
              <div className="admin-sidebar-label small text-body-secondary text-uppercase fw-semibold">
                {dictionary.admin.manage}
              </div>
            </div>
            <Nav className="flex-column px-2 gap-1">
              {items
                .filter(([, , , allowed]) => allowed)
                .map(([suffix, label, icon]) => {
                  const href = `/${locale}/admin${suffix}`;
                  const active =
                    suffix === ""
                      ? pathname === href || pathname === `${href}/`
                      : pathname.startsWith(href);

                  return (
                    <Nav.Link
                      key={href}
                      as={Link}
                      href={href}
                      active={active}
                      className="admin-nav-link rounded-2"
                      onClick={() => setNavigationOpen(false)}
                    >
                      <span className="admin-nav-icon-wrap">
                        <FontAwesomeIcon icon={icon} className="admin-nav-icon" />
                      </span>
                      <span>{label}</span>
                    </Nav.Link>
                  );
                })}
            </Nav>
            <div className="admin-sidebar-footer mt-auto px-3 py-3 small text-body-secondary">
              OAuth 2.0 · OpenID Connect
            </div>
          </aside>
          <main className="admin-main">
            <div className="admin-main-inner">{children}</div>
          </main>
        </div>
      </div>
    </ConsoleAlertsProvider>
  );
}
