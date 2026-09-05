"use client";

import { faEye, faEyeSlash, faLock } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { type ComponentProps, useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";

type PasswordFieldProps = {
  controlId?: string;
  label: string;
  autoComplete?: string;
  placeholder: string;
  showLabel: string;
  hideLabel: string;
  inputProps?: ComponentProps<typeof Form.Control>;
};

export function PasswordField({
  controlId = "password",
  label,
  autoComplete = "current-password",
  placeholder,
  showLabel,
  hideLabel,
  inputProps,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Form.Group className="mb-4" controlId={controlId}>
      <Form.Label>{label}</Form.Label>
      <InputGroup>
        <InputGroup.Text>
          <FontAwesomeIcon icon={faLock} />
        </InputGroup.Text>
        <Form.Control
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          placeholder={placeholder}
          {...inputProps}
        />
        <Button
          variant="link"
          className="console-input-action"
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
