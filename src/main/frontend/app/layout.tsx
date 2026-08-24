import "bootstrap/dist/css/bootstrap.min.css";
import "@fortawesome/fontawesome-svg-core/styles.css";
import "./styles.css";

import { AdminAuthProvider } from "@/components/admin/AdminAuthProvider";
import { AccountAuthProvider } from "@/components/account/AccountAuthProvider";
import { StoreProvider } from "@/store/StoreProvider";
import { ThemeManager } from "@/components/auth/ThemeManager";

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" data-bs-theme="light" suppressHydrationWarning>
      <head>
        <meta name="color-scheme" content="light dark" />
      </head>
      <body>
        <StoreProvider>
          <ThemeManager />
          <AdminAuthProvider>
            <AccountAuthProvider>{children}</AccountAuthProvider>
          </AdminAuthProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
