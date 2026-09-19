"use client";

import { useSearchParams } from "@/routing/navigation";
import Link from "@/routing/Link";
import { Suspense, useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "@/lib/form";
import { z } from "zod";
import { Alert, Button, Card, Form, InputGroup, Spinner, Stack } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import type { Locale } from "@/i18n/config";
import { ActionIcon } from "@/components/shared/ActionIcon";
import { Icon, type IconName } from "@/components/shared/Icon";

import { PasswordField } from "./PasswordField";
import { authenticatePasskey, supportsConditionalMediation } from "@/lib/webauthn";

type LoginFormProps = {
  dictionary: Dictionary;
  locale?: Locale;
};

export function LoginForm({ dictionary }: LoginFormProps) {
  const [submitting, setSubmitting] = useState(false);
  const [settings, setSettings] = useState({
    userRegistration: true,
    forgotPassword: true,
    rememberMe: true,
    passkeys: false,
    webauthnMediation: "none" as "none" | "optional" | "conditional",
  });
  const [socialProviders, setSocialProviders] = useState<SocialProvider[]>([]);
  useEffect(() => {
    if (typeof fetch !== "function") return;
    fetch("/api/auth/login-settings")
      .then((response) => (response.ok ? response.json() : null))
      .then((value) => {
        if (value) setSettings((current) => ({ ...current, ...value }));
      })
      .catch(() => {});
    fetch("/api/auth/social-providers")
      .then((response) => (response.ok ? response.json() : []))
      .then((value: unknown) => {
        if (Array.isArray(value)) {
          setSocialProviders(
            value.flatMap((provider) => {
              if (typeof provider === "string") {
                return [{ provider, providerType: provider, configured: true }];
              }
              if (
                provider &&
                typeof provider === "object" &&
                "provider" in provider &&
                typeof provider.provider === "string" &&
                "configured" in provider &&
                typeof provider.configured === "boolean"
              ) {
                return [
                  {
                    provider: provider.provider,
                    providerType:
                      "providerType" in provider && typeof provider.providerType === "string"
                        ? provider.providerType
                        : provider.provider,
                    configured: provider.configured,
                  },
                ];
              }
              return [];
            }),
          );
        }
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
    void handleSubmit(() => {
      setSubmitting(true);
      form.submit();
    })(event);
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

        <Form method="post" action="/login" onSubmit={submit} noValidate>
          <Form.Group className="mb-3" controlId="username">
            <Form.Label>{dictionary.login.username}</Form.Label>
            <InputGroup hasValidation={Boolean(errors.username)}>
              <InputGroup.Text>
                <Icon icon="user" />
              </InputGroup.Text>
              <Form.Control
                type="text"
                autoComplete="username webauthn"
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
            error={errors.password?.message}
            inputProps={{ isInvalid: Boolean(errors.password), ...register("password") }}
          />

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

          <Button type="submit" size="lg" className="w-100" disabled={submitting}>
            {submitting ? (
              <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
            ) : (
              <ActionIcon action="login" />
            )}
            {dictionary.login.submit}
          </Button>
        </Form>
        {settings.passkeys && (
          <Suspense fallback={null}>
            <PasskeyLoginButton dictionary={dictionary} mediation={settings.webauthnMediation} />
          </Suspense>
        )}
        {socialProviders.length > 0 && (
          <SocialLoginButtons providers={socialProviders} dictionary={dictionary} />
        )}
        {settings.userRegistration && (
          <Link className="d-block text-center mt-3" href={`/register`}>
            {dictionary.login.register}
          </Link>
        )}
      </Card.Body>
    </Card>
  );
}

const SOCIAL_PROVIDER_LABELS: Record<string, string> = {
  google: "Google",
  github: "GitHub",
  linkedin: "LinkedIn",
  microsoft: "Microsoft",
};

const SOCIAL_PROVIDER_ICONS: Record<string, IconName> = {
  google: "google",
  github: "github",
  linkedin: "linkedin",
  microsoft: "microsoft",
};

type SocialProvider = {
  provider: string;
  providerType: string;
  configured: boolean;
};

function SocialLoginButtons({
  providers,
  dictionary,
}: {
  providers: SocialProvider[];
  dictionary: LoginFormProps["dictionary"];
}) {
  const [submittingProvider, setSubmittingProvider] = useState<string | null>(null);
  const supportedProviders = providers.filter((provider) => provider.provider.length > 0);

  if (supportedProviders.length === 0) return null;

  return (
    <div className="mt-4">
      <div className="d-flex align-items-center gap-2 text-body-secondary small mb-2">
        <hr className="flex-grow-1 my-0" />
        <span>{dictionary.login.socialDivider}</span>
        <hr className="flex-grow-1 my-0" />
      </div>
      <div className="d-flex justify-content-center gap-2">
        {supportedProviders.map((provider) => {
          const label = SOCIAL_PROVIDER_LABELS[provider.providerType] ?? provider.provider;
          const icon: IconName = SOCIAL_PROVIDER_ICONS[provider.providerType] ?? "globe";
          return (
            <Button
              key={provider.provider}
              as="a"
              href={provider.configured ? `/oauth2/authorization/${provider.provider}` : undefined}
              role="button"
              variant="secondary"
              size="lg"
              className="social-login-button p-0"
              title={`${dictionary.login.socialLogin} ${label}`}
              aria-label={`${dictionary.login.socialLogin} ${label}`}
              disabled={!provider.configured || submittingProvider !== null}
              aria-disabled={!provider.configured || submittingProvider !== null}
              onClick={(event) => {
                if (!provider.configured || submittingProvider !== null) {
                  event.preventDefault();
                  return;
                }
                setSubmittingProvider(provider.provider);
              }}
            >
              {submittingProvider === provider.provider && (
                <Spinner animation="border" aria-hidden="true" size="sm" />
              )}
              {submittingProvider !== provider.provider && <Icon icon={icon} size="lg" />}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function PasskeyLoginButton({
  dictionary,
  mediation,
}: LoginFormProps & { mediation: "none" | "optional" | "conditional" }) {
  const searchParams = useSearchParams();
  const [submitting, setSubmitting] = useState(false);
  const [automaticPending, setAutomaticPending] = useState(false);
  const [error, setError] = useState(false);
  const requestController = useRef<AbortController | null>(null);
  const conditionalAttempted = useRef(false);
  const returnTo = searchParams.get("return_to") || "/";

  const signIn = useCallback(
    async (automatic = false) => {
      requestController.current?.abort();
      const controller = new AbortController();
      requestController.current = controller;
      setAutomaticPending(automatic);
      setSubmitting(true);
      setError(false);
      try {
        const response = await authenticatePasskey(
          async (url, init) => {
            const value = await fetch(url, {
              ...init,
              credentials: "same-origin",
              signal: controller.signal,
            });
            let data: unknown = null;
            try {
              data = await value.json();
            } catch {
              // A successful WebAuthn response has no body.
            }
            return {
              status: value.status,
              data,
              url: value.url,
              redirectUrl: value.headers.get("Location"),
            };
          },
          {
            mediation:
              automatic && mediation !== "none" ? mediation : automatic ? undefined : "optional",
            signal: controller.signal,
          },
        );
        if (response.status >= 300) throw new Error();
        const redirectedUrl = response.redirectUrl
          ? new URL(response.redirectUrl, window.location.origin)
          : response.url
            ? new URL(response.url, window.location.origin)
            : null;
        const redirectedTarget = redirectedUrl
          ? redirectedUrl.pathname + redirectedUrl.search
          : "/";
        const target = returnTo.startsWith("/") && returnTo !== "/" ? returnTo : redirectedTarget;
        window.location.assign(target.startsWith("/") ? target : "/");
      } catch {
        if (!automatic) setError(true);
      } finally {
        if (automatic) setAutomaticPending(false);
        if (requestController.current === controller) {
          requestController.current = null;
          setSubmitting(false);
        }
      }
    },
    [mediation, returnTo],
  );

  useEffect(() => {
    if (mediation !== "conditional" || conditionalAttempted.current) return;
    conditionalAttempted.current = true;
    void supportsConditionalMediation()
      .then((available) => {
        if (available) void signIn(true);
      })
      .catch(() => {});
  }, [mediation, signIn]);

  return (
    <div className="mt-3">
      <div className="d-flex align-items-center gap-2 text-body-secondary small mb-2">
        <hr className="flex-grow-1 my-0" />
        <span>{dictionary.login.passkeyDivider}</span>
        <hr className="flex-grow-1 my-0" />
      </div>
      {error && <Alert variant="danger">{dictionary.login.passkeyError}</Alert>}
      <Button
        type="button"
        variant="primary"
        size="lg"
        className="w-100"
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => void signIn()}
        disabled={submitting && !automaticPending}
      >
        {submitting ? (
          <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
        ) : (
          <Icon icon="key" />
        )}
        {dictionary.login.passkey}
      </Button>
    </div>
  );
}

function LoginStatusAlerts({ dictionary }: LoginFormProps) {
  const searchParams = useSearchParams();
  const accountLinkRequired = searchParams.has("account_link_required");
  const loginError = searchParams.has("error") && !accountLinkRequired;
  const loggedOut = searchParams.has("logout");

  return (
    <>
      {accountLinkRequired && (
        <Alert variant="warning">{dictionary.login.accountLinkRequired}</Alert>
      )}
      {loginError && <Alert variant="danger">{dictionary.login.invalidCredentials}</Alert>}
      {loggedOut && <Alert variant="success">{dictionary.login.loggedOut}</Alert>}
      {searchParams.has("deleted") && (
        <Alert variant="success">{dictionary.login.accountDeleted}</Alert>
      )}
    </>
  );
}
