"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { Alert, Button, Card, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import type { Dictionary } from "@/i18n/get-dictionary";
import { ActionIcon } from "@/components/shared/ActionIcon";
import { problemViolations } from "@/lib/problem-detail";
import { type AccountApiError, useDeleteAccountMutation } from "@/store/account-api-slice";

import { useAccountAuth } from "./AccountAuthProvider";

export function AccountDeleteForm({ dictionary }: { dictionary: Dictionary }) {
  const copy = dictionary.account;
  const { accessToken, clearLocalSession } = useAccountAuth();
  const [deleteAccount] = useDeleteAccountMutation();
  const [failed, setFailed] = useState(false);
  const schema = z.object({
    currentPassword: z.string().min(1, copy.validation.required).max(200, copy.validation.max200),
  });
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<{ currentPassword: string }>({ resolver: zodResolver(schema) });

  const submit = handleSubmit(async ({ currentPassword }) => {
    if (!accessToken) return;
    setFailed(false);
    try {
      await deleteAccount({ accessToken, currentPassword }).unwrap();
      clearLocalSession();
      window.location.replace(`/login?deleted`);
    } catch (error) {
      setFailed(true);
      if (
        problemViolations((error as AccountApiError).data).some(
          ({ field }) => field === "currentPassword",
        )
      ) {
        setError("currentPassword", { message: copy.validation.currentPassword });
      }
    }
  });

  return (
    <Card className="admin-panel-card account-panel-card border-danger">
      <Card.Header className="bg-body p-4 border-bottom border-danger-subtle">
        <h2 className="h5 mb-1 text-danger">{copy.deleteAccount.title}</h2>
        <div className="small text-body-secondary">{copy.deleteAccount.subtitle}</div>
      </Card.Header>
      <Card.Body className="p-4">
        {failed && <Alert variant="danger">{copy.deleteAccount.error}</Alert>}
        <Form onSubmit={submit} noValidate>
          <p className="text-body-secondary">{copy.deleteAccount.warning}</p>
          <Form.Group className="mb-4" controlId="delete-account-password">
            <Form.Label>{copy.deleteAccount.currentPassword}</Form.Label>
            <Form.Control
              type="password"
              autoComplete="current-password"
              isInvalid={Boolean(errors.currentPassword)}
              {...register("currentPassword")}
            />
            <Form.Control.Feedback type="invalid">
              {errors.currentPassword?.message}
            </Form.Control.Feedback>
          </Form.Group>
          <div className="account-form-actions">
            <Button type="submit" variant="danger" disabled={isSubmitting}>
              <ActionIcon action="delete" />
              {isSubmitting ? copy.common.saving : copy.deleteAccount.submit}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={isSubmitting}
              onClick={() => reset()}
            >
              <ActionIcon action="cancel" />
              {copy.common.cancel}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
