"use client";

import { useState } from "react";
import { Button, ButtonGroup } from "react-bootstrap";
import { ActionIcon } from "./ActionIcon";

export function RecoveryCodesActions({
  codes,
  labels,
}: {
  codes: string[];
  labels: {
    copy: string;
    copied: string;
    download: string;
    print: string;
  };
}) {
  const [copied, setCopied] = useState(false);
  const text = codes.join("\n");

  const copy = async () => {
    if (!navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard access can be denied by the browser or document policy.
    }
  };

  const download = () => {
    const url = URL.createObjectURL(new Blob([`${text}\n`], { type: "text/plain;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "recovery-codes.txt";
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <ButtonGroup size="sm" aria-label="Recovery code actions">
      <Button variant="secondary" onClick={() => void copy()}>
        <ActionIcon action="copy" />
        {copied ? labels.copied : labels.copy}
      </Button>
      <Button variant="secondary" onClick={download}>
        <ActionIcon action="download" />
        {labels.download}
      </Button>
      <Button variant="secondary" onClick={() => window.print()}>
        <ActionIcon action="print" />
        {labels.print}
      </Button>
    </ButtonGroup>
  );
}
