"use client";

import { faCircleQuestion } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { OverlayTrigger, Tooltip } from "react-bootstrap";

export function HelpItem({ label, help }: { label: string; help: string }) {
  return (
    <span className="d-inline-flex align-items-center gap-1">
      <span>{label}</span>
      <OverlayTrigger placement="top" overlay={<Tooltip>{help}</Tooltip>}>
        <button className="console-help-button" type="button" aria-label={`${label} help`}>
          <FontAwesomeIcon icon={faCircleQuestion} />
        </button>
      </OverlayTrigger>
    </span>
  );
}
