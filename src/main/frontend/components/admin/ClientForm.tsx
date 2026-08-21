"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Form, Row } from "react-bootstrap";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { ErrorState, LoadingState } from "./AsyncState";
import { ResultModal } from "./ResultModal";
import type { AdminClient } from "./ClientsTable";

type Detail = AdminClient & {
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  authorizationCodeTimeToLive: string;
  accessTokenTimeToLive: string;
  refreshTokenTimeToLive: string;
};

type FormState = {
  clientId: string;
  clientName: string;
  clientAuthenticationMethods: string[];
  authorizationGrantTypes: string[];
  redirectUris: string;
  postLogoutRedirectUris: string;
  scopes: string;
  requireAuthorizationConsent: boolean;
  requireProofKey: boolean;
  authorizationCodeTimeToLive: string;
  accessTokenTimeToLive: string;
  refreshTokenTimeToLive: string;
};

const EMPTY: FormState = {
  clientId: "",
  clientName: "",
  clientAuthenticationMethods: ["client_secret_basic"],
  authorizationGrantTypes: ["authorization_code", "refresh_token"],
  redirectUris: "",
  postLogoutRedirectUris: "",
  scopes: "openid profile",
  requireAuthorizationConsent: true,
  requireProofKey: true,
  authorizationCodeTimeToLive: "PT5M",
  accessTokenTimeToLive: "PT5M",
  refreshTokenTimeToLive: "PT1H",
};

const METHODS = ["client_secret_basic", "client_secret_post", "none"];
const GRANTS = ["authorization_code", "refresh_token", "client_credentials"];
const clientSchema = (validation: Dictionary["admin"]["common"]["validation"]) =>
  z
    .object({
      clientId: z.string().trim().min(1, validation.required),
      clientName: z.string().trim().min(1, validation.required),
      scopes: z.string().trim().min(1, validation.scope),
      clientAuthenticationMethods: z.array(z.string()).min(1, validation.selection),
      authorizationGrantTypes: z.array(z.string()).min(1, validation.selection),
      redirectUris: z
        .string()
        .refine((value) => lines(value).every(isValidAbsoluteUri), validation.uri),
      postLogoutRedirectUris: z
        .string()
        .refine((value) => lines(value).every(isValidAbsoluteUri), validation.uri),
      requireAuthorizationConsent: z.boolean(),
      requireProofKey: z.boolean(),
      authorizationCodeTimeToLive: z.string(),
      accessTokenTimeToLive: z.string(),
      refreshTokenTimeToLive: z.string(),
    })
    .superRefine((value, context) => {
      const authorizationCode = value.authorizationGrantTypes.includes("authorization_code");
      const publicClient = value.clientAuthenticationMethods.includes("none");
      if (authorizationCode && lines(value.redirectUris).length === 0)
        context.addIssue({ code: "custom", path: ["redirectUris"], message: validation.required });
      if (publicClient && value.clientAuthenticationMethods.length > 1)
        context.addIssue({
          code: "custom",
          path: ["clientAuthenticationMethods"],
          message: validation.selection,
        });
      if (publicClient && value.authorizationGrantTypes.includes("client_credentials"))
        context.addIssue({
          code: "custom",
          path: ["authorizationGrantTypes"],
          message: validation.selection,
        });
      if (publicClient && authorizationCode && !value.requireProofKey)
        context.addIssue({
          code: "custom",
          path: ["authorizationGrantTypes"],
          message: validation.selection,
        });
      if (value.requireProofKey && !authorizationCode)
        context.addIssue({
          code: "custom",
          path: ["authorizationGrantTypes"],
          message: validation.selection,
        });
    });

