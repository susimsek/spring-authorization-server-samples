"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { Alert, Button, Card, Form, Spinner, Stack } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import Link from "@/routing/Link";
import type { Dictionary } from "@/i18n/get-dictionary";
import { accountActionError, submitAccountAction } from "@/lib/account-actions-api";
import { applyProblemToForm } from "@/lib/problem-detail";
import { ActionIcon } from "@/components/shared/ActionIcon";

import { PasswordField } from "./PasswordField";

type RegistrationFormProps = { dictionary: Dictionary };
type Values = {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
};

export function RegistrationForm({ dictionary }: RegistrationFormProps) {
  const copy = dictionary.registration;
  const [created, setCreated] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const schema = z
    .object({
      username: z.string().trim().min(1, copy.validation.required).max(100, copy.validation.max100),
      firstName: z
        .string()
        .trim()
        .min(1, copy.validation.required)
        .max(100, copy.validation.max100),
      lastName: z.string().trim().min(1, copy.validation.required).max(100, copy.validation.max100),
      email: z
        .string()
        .trim()
        .min(1, copy.validation.required)
        .email(copy.validation.email)
        .max(200, copy.validation.max200),
      password: z.string().min(12, copy.validation.password).max(128, copy.validation.max200),
      confirmPassword: z.string().min(1, copy.validation.required).max(200, copy.validation.max200),
    })
    .refine((values) => values.password === values.confirmPassword, {
      path: ["confirmPassword"],
      message: copy.validation.passwordMismatch,
    });
  const {
    register,
    handleSubmit,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      username: "",
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const submit = handleSubmit(async (values) => {
    setError(null);
    try {
      await submitAccountAction("register", {
        ...values,
        locale: document.documentElement.lang || "en",
      });
      setCreated(true);
    } catch (failure) {
      const result = applyProblemToForm(failure, setFieldError, {
        fields: ["username", "firstName", "lastName", "email", "password", "confirmPassword"],
        fallbackMessage: dictionary.account.validation.invalid,
      });
      if (!result.firstField) setError(accountActionError(failure, dictionary));
    }
  });

  return (
    <Card className="auth-card">
      <Card.Body className="p-4 p-md-5">
        <Stack gap={1} className="mb-4">
          <span className="text-primary text-uppercase fw-semibold small">{copy.eyebrow}</span>
          <h1 className="h3 fw-bold mb-1">{copy.title}</h1>
          <p className="text-body-secondary mb-0">{copy.subtitle}</p>
        </Stack>
        {error && <Alert variant="danger">{error}</Alert>}
        {created ? (
          <>
            <Alert variant="success">{copy.created}</Alert>
            <Link className="d-inline-block mt-2" href={`/login`}>
              {copy.backToLogin}
            </Link>
          </>
        ) : (
          <Form onSubmit={submit} noValidate>
            <Form.Group className="mb-3" controlId="registration-username">
              <Form.Label>{copy.username}</Form.Label>
              <Form.Control isInvalid={Boolean(errors.username)} {...register("username")} />
              <Form.Control.Feedback type="invalid">
                {errors.username?.message}
              </Form.Control.Feedback>
            </Form.Group>
            <div className="row g-3">
              <Form.Group className="col-md-6" controlId="registration-first-name">
                <Form.Label>{copy.firstName}</Form.Label>
                <Form.Control isInvalid={Boolean(errors.firstName)} {...register("firstName")} />
                <Form.Control.Feedback type="invalid">
                  {errors.firstName?.message}
                </Form.Control.Feedback>
              </Form.Group>
              <Form.Group className="col-md-6" controlId="registration-last-name">
                <Form.Label>{copy.lastName}</Form.Label>
                <Form.Control isInvalid={Boolean(errors.lastName)} {...register("lastName")} />
                <Form.Control.Feedback type="invalid">
                  {errors.lastName?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </div>
            <Form.Group className="mt-3 mb-3" controlId="registration-email">
              <Form.Label>{copy.email}</Form.Label>
              <Form.Control
                type="email"
                autoComplete="email"
                isInvalid={Boolean(errors.email)}
                {...register("email")}
              />
              <Form.Control.Feedback type="invalid">{errors.email?.message}</Form.Control.Feedback>
            </Form.Group>
            <PasswordField
              controlId="registration-password"
              label={copy.password}
              placeholder={copy.password}
              showLabel={dictionary.login.showPassword}
              hideLabel={dictionary.login.hidePassword}
              autoComplete="new-password"
              inputProps={{ isInvalid: Boolean(errors.password), ...register("password") }}
            />
            {errors.password && (
              <div className="invalid-feedback d-block">{errors.password.message}</div>
            )}
            <PasswordField
              controlId="registration-confirm-password"
              label={copy.confirmPassword}
              placeholder={copy.confirmPassword}
              showLabel={dictionary.login.showPassword}
              hideLabel={dictionary.login.hidePassword}
              autoComplete="new-password"
              inputProps={{
                isInvalid: Boolean(errors.confirmPassword),
                ...register("confirmPassword"),
              }}
            />
            {errors.confirmPassword && (
              <div className="invalid-feedback d-block">{errors.confirmPassword.message}</div>
            )}
            <Button type="submit" size="lg" className="w-100" disabled={isSubmitting}>
              {isSubmitting ? (
                <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
              ) : (
                <ActionIcon action="add" />
              )}
              {copy.submit}
            </Button>
          </Form>
        )}
        {!created && (
          <Link className="d-inline-block mt-3" href={`/login`}>
            {copy.backToLogin}
          </Link>
        )}
      </Card.Body>
    </Card>
  );
}
