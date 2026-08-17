"use client";

import { faGlobe } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { usePathname, useRouter } from "next/navigation";
import { Dropdown } from "react-bootstrap";

import type { Locale } from "@/i18n/config";

type LanguageSwitcherProps = {
  locale: Locale;
  label: string;
};

const languageNames: Record<Locale, string> = {
  en: "English",
  tr: "Türkçe",
};

export function LanguageSwitcher({ locale, label }: LanguageSwitcherProps) {
  const pathname = usePathname();
  const router = useRouter();

  function changeLanguage(nextLocale: Locale) {
    // Cookie persistence is intentionally a browser-side side effect.
    // eslint-disable-next-line react-hooks/immutability
    document.cookie = `AUTH_LOCALE=${nextLocale}; Path=/; Max-Age=31536000; SameSite=Lax`;

    const params = new URLSearchParams(window.location.search);
    const query = params.size ? `?${params.toString()}` : "";
    const localizedPath = replaceLocale(pathname, locale, nextLocale);
    router.push(`${localizedPath}${query}`);
  }

  return (
    <Dropdown align="end">
      <Dropdown.Toggle variant="outline-secondary" size="sm" aria-label={label}>
        <FontAwesomeIcon icon={faGlobe} className="me-2" />
        {languageNames[locale]}
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {(Object.keys(languageNames) as Locale[]).map((language) => (
          <Dropdown.Item
            key={language}
            active={language === locale}
            onClick={() => changeLanguage(language)}
          >
            {languageNames[language]}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
}

function replaceLocale(pathname: string, currentLocale: Locale, nextLocale: Locale) {
  const currentPrefix = `/${currentLocale}`;
  if (pathname === currentPrefix) {
    return `/${nextLocale}`;
  }
  if (pathname.startsWith(`${currentPrefix}/`)) {
    return `/${nextLocale}${pathname.slice(currentPrefix.length)}`;
  }
  return `/${nextLocale}${pathname.startsWith("/") ? pathname : `/${pathname}`}`;
}
