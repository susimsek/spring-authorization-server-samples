"use client";

import { faEye, faEyeSlash, faLock } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";

type PasswordFieldProps = {
  label: string;
  placeholder: string;
  showLabel: string;
  hideLabel: string;
};

export function PasswordField({ label, placeholder, showLabel, hideLabel }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Form.Group className="mb-4" controlId="password">
      <Form.Label>{label}</Form.Label>
      <InputGroup>
        <InputGroup.Text>
          <FontAwesomeIcon icon={faLock} />
        </InputGroup.Text>
        <Form.Control
          name="password"
          type={visible ? "text" : "password"}
          autoComplete="current-password"
          placeholder={placeholder}
          required
        />
        <Button
          variant="outline-secondary"
          type="button"
          aria-label={visible ? hideLabel : showLabel}
          onClick={() => setVisible((value) => !value)}
        >
          <FontAwesomeIcon icon={visible ? faEyeSlash : faEye} />
        </Button>
      </InputGroup>
    </Form.Group>
  );
}
