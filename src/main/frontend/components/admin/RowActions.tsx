"use client";

import type { ReactNode } from "react";
import { Dropdown } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEllipsisVertical } from "@fortawesome/free-solid-svg-icons";
import { useParams } from "next/navigation";

import { getDictionary } from "@/i18n/get-dictionary";

export function RowActions({ label, children }: { label?: string; children: ReactNode }) {
  const params = useParams<{ lang: string }>();
  const defaultLabel = getDictionary(params?.lang === "tr" ? "tr" : "en").admin.common.actions;
  return (
    <Dropdown align="end">
      <Dropdown.Toggle
        aria-label={label ?? defaultLabel}
        className="admin-row-actions-toggle"
        size="sm"
        variant="link"
      >
        <FontAwesomeIcon icon={faEllipsisVertical} />
      </Dropdown.Toggle>
      <Dropdown.Menu className="shadow-sm">{children}</Dropdown.Menu>
    </Dropdown>
  );
}
