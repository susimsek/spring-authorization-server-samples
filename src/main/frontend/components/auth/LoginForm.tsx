"use client";

import { faUser } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useSearchParams } from "@/routing/navigation";
import Link from "@/routing/Link";
import { Suspense, useEffect, useState, type FormEvent } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Alert, Button, Card, Form, InputGroup, Stack } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import type { Locale } from "@/i18n/config";
import { ActionIcon } from "@/components/shared/ActionIcon";

import { PasswordField } from "./PasswordField";

type LoginFormProps = {
  dictionary: Dictionary;
  locale?: Locale;
};

export function LoginForm({ dictionary }: LoginFormProps) {
  const [settings, setSettings] = useState({
    userRegistration: true,
    forgotPassword: true,
    rememberMe: true,
  });
  useEffect(() => {
    if (typeof fetch !== "function") return;
    fetch("/api/auth/login-settings")
      .then((response) => (response.ok ? response.json() : null))
      .then((value) => {
        if (value) setSettings(value);
      })
      .catch(() => {});
  }, []);
  const schema = z.object({
    username: z.string().trim().min(1, dictionary.admin.common.validation.required),
    password: z.string().min(1, dictionary.admin.common.validation.required),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ username: string; password: string }>({
    resolver: zodResolver(schema),
    mode: "onBlur",
  });

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    void handleSubmit(() => form.submit())(event);
  };

  return (
    <Card className="auth-card">
      <Card.Body className="p-4 p-md-5">
        <Stack gap={1} className="mb-4">
          <span className="text-primary text-uppercase fw-semibold small">
            {dictionary.login.eyebrow}
          </span>
          <h1 className="h3 fw-bold mb-1">{dictionary.login.title}</h1>
          <p className="text-body-secondary mb-0">{dictionary.login.subtitle}</p>
        </Stack>

        <Suspense fallback={null}>
          <LoginStatusAlerts dictionary={dictionary} />
        </Suspense>

        <Form method="post" action="/login" onSubmit={submit}>
          <Form.Group className="mb-3" controlId="username">
            <Form.Label>{dictionary.login.username}</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <FontAwesomeIcon icon={faUser} />
              </InputGroup.Text>
              <Form.Control
                type="text"
                autoComplete="username"
                placeholder={dictionary.login.usernamePlaceholder}
                autoFocus
                isInvalid={Boolean(errors.username)}
                {...register("username")}
              />
            </InputGroup>
            <Form.Control.Feedback type="invalid" className="d-block">
              {errors.username?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <PasswordField
            label={dictionary.login.password}
            placeholder={dictionary.login.passwordPlaceholder}
            showLabel={dictionary.login.showPassword}
            hideLabel={dictionary.login.hidePassword}
            inputProps={{ isInvalid: Boolean(errors.password), ...register("password") }}
          />
          {errors.password && (
            <div className="invalid-feedback d-block">{errors.password.message}</div>
          )}

          {settings.rememberMe && (
            <Form.Check
              className="mb-3"
              id="remember-me"
              label={dictionary.login.rememberMe}
              name="remember-me"
              type="checkbox"
            />
          )}

          {settings.forgotPassword && (
            <div className="text-end mb-3">
              <Link href={`/forgot-password`}>{dictionary.login.forgotPassword}</Link>
            </div>
          )}

          <Button type="submit" size="lg" className="w-100">
            <ActionIcon action="login" />
            {dictionary.login.submit}
          </Button>
        </Form>
        {settings.userRegistration && (
          <Link className="d-block text-center mt-3" href={`/register`}>
            {dictionary.login.register}
          </Link>
        )}
      </Card.Body>
    </Card>
  );
}

function LoginStatusAlerts({ dictionary }: LoginFormProps) {
  const searchParams = useSearchParams();
  const loginError = searchParams.has("error");
  const loggedOut = searchParams.has("logout");

  return (
    <>
      {loginError && <Alert variant="danger">{dictionary.login.invalidCredentials}</Alert>}
      {loggedOut && <Alert variant="success">{dictionary.login.loggedOut}</Alert>}
      {searchParams.has("deleted") && (
        <Alert variant="success">{dictionary.login.accountDeleted}</Alert>
      )}
    </>
  );
}
