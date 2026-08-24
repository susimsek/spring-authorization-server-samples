"use client";

import type { ReactNode } from "react";
import { Dropdown } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faEllipsisVertical } from "@fortawesome/free-solid-svg-icons";

export function RowActions({
  label = "Actions",
  children,
}: {
  label?: string;
  children: ReactNode;
}) {
  return (
    <Dropdown align="end">
      <Dropdown.Toggle
        aria-label={label}
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
