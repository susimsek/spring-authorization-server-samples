"use client";

import { faRightFromBracket, faUser } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { Dropdown } from "react-bootstrap";

export function ConsoleUserMenu({
  username,
  accountHref,
  accountLabel,
  logoutLabel,
  signedInAsLabel,
  onLogout,
  avatarSrc,
}: {
  username: string;
  accountHref: string;
  accountLabel: string;
  logoutLabel: string;
  signedInAsLabel: string;
  onLogout: () => void;
  avatarSrc?: string | null;
}) {
  const [avatarFailed, setAvatarFailed] = useState(false);
  const initial = username.trim().charAt(0).toUpperCase() || "?";

  return (
    <Dropdown align="end">
      <Dropdown.Toggle
        variant="link"
        className="console-user-toggle d-flex align-items-center gap-2 text-decoration-none"
        aria-label={username}
      >
        <span className="console-user-avatar" aria-hidden="true">
          {avatarSrc && !avatarFailed && (
            <Image
              src={avatarSrc}
              alt=""
              width={32}
              height={32}
              unoptimized
              className="console-user-avatar-image"
              onError={() => setAvatarFailed(true)}
            />
          )}
          <span className={avatarFailed ? undefined : "visually-hidden"}>{initial}</span>
        </span>
        <span className="console-user-name d-none d-md-inline text-truncate">{username}</span>
      </Dropdown.Toggle>
      <Dropdown.Menu className="console-user-menu shadow-sm">
        <div className="px-3 py-2 border-bottom">
          <div className="small text-body-secondary">{signedInAsLabel}</div>
          <div className="fw-semibold text-truncate">{username}</div>
        </div>
        <Dropdown.Item as={Link} href={accountHref}>
          <FontAwesomeIcon icon={faUser} className="me-2" />
          {accountLabel}
        </Dropdown.Item>
        <Dropdown.Divider />
        <Dropdown.Item onClick={onLogout}>
          <FontAwesomeIcon icon={faRightFromBracket} className="me-2" />
          {logoutLabel}
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
}
