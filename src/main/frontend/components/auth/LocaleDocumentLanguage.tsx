"use client";

import { useEffect } from "react";

type LocaleDocumentLanguageProps = {
  lang: string;
  children: React.ReactNode;
};

export function LocaleDocumentLanguage({ lang, children }: LocaleDocumentLanguageProps) {
  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);

  return children;
}
