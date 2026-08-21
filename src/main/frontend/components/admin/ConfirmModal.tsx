"use client";

import { Button, Modal } from "react-bootstrap";

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
          {cancelLabel}
        </Button>
        <Button disabled={busy} onClick={onConfirm} type="button" variant="danger">
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
