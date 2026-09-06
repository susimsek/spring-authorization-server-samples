"use client";

import { Container, Navbar } from "react-bootstrap";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { Icon } from "@/components/shared/Icon";

import { LanguageSwitcher } from "./LanguageSwitcher";
import { ThemeSwitcher } from "./ThemeSwitcher";

type AuthNavbarProps = {
  locale: Locale;
  dictionary: Dictionary;
};

export function AuthNavbar({ locale, dictionary }: AuthNavbarProps) {
  return (
    <Navbar className="auth-navbar bg-body border-bottom">
      <Container>
        <Navbar.Brand href={`/login`} className="d-flex align-items-center gap-2 fw-semibold">
          <span className="text-primary">
            <Icon icon="shieldHalved" />
          </span>
          {dictionary.brand.product}
        </Navbar.Brand>

        <div className="d-flex align-items-center gap-2">
          <LanguageSwitcher locale={locale} label={dictionary.navbar.language} />
          <ThemeSwitcher dictionary={dictionary} />
        </div>
      </Container>
    </Navbar>
  );
}
