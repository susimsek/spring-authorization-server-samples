"use client";

import { faDesktop, faMoon, faSun } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useEffect, useSyncExternalStore } from "react";
import { Dropdown } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";

import { isTheme, resolveTheme, THEME_STORAGE_KEY, type Theme } from "./theme";

type ThemeSwitcherProps = {
  dictionary: Dictionary;
};

const themeIcons = { system: faDesktop, light: faSun, dark: faMoon } as const;
const THEME_CHANGE_EVENT = "auth-theme-change";

export function ThemeSwitcher({ dictionary }: ThemeSwitcherProps) {
  const theme = useSyncExternalStore(subscribeTheme, getThemeSnapshot, getServerThemeSnapshot);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const applyTheme = () => {
      document.documentElement.setAttribute(
        "data-bs-theme",
        resolveTheme(theme, mediaQuery.matches),
      );
    };

    applyTheme();
    if (theme === "system") {
      mediaQuery.addEventListener("change", applyTheme);
      return () => mediaQuery.removeEventListener("change", applyTheme);
    }
    return undefined;
  }, [theme]);

  function changeTheme(nextTheme: Theme) {
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    window.dispatchEvent(new Event(THEME_CHANGE_EVENT));
  }

  const labels: Record<Theme, string> = {
    system: dictionary.theme.system,
    light: dictionary.theme.light,
    dark: dictionary.theme.dark,
  };

  return (
    <Dropdown align="end">
      <Dropdown.Toggle variant="outline-secondary" size="sm" aria-label={dictionary.theme.label}>
        <FontAwesomeIcon icon={themeIcons[theme]} className="me-2" />
        {labels[theme]}
      </Dropdown.Toggle>
      <Dropdown.Menu>
        {(Object.keys(labels) as Theme[]).map((value) => (
          <Dropdown.Item key={value} active={value === theme} onClick={() => changeTheme(value)}>
            <FontAwesomeIcon icon={themeIcons[value]} className="me-2" />
            {labels[value]}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
}

function subscribeTheme(callback: () => void) {
  const handleStorage = (event: StorageEvent) => {
    if (event.key === THEME_STORAGE_KEY) {
      callback();
    }
  };
  const handleThemeChange = () => callback();

  window.addEventListener("storage", handleStorage);
  window.addEventListener(THEME_CHANGE_EVENT, handleThemeChange);
  return () => {
    window.removeEventListener("storage", handleStorage);
    window.removeEventListener(THEME_CHANGE_EVENT, handleThemeChange);
  };
}

function getThemeSnapshot(): Theme {
  const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  return isTheme(storedTheme) ? storedTheme : "system";
}

function getServerThemeSnapshot(): Theme {
  return "system";
}
