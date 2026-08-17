import "bootstrap/dist/css/bootstrap.min.css";
import "@fortawesome/fontawesome-svg-core/styles.css";
import "./styles.css";

import { config } from "@fortawesome/fontawesome-svg-core";

config.autoAddCss = false;

const themeScript = `
(() => {
  try {
    const stored = localStorage.getItem("AUTH_THEME");
    const theme = stored === "light" || stored === "dark" || stored === "system" ? stored : "system";
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    document.documentElement.setAttribute(
      "data-bs-theme",
      theme === "system" ? (prefersDark ? "dark" : "light") : theme,
    );
  } catch {
    document.documentElement.setAttribute("data-bs-theme", "light");
  }
})();
`;

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <meta name="color-scheme" content="light dark" />
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
