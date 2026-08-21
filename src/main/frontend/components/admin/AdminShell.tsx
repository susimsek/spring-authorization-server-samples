"use client";

import {
  faAddressCard,
  faGaugeHigh,
  faKey,
  faLaptop,
  faRightFromBracket,
  faShieldHalved,
  faUserShield,
  faUsers,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Button, Container, Nav, Navbar } from "react-bootstrap";

import { LanguageSwitcher } from "@/components/auth/LanguageSwitcher";
import { ThemeSwitcher } from "@/components/auth/ThemeSwitcher";
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
  const router = useRouter();
  const { access, logout } = useAdminAuth();
  const items = [
    ["", dictionary.admin.nav.dashboard, faGaugeHigh, true],
    ["/clients", dictionary.admin.nav.clients, faAddressCard, access?.viewClients],
    ["/users", dictionary.admin.nav.users, faUsers, access?.viewUsers],
    ["/roles", dictionary.admin.nav.roles, faUserShield, access?.manageRoles],
    ["/sessions", dictionary.admin.nav.sessions, faLaptop, access?.viewSessions],
    ["/consents", dictionary.admin.nav.consents, faShieldHalved, access?.viewConsents],
    ["/keys", dictionary.admin.nav.keys, faKey, access?.viewKeys],
  ] as const;

  return (
    <div className="min-vh-100 bg-body-tertiary">
      <Navbar className="bg-body border-bottom admin-navbar" sticky="top">
        <Container fluid className="px-3 px-lg-4">
          <Navbar.Brand
            as={Link}
            href={`/${locale}/admin`}
            className="d-flex align-items-center gap-2 fw-semibold"
          >
            <span className="admin-brand-mark">
              <FontAwesomeIcon icon={faShieldHalved} />
            </span>
            {dictionary.admin.product}
          </Navbar.Brand>
          <div className="d-flex align-items-center gap-2">
            <LanguageSwitcher locale={locale} label={dictionary.navbar.language} />
            <ThemeSwitcher dictionary={dictionary} />
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => {
                void logout().finally(() => router.push(`/${locale}/login`));
              }}
            >
              <FontAwesomeIcon icon={faRightFromBracket} className="me-1" />
              {dictionary.admin.common.logout}
            </Button>
          </div>
        </Container>
      </Navbar>
      <div className="admin-layout">
        <aside className="admin-sidebar bg-body border-end">
          <div className="px-3 pt-4 pb-2 small text-body-secondary text-uppercase fw-semibold">
            {dictionary.admin.manage}
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
                  >
                    <FontAwesomeIcon icon={icon} className="admin-nav-icon" />
                    <span>{label}</span>
                  </Nav.Link>
                );
              })}
          </Nav>
        </aside>
        <main className="admin-main">{children}</main>
      </div>
    </div>
  );
}
