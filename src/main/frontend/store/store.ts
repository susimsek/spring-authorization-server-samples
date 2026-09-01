import { configureStore } from "@reduxjs/toolkit";

import authReducer from "./auth-slice";
import { accountApi } from "./account-api-slice";
import themeReducer from "./theme-slice";

export const makeStore = () =>
  configureStore({
    reducer: {
      auth: authReducer,
      [accountApi.reducerPath]: accountApi.reducer,
      theme: themeReducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(accountApi.middleware),
    // Auth state contains bearer/id tokens; never expose them through Redux DevTools.
    devTools: false,
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
