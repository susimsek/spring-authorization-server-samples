"use client";

import { Button, Modal, Spinner } from "react-bootstrap";

import { ActionIcon } from "@/components/shared/ActionIcon";

export function ConfirmModal({
  show,
  message,
  cancelLabel,
  confirmLabel,
  busy = false,
  onCancel,
  onConfirm,
}: {
  show: boolean;
  message: string;
  cancelLabel: string;
  confirmLabel: string;
  busy?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <Modal centered onHide={onCancel} show={show}>
      <Modal.Body>{message}</Modal.Body>
      <Modal.Footer>
        <Button disabled={busy} onClick={onCancel} type="button" variant="secondary">
          <ActionIcon action="cancel" />
          {cancelLabel}
        </Button>
        <Button disabled={busy} onClick={onConfirm} type="button" variant="danger">
          {busy ? (
            <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
          ) : (
            <ActionIcon action="check" />
          )}
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
