"use client";

import { I18nProvider } from "next-i18next/client";
import { useEffect, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import config from "@/i18n.config";
import en from "@/locales/en/common.json";
import { defaultLocale, isLocale } from "./config";
import type { Dictionary } from "./get-dictionary";
import { detectLocale, persistLocale } from "./locale-cookie";
import { setLocale } from "@/store/locale-slice";
import { useAppDispatch } from "@/store/hooks";

export function ClientI18nProvider({ children }: { children: ReactNode }) {
  const [initialLocale] = useState(() => detectLocale(document.cookie, navigator.languages));
  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(setLocale(initialLocale));
  }, [dispatch, initialLocale]);
  return (
    <I18nProvider
      language={initialLocale}
      resources={config.resources}
      supportedLngs={config.supportedLngs}
      fallbackLng={config.fallbackLng}
      defaultNS={config.defaultNS}
    >
      <LocaleEffects />
      {children}
    </I18nProvider>
  );
}

function LocaleEffects() {
  const locale = useLocale();
  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(setLocale(locale));
    document.documentElement.lang = locale;
    persistLocale(locale);
  }, [dispatch, locale]);
  return null;
}

export function useLocale() {
  const { i18n } = useTranslation("common");
  const language = i18n.resolvedLanguage ?? i18n.language;
  return isLocale(language ?? "") ? (language as "en" | "tr") : defaultLocale;
}

/** Typed translation objects for the existing presentational component props. */
export function useDictionary(): Dictionary {
  const { i18n } = useTranslation("common");
  const language = i18n.resolvedLanguage ?? defaultLocale;
  return (i18n.getResourceBundle?.(language, "common") ?? en) as Dictionary;
}
