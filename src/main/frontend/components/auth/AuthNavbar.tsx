"use client";

import { faShieldHalved } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { Container, Navbar } from "react-bootstrap";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";

import { LanguageSwitcher } from "./LanguageSwitcher";
import { ThemeSwitcher } from "./ThemeSwitcher";

type AuthNavbarProps = {
  locale: Locale;
  dictionary: Dictionary;
};

export function AuthNavbar({ locale, dictionary }: AuthNavbarProps) {
  return (
    <Navbar className="bg-body border-bottom">
      <Container>
        <Navbar.Brand
          href={`/${locale}/login`}
          className="d-flex align-items-center gap-2 fw-semibold"
        >
          <span className="text-primary">
            <FontAwesomeIcon icon={faShieldHalved} />
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
