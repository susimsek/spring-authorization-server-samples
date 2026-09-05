"use client";

import { useEffect, useRef, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Card, Form, Stack } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useSearchParams } from "@/routing/navigation";
import type { Dictionary } from "@/i18n/get-dictionary";
import { ActionIcon } from "@/components/shared/ActionIcon";

type Action = { key: string; displayName: string; description: string; version: number };
type TotpSetup = {
  secret: string;
  otpauthUri: string;
  algorithm: string;
  digits: number;
  periodSeconds: number;
};

export function RequiredActionsPage({ dictionary }: { dictionary: Dictionary }) {
  const copy = dictionary.requiredActions;
  const returnTo = safeReturnTo(useSearchParams().get("return_to"));
  const [actions, setActions] = useState<Action[]>([]);
  const [loading, setLoading] = useState(true);
  const [fatalError, setFatalError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [totpSetup, setTotpSetup] = useState<TotpSetup | null>(null);
  const totpSetupRequested = useRef(false);
  const profileSchema = z.object({
    firstName: z
      .string()
      .trim()
      .min(1, copy.required)
      .max(100, dictionary.account.validation.max100),
    lastName: z
      .string()
      .trim()
      .min(1, copy.required)
      .max(100, dictionary.account.validation.max100),
    email: z
      .string()
      .trim()
      .min(1, copy.required)
      .email(copy.email)
      .max(200, dictionary.account.validation.max200),
  });
  const profileForm = useForm<z.infer<typeof profileSchema>>({
    resolver: zodResolver(profileSchema),
  });
  const passwordSchema = z
    .object({
      newPassword: z.string().max(128, copy.passwordTooLong),
      confirmPassword: z.string().min(1, copy.required),
    })
    .superRefine(({ newPassword }, context) => {
      const missing = [
        newPassword.length < 12 ? copy.passwordMinLength : null,
        /[A-Z]/.test(newPassword) ? null : copy.passwordUppercase,
        /[a-z]/.test(newPassword) ? null : copy.passwordLowercase,
        /[0-9]/.test(newPassword) ? null : copy.passwordDigit,
        /[^A-Za-z0-9\s]/.test(newPassword) ? null : copy.passwordSymbol,
      ].filter((value): value is string => value !== null);
      if (missing.length > 0) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["newPassword"],
          message: `${copy.passwordMissingPrefix}${missing.join(", ")}.`,
        });
      }
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      path: ["confirmPassword"],
      message: copy.passwordMismatch,
    });
  const passwordForm = useForm<z.infer<typeof passwordSchema>>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  });
  const totpForm = useForm<{ code: string }>({
    resolver: zodResolver(z.object({ code: z.string().regex(/^\d{6,8}$/, copy.invalidCode) })),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = profileForm;

  useEffect(() => {
    fetch("/api/required-actions", { credentials: "same-origin" })
      .then((response) => {
        if (!response.ok) throw new Error();
        return response.json() as Promise<Action[]>;
      })
      .then(setActions)
      .catch(() => setFatalError(copy.error))
      .finally(() => setLoading(false));
  }, [copy.error]);

  useEffect(() => {
    if (actions[0]?.key !== "CONFIGURE_TOTP") return;
    if (totpSetupRequested.current) return;
    totpSetupRequested.current = true;
    fetch("/api/required-actions/CONFIGURE_TOTP/setup", { credentials: "same-origin" })
      .then(async (response) => {
        if (!response.ok) throw new Error(await responseError(response, copy.error));
        return (await response.json()) as TotpSetup;
      })
      .then(setTotpSetup)
      .catch((cause) => {
        setTotpSetup(null);
        setFatalError(cause instanceof Error ? cause.message : copy.error);
      });
  }, [actions, copy.error]);

  const complete = async (values: Record<string, unknown>) => {
    const action = actions[0];
    if (!action || busy) return;
    setBusy(true);
    setSubmitError(null);
    if (action.key === "CONFIGURE_TOTP") totpForm.clearErrors("code");
    try {
      const response = await fetch(`/api/required-actions/${encodeURIComponent(action.key)}`, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ values }),
      });
      if (response.status === 401) {
        window.location.assign(returnTo);
        return;
      }
      if (!response.ok) {
        const message = await responseError(response, copy.error);
        if (action.key === "CONFIGURE_TOTP") {
          totpForm.setError("code", { type: "server", message });
          return;
        }
        throw new Error(message);
      }
      // Resume the original authorization request. The authorization filter redirects
      // back here when another required action remains.
      window.location.assign(returnTo);
    } catch (cause) {
      setSubmitError(cause instanceof Error ? cause.message : copy.error);
    } finally {
      setBusy(false);
    }
  };

  const completeTotp = (values: { code: string }) => {
    const expectedDigits = totpSetup?.digits ?? 6;
    if (!new RegExp(`^\\d{${expectedDigits}}$`).test(values.code)) {
      totpForm.setError("code", { type: "validate", message: copy.invalidCode });
      return;
    }
    return complete(values);
  };

  if (loading)
    return (
      <Card className="auth-card">
        <Card.Body>{copy.loading}</Card.Body>
      </Card>
    );
  if (fatalError)
    return (
      <Card className="auth-card">
        <Card.Body>
          <Alert variant="danger">{fatalError}</Alert>
        </Card.Body>
      </Card>
    );
  const action = actions[0];
  if (!action) {
    window.location.assign(returnTo);
    return null;
  }

  return (
    <Card className="auth-card">
      <Card.Body className="p-4 p-md-5">
        <Stack gap={2}>
          <span className="text-primary text-uppercase fw-semibold small">{copy.eyebrow}</span>
          <h1 className="h3 fw-bold mb-0">{action.displayName}</h1>
          <p className="text-body-secondary">{action.description}</p>
          {submitError && <Alert variant="danger">{submitError}</Alert>}
          {action.key === "UPDATE_PROFILE" ? (
            <Form onSubmit={handleSubmit((values) => complete(values))} noValidate>
              <Stack gap={3}>
                <Form.Group>
                  <Form.Label>{copy.firstName}</Form.Label>
                  <Form.Control
                    maxLength={100}
                    isInvalid={Boolean(errors.firstName)}
                    {...register("firstName")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.firstName?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group>
                  <Form.Label>{copy.lastName}</Form.Label>
                  <Form.Control
                    maxLength={100}
                    isInvalid={Boolean(errors.lastName)}
                    {...register("lastName")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.lastName?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group>
                  <Form.Label>{copy.emailLabel}</Form.Label>
                  <Form.Control
                    maxLength={200}
                    type="email"
                    isInvalid={Boolean(errors.email)}
                    {...register("email")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.email?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Button type="submit" disabled={busy}>
                  <ActionIcon action="next" />
                  {busy ? copy.saving : copy.continue}
                </Button>
              </Stack>
            </Form>
          ) : action.key === "UPDATE_PASSWORD" ? (
            <Form onSubmit={passwordForm.handleSubmit((values) => complete(values))} noValidate>
              <Stack gap={3}>
                <Form.Group controlId="required-action-new-password">
                  <Form.Label>{copy.password}</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    required
                    isInvalid={Boolean(passwordForm.formState.errors.newPassword)}
                    aria-describedby="required-action-password-help required-action-new-password-error"
                    {...passwordForm.register("newPassword")}
                  />
                  {!passwordForm.formState.errors.newPassword && (
                    <Form.Text id="required-action-password-help">{copy.passwordHelp}</Form.Text>
                  )}
                  {passwordForm.formState.errors.newPassword && (
                    <Form.Control.Feedback
                      id="required-action-new-password-error"
                      className="d-block"
                      type="invalid"
                    >
                      {passwordForm.formState.errors.newPassword.message}
                    </Form.Control.Feedback>
                  )}
                </Form.Group>
                <Form.Group controlId="required-action-confirm-password">
                  <Form.Label>{copy.confirmPassword}</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    required
                    isInvalid={Boolean(passwordForm.formState.errors.confirmPassword)}
                    aria-describedby="required-action-confirm-password-error"
                    {...passwordForm.register("confirmPassword")}
                  />
                  {passwordForm.formState.errors.confirmPassword && (
                    <Form.Control.Feedback
                      id="required-action-confirm-password-error"
                      className="d-block"
                      type="invalid"
                    >
                      {passwordForm.formState.errors.confirmPassword.message}
                    </Form.Control.Feedback>
                  )}
                </Form.Group>
                <Button type="submit" disabled={busy || passwordForm.formState.isSubmitting}>
                  <ActionIcon action="next" />
                  {busy ? copy.saving : copy.continue}
                </Button>
              </Stack>
            </Form>
          ) : action.key === "CONFIGURE_TOTP" ? (
            <Form onSubmit={totpForm.handleSubmit(completeTotp)} noValidate>
              <Stack gap={3}>
                {totpSetup && (
                  <div className="small">
                    <div className="fw-semibold mb-1">{copy.totpSecret}</div>
                    <code className="d-block text-break">{totpSetup.secret}</code>
                    <div className="text-body-secondary mt-2">{copy.totpUriHelp}</div>
                    <code className="d-block text-break">{totpSetup.otpauthUri}</code>
                  </div>
                )}
                <Form.Group controlId="required-action-totp-code">
                  <Form.Label>{copy.totpCode}</Form.Label>
                  <Form.Control
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={totpSetup?.digits ?? 8}
                    isInvalid={Boolean(totpForm.formState.errors.code)}
                    {...totpForm.register("code")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {totpForm.formState.errors.code?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Button type="submit" disabled={busy || !totpSetup}>
                  <ActionIcon action="check" /> {busy ? copy.saving : copy.continue}
                </Button>
              </Stack>
            </Form>
          ) : action.key === "UPDATE_EMAIL" ? (
            <Alert variant="info">{copy.emailPending}</Alert>
          ) : (
            <Button
              disabled={busy}
              onClick={() => void complete({ accepted: true, version: action.version })}
            >
              <ActionIcon action="check" />
              {busy ? copy.saving : copy.accept}
            </Button>
          )}
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
