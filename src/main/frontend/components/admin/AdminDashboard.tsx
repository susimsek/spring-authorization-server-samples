"use client";

import { useEffect, useState } from "react";

import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";

import { useAdminAuth } from "./AdminAuthProvider";

type Dashboard = {
  clients: number;
  users: number;
  sessions: number;
  consents: number;
};

export function AdminDashboard({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAdminAuth();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const controller = new AbortController();
    adminRequest<Dashboard>(accessToken, { url: "/api/admin/dashboard", signal: controller.signal })
      .then((response) => {
        if (response.status === 200) {
          setDashboard(response.data);
        }
      })
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setDashboard(null);
        }
      });

    return () => controller.abort();
  }, [accessToken]);

  const cards = [
    [dictionary.admin.nav.clients, dashboard?.clients],
    [dictionary.admin.nav.users, dashboard?.users],
    [dictionary.admin.nav.sessions, dashboard?.sessions],
    [dictionary.admin.nav.consents, dashboard?.consents],
  ];

  return (
    <div className="row g-3">
      {cards.map(([label, count]) => (
        <div className="col-md-6 col-xl-3" key={label}>
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <div className="text-body-secondary small">{label}</div>
              <div className="h3 mb-0">{count ?? "-"}</div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
