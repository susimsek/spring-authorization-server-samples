"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Card, Form, Stack } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useSearchParams } from "@/routing/navigation";
import type { Dictionary } from "@/i18n/get-dictionary";
import { ActionIcon } from "@/components/shared/ActionIcon";

type Action = { key: string; displayName: string; description: string; version: number };

export function RequiredActionsPage({ dictionary }: { dictionary: Dictionary }) {
  const copy = dictionary.requiredActions;
  const returnTo = safeReturnTo(useSearchParams().get("return_to"));
  const [actions, setActions] = useState<Action[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);
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
      newPassword: z
        .string()
        .min(12, copy.password)
        .max(128, copy.password)
        .regex(/[A-Z]/, copy.password)
        .regex(/[a-z]/, copy.password)
        .regex(/[0-9]/, copy.password)
        .regex(/[^A-Za-z0-9\s]/, copy.password),
      confirmPassword: z.string().min(1, copy.required),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      path: ["confirmPassword"],
      message: copy.passwordMismatch,
    });
  const passwordForm = useForm<z.infer<typeof passwordSchema>>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
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
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  const complete = async (values: Record<string, unknown>) => {
    const action = actions[0];
    if (!action || busy) return;
    setBusy(true);
    setError(false);
    try {
      const response = await fetch(`/api/required-actions/${encodeURIComponent(action.key)}`, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ values }),
      });
      if (!response.ok) throw new Error();
      const next = await fetch("/api/required-actions", { credentials: "same-origin" });
      const pending = next.ok ? ((await next.json()) as Action[]) : [];
      if (pending.length === 0) window.location.assign(returnTo);
      else setActions(pending);
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };

  if (loading)
    return (
      <Card className="auth-card">
        <Card.Body>{copy.loading}</Card.Body>
      </Card>
    );
  if (error)
    return (
      <Card className="auth-card">
        <Card.Body>
          <Alert variant="danger">{copy.error}</Alert>
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
                <Form.Group>
                  <Form.Label>{copy.password}</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    isInvalid={Boolean(passwordForm.formState.errors.newPassword)}
                    {...passwordForm.register("newPassword")}
                  />
                  <Form.Text>{copy.passwordHelp}</Form.Text>
                  <Form.Control.Feedback type="invalid">
                    {passwordForm.formState.errors.newPassword?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group>
                  <Form.Label>{copy.confirmPassword}</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    isInvalid={Boolean(passwordForm.formState.errors.confirmPassword)}
                    {...passwordForm.register("confirmPassword")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {passwordForm.formState.errors.confirmPassword?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Button type="submit" disabled={busy || passwordForm.formState.isSubmitting}>
                  <ActionIcon action="next" />
                  {busy ? copy.saving : copy.continue}
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
