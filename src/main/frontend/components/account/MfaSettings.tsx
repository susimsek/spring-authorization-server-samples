"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Card, Form, Stack } from "react-bootstrap";
import type { Dictionary } from "@/i18n/get-dictionary";
import { ActionIcon } from "@/components/shared/ActionIcon";
import { useAccountAuth } from "./AccountAuthProvider";

type Status = {
  enabled: boolean;
  available: boolean;
  required: boolean;
  issuer: string;
  digits: number;
};
type Setup = { secret: string; otpauthUri: string; digits: number };

export function MfaSettings({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken, clearLocalSession } = useAccountAuth();
  const copy = dictionary.account.security.mfa;
  const [status, setStatus] = useState<Status | null>(null);
  const [setup, setSetup] = useState<Setup | null>(null);
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [codeError, setCodeError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!accessToken) return;
    fetch("/api/account/mfa", {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
      .then(async (response) => {
        if (!response.ok) throw new Error();
        setStatus(await response.json());
      })
      .catch(() => setError(copy.error));
  }, [accessToken, copy.error]);
  if (!status || !status.available) return null;
  const setupMfa = async () => {
    if (!accessToken) return;
    setBusy(true);
    setError(null);
    try {
      const response = await fetch("/api/account/mfa/setup", {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (!response.ok) throw new Error(await responseError(response, copy.error));
      setSetup(await response.json());
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : copy.error);
    } finally {
      setBusy(false);
    }
  };
  const mutate = async (url: string) => {
    const expectedDigits = setup?.digits ?? status.digits;
    if (!accessToken || !new RegExp(`^\\d{${expectedDigits}}$`).test(code)) {
      setCodeError(copy.invalidCode);
      return;
    }
    setBusy(true);
    setError(null);
    setCodeError(null);
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      });
      if (!response.ok) {
        if (response.status === 400) {
          setCodeError(await responseError(response, copy.invalidCode));
          return;
        }
        throw new Error(await responseError(response, copy.error));
      }
      setCode("");
      setSetup(null);
      clearLocalSession();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : copy.error);
    } finally {
      setBusy(false);
    }
  };
  return (
    <Card className="admin-panel-card account-panel-card">
      <Card.Header className="bg-body p-4 border-bottom">
        <h2 className="h5 mb-1">{copy.title}</h2>
        <div className="small text-body-secondary">{copy.help}</div>
      </Card.Header>
      <Card.Body className="p-4">
        <Stack gap={3}>
          {error && <Alert variant="danger">{error}</Alert>}
          {status.enabled ? (
            <>
              <Alert variant="success">{copy.enabled}</Alert>
              <Form.Control
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={status.digits}
                isInvalid={Boolean(codeError)}
                value={code}
                onChange={(event) => {
                  setCode(event.target.value);
                  setCodeError(null);
                }}
                placeholder={copy.code}
              />
              <Form.Control.Feedback type="invalid">{codeError}</Form.Control.Feedback>
              <Button
                variant="danger"
                disabled={busy}
                onClick={() => void mutate("/api/account/mfa/disable")}
              >
                <ActionIcon action="delete" /> {copy.disable}
              </Button>
            </>
          ) : (
            <>
              {!setup && (
                <Button disabled={busy} onClick={() => void setupMfa()}>
                  {copy.setup}
                </Button>
              )}
              {setup && (
                <>
                  <div className="small">
                    <div className="fw-semibold">{copy.secret}</div>
                    <code className="d-block text-break">{setup.secret}</code>
                    <code className="d-block text-break mt-2">{setup.otpauthUri}</code>
                  </div>
                  <Form.Control
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={setup.digits}
                    isInvalid={Boolean(codeError)}
                    value={code}
                    onChange={(event) => {
                      setCode(event.target.value);
                      setCodeError(null);
                    }}
                    placeholder={copy.code}
                  />
                  <Form.Control.Feedback type="invalid">{codeError}</Form.Control.Feedback>
                  <Button disabled={busy} onClick={() => void mutate("/api/account/mfa/enable")}>
                    <ActionIcon action="check" /> {copy.enable}
                  </Button>
                </>
              )}
            </>
          )}
        </Stack>
      </Card.Body>
    </Card>
  );
}

async function responseError(response: Response, fallback: string) {
  try {
    const body = (await response.json()) as { detail?: unknown };
    return typeof body.detail === "string" && body.detail.trim() ? body.detail : fallback;
  } catch {
    return fallback;
  }
}
