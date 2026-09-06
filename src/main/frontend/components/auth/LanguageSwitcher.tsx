"use client";

import { useTranslation } from "react-i18next";
import { useLocale } from "@/i18n/client";
import { persistLocale } from "@/i18n/locale-cookie";
import { Dropdown } from "react-bootstrap";
import { Icon } from "@/components/shared/Icon";

import type { Locale } from "@/i18n/config";

type LanguageSwitcherProps = {
  locale: Locale;
  label: string;
};

const languageNames: Record<Locale, string> = {
  en: "English",
  tr: "Türkçe",
};

export function LanguageSwitcher({ label }: LanguageSwitcherProps) {
  const { i18n } = useTranslation("common");
  const activeLocale = useLocale();

  function changeLanguage(nextLocale: Locale) {
    persistLocale(nextLocale);
    void i18n.changeLanguage(nextLocale);
  }

  return (
    <Dropdown align="end">
      <Dropdown.Toggle
        variant="link"
        size="sm"
        className="console-navbar-control"
        aria-label={label}
      >
        <Icon icon="globe" className="me-2" />
        {languageNames[activeLocale]}
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {(Object.keys(languageNames) as Locale[]).map((language) => (
          <Dropdown.Item
            key={language}
            active={language === activeLocale}
            onClick={() => changeLanguage(language)}
          >
            {languageNames[language]}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
}
