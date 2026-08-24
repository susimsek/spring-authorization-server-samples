import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

import type { Locale } from "@/i18n/config";

type I18nState = {
  locale: Locale;
};

const initialState: I18nState = {
  locale: "en",
};

const i18nSlice = createSlice({
  name: "i18n",
  initialState,
  reducers: {
    setLocale(state, action: PayloadAction<Locale>) {
      state.locale = action.payload;
    },
  },
});

export const { setLocale } = i18nSlice.actions;
export default i18nSlice.reducer;
