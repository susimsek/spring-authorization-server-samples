"use client";

import { useEffect } from "react";

import { isLocale } from "@/i18n/config";
import { useAppDispatch } from "@/store/hooks";
import { setLocale } from "@/store/i18n-slice";

type LocaleDocumentLanguageProps = {
  lang: string;
  children: React.ReactNode;
};

export function LocaleDocumentLanguage({ lang, children }: LocaleDocumentLanguageProps) {
  const dispatch = useAppDispatch();

  useEffect(() => {
    document.documentElement.lang = lang;
    if (isLocale(lang)) dispatch(setLocale(lang));
  }, [dispatch, lang]);

  return children;
}
