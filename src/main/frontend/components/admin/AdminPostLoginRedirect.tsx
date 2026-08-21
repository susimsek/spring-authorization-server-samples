"use client";

import { useEffect } from "react";

const ADMIN_RETURN_TO = "AUTH_ADMIN_RETURN_TO";

export function AdminPostLoginRedirect() {
  useEffect(() => {
    const returnTo = sessionStorage.getItem(ADMIN_RETURN_TO);
    if (!returnTo || !returnTo.startsWith("/")) {
      return;
    }

    sessionStorage.removeItem(ADMIN_RETURN_TO);
    window.location.replace(returnTo);
  }, []);

  return null;
}
