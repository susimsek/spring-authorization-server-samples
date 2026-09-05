"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Card, Form, Stack } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useSearchParams } from "@/routing/navigation";
import type { Dictionary } from "@/i18n/get-dictionary";
import { ActionIcon } from "@/components/shared/ActionIcon";

export function MfaChallengePage({ dictionary }: { dictionary: Dictionary }) {
  const copy = dictionary.mfa;
  const returnTo = safeReturnTo(useSearchParams().get("return_to"));
  const [failed, setFailed] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const schema = z.object({ code: z.string().regex(/^\d{6,8}$/, copy.invalidCode) });
  const {
    register,
    handleSubmit,
    clearErrors,
    setError,
    formState: { errors },
  } = useForm<{ code: string }>({
    resolver: zodResolver(schema),
  });
  const submit = handleSubmit(async ({ code }) => {
    setBusy(true);
    setFailed(null);
    clearErrors("code");
    try {
      const response = await fetch("/api/auth/mfa/verify", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      });
      if (response.status === 401) {
        window.location.assign(returnTo);
        return;
      }
      if (!response.ok) {
        setError("code", {
          type: "server",
          message: await responseError(response, copy.invalidCode),
        });
        return;
      }
      window.location.assign(returnTo);
    } catch {
      setFailed(copy.invalidCode);
    } finally {
      setBusy(false);
    }
  });
  return (
    <Card className="auth-card">
      <Card.Body className="p-4 p-md-5">
        <Stack gap={3}>
          <span className="text-primary text-uppercase fw-semibold small">{copy.eyebrow}</span>
          <h1 className="h3 fw-bold mb-0">{copy.title}</h1>
          <p className="text-body-secondary mb-0">{copy.subtitle}</p>
          {failed && <Alert variant="danger">{failed}</Alert>}
          <Form onSubmit={submit} noValidate>
            <Form.Group className="mb-4" controlId="mfa-code">
              <Form.Label>{copy.code}</Form.Label>
              <Form.Control
                inputMode="numeric"
                autoComplete="one-time-code"
                autoFocus
                isInvalid={Boolean(errors.code)}
                {...register("code")}
              />
              <Form.Control.Feedback type="invalid">{errors.code?.message}</Form.Control.Feedback>
            </Form.Group>
            <Button type="submit" disabled={busy} className="w-100">
              <ActionIcon action="check" /> {busy ? copy.verifying : copy.verify}
            </Button>
          </Form>
        </Stack>
      </Card.Body>
    </Card>
  );
}

function safeReturnTo(value: string | null) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) return "/";
  return value;
}

async function responseError(response: Response, fallback: string) {
  try {
    const body = (await response.json()) as { detail?: unknown };
    return typeof body.detail === "string" && body.detail.trim() ? body.detail : fallback;
  } catch {
    return fallback;
  }
}