function lines(value: string) {
  return value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function isValidAbsoluteUri(value: string) {
  if (!URL.canParse(value)) return false;
  const uri = new URL(value);
  return Boolean(uri.protocol) && uri.hash === "";
}

function words(value: string) {
  return value
    .split(/\s+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function ClientForm({
  locale,
  dictionary,
  mode,
  id,
}: {
  locale: Locale;
  dictionary: Dictionary;
  mode: "create" | "edit";
  id?: string | null;
}) {
  const router = useRouter();
  const { accessToken } = useAdminAuth();
  const missingId = mode === "edit" && !id;
  const [loading, setLoading] = useState(mode === "edit" && Boolean(id));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [createdSecret, setCreatedSecret] = useState<string | null>(null);
  const [createdClientId, setCreatedClientId] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
    reset,
    setValue,
    setError: setFieldError,
    control,
  } = useForm<FormState>({
    resolver: zodResolver(clientSchema(dictionary.admin.common.validation)),
    mode: "onBlur",
    defaultValues: EMPTY,
  });
  const clientAuthenticationMethods = useWatch({
    control,
    name: "clientAuthenticationMethods",
    defaultValue: EMPTY.clientAuthenticationMethods,
  });
  const authorizationGrantTypes = useWatch({
    control,
    name: "authorizationGrantTypes",
    defaultValue: EMPTY.authorizationGrantTypes,
  });
  const requireProofKey = useWatch({
    control,
    name: "requireProofKey",
    defaultValue: EMPTY.requireProofKey,
  });
  const requireAuthorizationConsent = useWatch({
    control,
    name: "requireAuthorizationConsent",
    defaultValue: EMPTY.requireAuthorizationConsent,
  });

  useEffect(() => {
    if (mode !== "edit") {
      return;
    }
    if (!id || !accessToken) {
      return;
    }

    adminRequest<Detail>(accessToken, {
      url: `/api/admin/clients/${encodeURIComponent(id)}`,
    })
      .then((response) => {
        if (response.status >= 300) {
          throw new Error();
        }
        return response.data;
      })
      .then((client) =>
        reset({
          clientId: client.clientId,
          clientName: client.clientName,
          clientAuthenticationMethods: client.clientAuthenticationMethods,
          authorizationGrantTypes: client.authorizationGrantTypes,
          redirectUris: client.redirectUris.join("\n"),
          postLogoutRedirectUris: client.postLogoutRedirectUris.join("\n"),
          scopes: client.scopes.join(" "),
          requireAuthorizationConsent: client.requireAuthorizationConsent,
          requireProofKey: client.requireProofKey,
          authorizationCodeTimeToLive: client.authorizationCodeTimeToLive ?? "PT5M",
          accessTokenTimeToLive: client.accessTokenTimeToLive ?? "PT5M",
          refreshTokenTimeToLive: client.refreshTokenTimeToLive ?? "PT1H",
        }),
      )
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, id, mode, reset]);

  const toggle = (
    field: "clientAuthenticationMethods" | "authorizationGrantTypes",
    value: string,
  ) => {
    const selected = getValues(field);
    setValue(
      field,
      selected.includes(value) ? selected.filter((item) => item !== value) : [...selected, value],
      { shouldDirty: true, shouldValidate: true },
    );
  };

  const submit = async (values: FormState) => {
    setSaving(true);
    setError(false);
    setErrorMessage(null);

    if (!accessToken) {
      setSaving(false);
      return;
    }

    const payload = {
      ...values,
      redirectUris: lines(values.redirectUris),
      postLogoutRedirectUris: lines(values.postLogoutRedirectUris),
      scopes: words(values.scopes),
    };

    const url =
      mode === "create"
        ? "/api/admin/clients"
        : `/api/admin/clients/${encodeURIComponent(id ?? "")}`;

    try {
      const response = await adminRequest<Detail | { client: Detail; clientSecret: string | null }>(
        accessToken,
        {
          url,
          method: mode === "create" ? "POST" : "PUT",
          headers: { "Content-Type": "application/json" },
          data: payload,
        },
      );
      if (response.status >= 300) {
        const errorCode = problemErrorCode(response.data);
        problemViolations(response.data).forEach(({ field }) =>
          setFieldError(field as keyof FormState, {
            message:
              errorCode === "admin_client_duplicate_client_id"
                ? dictionary.admin.common.validation.clientIdDuplicate
                : field === "scopes"
                  ? dictionary.admin.common.validation.scope
                  : field === "redirectUris"
                    ? dictionary.admin.common.validation.uri
                    : field === "clientAuthenticationMethods" || field === "authorizationGrantTypes"
                      ? dictionary.admin.common.validation.selection
                      : dictionary.admin.common.validation.required,
          }),
        );
        throw new Error(
          typeof response.data === "object" &&
            response.data !== null &&
            "detail" in response.data &&
            typeof response.data.detail === "string"
            ? response.data.detail
            : dictionary.admin.clients.saveError,
        );
      }
      if (mode === "create") {
        const created = response.data as {
          client: Detail;
          clientSecret: string | null;
        };
        if (created.clientSecret) {
          setCreatedClientId(created.client.id);
          setCreatedSecret(created.clientSecret);
          return;
        }
        router.push(`/${locale}/admin/clients/detail?id=${encodeURIComponent(created.client.id)}`);
      } else {
        const saved = response.data as Detail;
        router.push(`/${locale}/admin/clients/detail?id=${encodeURIComponent(saved.id)}`);
      }
      router.refresh();
    } catch (exception: unknown) {
      setError(true);
      setErrorMessage(
        exception instanceof Error ? exception.message : dictionary.admin.clients.saveError,
      );
    } finally {
      setSaving(false);
    }
  };

  if (missingId) {
    return <ErrorState message={dictionary.admin.clients.notFound} />;
  }

  if (loading) return <LoadingState />;

  return (
    <Form onSubmit={handleSubmit(submit)}>
      {error && (
        <Alert variant="danger">{errorMessage ?? dictionary.admin.clients.saveError}</Alert>
      )}
      <Card className="border-0 shadow-sm mb-3">
        <Card.Body>
          <h2 className="h6 mb-3">{dictionary.admin.clients.general}</h2>
          <Row className="g-3">
            <Col md={6}>
              <Form.Label>{dictionary.admin.clients.clientId}</Form.Label>
              <Form.Control isInvalid={Boolean(errors.clientId)} {...register("clientId")} />
              <Form.Control.Feedback type="invalid">
                {errors.clientId?.message}
              </Form.Control.Feedback>
            </Col>
            <Col md={6}>
              <Form.Label>{dictionary.admin.clients.clientName}</Form.Label>
              <Form.Control isInvalid={Boolean(errors.clientName)} {...register("clientName")} />
              <Form.Control.Feedback type="invalid">
                {errors.clientName?.message}
              </Form.Control.Feedback>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Card className="border-0 shadow-sm mb-3">
        <Card.Body>
          <h2 className="h6 mb-3">{dictionary.admin.clients.capabilities}</h2>
          <Row className="g-4">
            <Col lg={6}>
              <Form.Label>{dictionary.admin.clients.authMethods}</Form.Label>
              {METHODS.map((method) => (
                <Form.Check
                  key={method}
                  type="checkbox"
                  label={method}
                  checked={clientAuthenticationMethods.includes(method)}
                  onChange={() => toggle("clientAuthenticationMethods", method)}
                />
              ))}
              {errors.clientAuthenticationMethods && (
                <div className="invalid-feedback d-block">
                  {errors.clientAuthenticationMethods.message}
                </div>
              )}
            </Col>
            <Col lg={6}>
              <Form.Label>{dictionary.admin.clients.grants}</Form.Label>
              {GRANTS.map((grant) => (
                <Form.Check
                  key={grant}
                  type="checkbox"
                  label={grant}
                  checked={authorizationGrantTypes.includes(grant)}
                  onChange={() => toggle("authorizationGrantTypes", grant)}
                />
              ))}
              {errors.authorizationGrantTypes && (
                <div className="invalid-feedback d-block">
                  {errors.authorizationGrantTypes.message}
                </div>
              )}
            </Col>
            <Col lg={6}>
              <Form.Label>{dictionary.admin.clients.redirectUris}</Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                placeholder="https://app.example/callback"
                isInvalid={Boolean(errors.redirectUris)}
                {...register("redirectUris")}
              />
              <Form.Control.Feedback type="invalid">
                {errors.redirectUris?.message}
              </Form.Control.Feedback>
            </Col>
            <Col lg={6}>
              <Form.Label>{dictionary.admin.clients.postLogoutUris}</Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                isInvalid={Boolean(errors.postLogoutRedirectUris)}
                {...register("postLogoutRedirectUris")}
              />
              <Form.Control.Feedback type="invalid">
                {errors.postLogoutRedirectUris?.message}
              </Form.Control.Feedback>
            </Col>
            <Col xs={12}>
              <Form.Label>{dictionary.admin.clients.scopes}</Form.Label>
              <Form.Control
                placeholder="openid profile"
                isInvalid={Boolean(errors.scopes)}
                {...register("scopes")}
              />
              <Form.Control.Feedback type="invalid">{errors.scopes?.message}</Form.Control.Feedback>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Card className="border-0 shadow-sm mb-3">
        <Card.Body>
          <h2 className="h6 mb-3">{dictionary.admin.clients.security}</h2>
          <Form.Check
            type="switch"
            label={dictionary.admin.clients.requirePkce}
            checked={requireProofKey}
            onChange={(event) => {
              setValue("requireProofKey", event.target.checked, {
                shouldDirty: true,
                shouldValidate: true,
              });
            }}
          />
          <Form.Check
            type="switch"
            label={dictionary.admin.clients.requireConsent}
            checked={requireAuthorizationConsent}
            onChange={(event) => {
              setValue("requireAuthorizationConsent", event.target.checked, { shouldDirty: true });
            }}
          />
        </Card.Body>
      </Card>

      <div className="d-flex gap-2 justify-content-end">
        <Button variant="outline-secondary" onClick={() => router.push(`/${locale}/admin/clients`)}>
          {dictionary.admin.common.cancel}
        </Button>
        <Button type="submit" disabled={saving}>
          {saving ? dictionary.admin.common.saving : dictionary.admin.common.save}
        </Button>
      </div>

      <ResultModal
        closeLabel={dictionary.admin.common.close}
        message={dictionary.admin.clients.secretHelp}
        onClose={() => {
          if (createdClientId) {
            router.push(
              `/${locale}/admin/clients/detail?id=${encodeURIComponent(createdClientId)}`,
            );
            router.refresh();
          }
        }}
        show={createdSecret !== null}
        title={dictionary.admin.clients.secretTitle}
        value={createdSecret}
      />
    </Form>
  );
}
