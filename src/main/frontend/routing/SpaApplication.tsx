"use client";

import { BrowserRouter } from "react-router-dom";
import { ClientI18nProvider } from "@/i18n/client";
import { AppRoutes } from "./AppRoutes";

export default function SpaApplication() {
  return (
    <ClientI18nProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </ClientI18nProvider>
  );
}
