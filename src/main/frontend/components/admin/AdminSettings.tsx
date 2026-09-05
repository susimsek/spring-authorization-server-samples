"use client";

import { useParams } from "@/routing/navigation";
import { useDictionary } from "@/i18n/client";

import { AdminPageHeader } from "./AdminPageHeader";
import { DetailTabs } from "./DetailTabs";
import EmailSettings from "./EmailSettings";
import LoginSettings from "./LoginSettings";

const SETTINGS_TABS = ["login", "email"] as const;

export default function AdminSettings() {
  const dictionary = useDictionary();
  const { section: candidate } = useParams<{ section?: string }>();
  const active = SETTINGS_TABS.includes(candidate as (typeof SETTINGS_TABS)[number])
    ? candidate!
    : "login";
  const tabs = [
    {
      key: "login",
      label: dictionary.admin.loginSettings.title,
      href: "/admin/settings/login",
    },
    {
      key: "email",
      label: dictionary.admin.emailSettings.title,
      href: "/admin/settings/email",
    },
  ];

  return (
    <>
      <AdminPageHeader
        title={dictionary.admin.settings.title}
        description={dictionary.admin.settings.subtitle}
      />
      <DetailTabs tabs={tabs} active={active} />
      {active === "login" ? <LoginSettings embedded /> : <EmailSettings embedded />}
    </>
  );
}
