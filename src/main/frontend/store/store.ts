import { configureStore } from "@reduxjs/toolkit";

import authReducer from "./auth-slice";
import i18nReducer from "./i18n-slice";
import themeReducer from "./theme-slice";

export const makeStore = () =>
  configureStore({
    reducer: {
      auth: authReducer,
      i18n: i18nReducer,
      theme: themeReducer,
    },
    // Auth state contains bearer/id tokens; never expose them through Redux DevTools.
    devTools: false,
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
