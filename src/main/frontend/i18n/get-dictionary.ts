import type { Locale } from "./config";
import en from "./dictionaries/en.json";
import tr from "./dictionaries/tr.json";

const dictionaries = { en, tr };

export type Dictionary = typeof en;

export function getDictionary(locale: Locale): Dictionary {
  return dictionaries[locale];
}
