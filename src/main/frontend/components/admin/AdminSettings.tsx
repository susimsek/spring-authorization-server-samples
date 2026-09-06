"use client";

import { useEffect, useState } from "react";
import { Alert, Card } from "react-bootstrap";

import { useParams } from "@/routing/navigation";
import { useDictionary } from "@/i18n/client";
import { adminRequest } from "@/lib/admin-api";

import { AdminPageHeader } from "./AdminPageHeader";
import { useAdminAuth } from "./AdminAuthProvider";
import { DetailTabs } from "./DetailTabs";
import EmailSettings from "./EmailSettings";
import LoginSettings, { type LoginSettingsSection } from "./LoginSettings";

const SETTINGS_SECTIONS = [
  "general",
  "login",
  "email",
  "password-policy",
  "otp-policy",
  "brute-force",
  "sessions",
] as const;
type SettingsSection = (typeof SETTINGS_SECTIONS)[number];

type ServerInfo = { issuer: string };

export default function AdminSettings() {
  const dictionary = useDictionary();
  const { section: candidate } = useParams<{ section?: string }>();
  const active = SETTINGS_SECTIONS.includes(candidate as SettingsSection) ? candidate! : "general";
  const tabs = [
    { key: "general", label: dictionary.admin.settings.sections.general, href: "/admin/settings" },
    {
      key: "login",
      label: dictionary.admin.settings.sections.login,
      href: "/admin/settings/login",
    },
    {
      key: "email",
      label: dictionary.admin.settings.sections.email,
      href: "/admin/settings/email",
    },
    {
      key: "password-policy",
      label: dictionary.admin.settings.sections.passwordPolicy,
      href: "/admin/settings/password-policy",
    },
    {
      key: "otp-policy",
      label: dictionary.admin.settings.sections.otpPolicy,
      href: "/admin/settings/otp-policy",
    },
    {
      key: "brute-force",
      label: dictionary.admin.settings.sections.bruteForce,
      href: "/admin/settings/brute-force",
    },
    {
      key: "sessions",
      label: dictionary.admin.settings.sections.sessions,
      href: "/admin/settings/sessions",
    },
  ] satisfies Array<{ key: SettingsSection; label: string; href: string }>;

  return (
    <>
      <AdminPageHeader
        title={dictionary.admin.settings.title}
        description={dictionary.admin.settings.subtitle}
      />
      <DetailTabs tabs={tabs} active={active} />
      {active === "general" ? <GeneralSettings /> : null}
      {active === "email" ? <EmailSettings embedded /> : null}
      {active !== "general" && active !== "email" ? (
        <LoginSettings embedded focusSection={active as LoginSettingsSection} />
      ) : null}
    </>
  );
}

function GeneralSettings() {
  const dictionary = useDictionary();
  const { accessToken } = useAdminAuth();
  const [issuer, setIssuer] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<ServerInfo>(accessToken, { url: "/api/admin/server-info" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setIssuer(response.data.issuer);
      })
      .catch(() => setError(true));
  }, [accessToken]);

  return (
    <Card className="admin-panel-card">
      <Card.Body>
        <h2 className="h5 mb-2">{dictionary.admin.settings.generalTitle}</h2>
        <p className="text-body-secondary mb-4">{dictionary.admin.settings.generalDescription}</p>
        {error && <Alert variant="danger">{dictionary.admin.settings.generalError}</Alert>}
        <dl className="row mb-0">
          <dt className="col-sm-4">{dictionary.admin.settings.issuer}</dt>
          <dd className="col-sm-8 font-monospace text-break mb-0">{issuer ?? "—"}</dd>
        </dl>
        <p className="small text-body-secondary mt-4 mb-0">
          {dictionary.admin.settings.generalReadOnly}
        </p>
      </Card.Body>
    </Card>
  );
}
