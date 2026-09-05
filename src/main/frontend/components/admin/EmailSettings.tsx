"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Alert, Button, Card, Form } from "react-bootstrap";

import { useDictionary } from "@/i18n/client";
import { useAdminAuth } from "./AdminAuthProvider";
import { adminRequest } from "@/lib/admin-api";
import { AdminActionIcon } from "./AdminActionIcon";
import { ViewHeader } from "./ViewHeader";

type Settings = {
  enabled: boolean;
  fromAddress: string;
  baseUrl: string;
  host: string;
  port: number;
  username: string;
  password: string;
  smtpAuth: boolean;
  starttls: boolean;
  ssl: boolean;
};
type EmailResponse = Omit<Settings, "password"> & { passwordConfigured: boolean };

export default function EmailSettingsPage({ embedded = false }: { embedded?: boolean }) {
  const copy = useDictionary().admin.emailSettings;
  const validation = useDictionary().admin.common.validation;
  const { accessToken } = useAdminAuth();
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(false);
  const [saved, setSaved] = useState(false);
  const [passwordConfigured, setPasswordConfigured] = useState(false);
  const schema = z.object({
    enabled: z.boolean(),
    fromAddress: z.string().trim().email(validation.email),
    baseUrl: z.string().trim().url(validation.uri),
    host: z.string().trim().min(1, validation.required),
    port: z.number().int().min(1).max(65535),
    username: z.string(),
    password: z.string(),
    smtpAuth: z.boolean(),
    starttls: z.boolean(),
    ssl: z.boolean(),
  });
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Settings>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: {
      enabled: false,
      fromAddress: "",
      baseUrl: "",
      host: "",
      port: 587,
      username: "",
      password: "",
      smtpAuth: true,
      starttls: true,
      ssl: false,
    },
  });

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<EmailResponse>(accessToken, { url: "/api/admin/settings/email" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        reset({ ...response.data, password: "" } as Settings);
        setPasswordConfigured(Boolean(response.data.passwordConfigured));
        setLoaded(true);
      })
      .catch(() => setError(true));
  }, [accessToken, reset]);

  const submit = handleSubmit(async (values) => {
    if (!accessToken) return;
    setSaved(false);
    setError(false);
    try {
      const response = await adminRequest<EmailResponse>(accessToken, {
        method: "PUT",
        url: "/api/admin/settings/email",
        data: values,
      });
      if (response.status >= 300) throw new Error();
      reset({ ...response.data, password: "" } as Settings);
      setPasswordConfigured(Boolean(response.data.passwordConfigured));
      setSaved(true);
    } catch {
      setError(true);
    }
  });

  return (
    <div className="d-grid gap-4">
      {!embedded && <ViewHeader title={copy.title} description={copy.subtitle} />}
      {error && <Alert variant="danger">{copy.error}</Alert>}
      {saved && <Alert variant="success">{copy.saved}</Alert>}
      <Card className="admin-panel-card">
        <Card.Body>
          {!loaded ? (
            <div role="status">{copy.loading}</div>
          ) : (
            <Form noValidate onSubmit={submit}>
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.enabled}
                {...register("enabled")}
              />
              <div className="d-grid gap-3">
                <Form.Group controlId="email-from-address">
                  <Form.Label>{copy.fromAddress}</Form.Label>
                  <Form.Control
                    isInvalid={Boolean(errors.fromAddress)}
                    {...register("fromAddress")}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.fromAddress?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group controlId="email-port">
                  <Form.Label>{copy.port}</Form.Label>
                  <Form.Control
                    type="number"
                    isInvalid={Boolean(errors.port)}
                    {...register("port", { valueAsNumber: true })}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.port?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group controlId="email-host">
                  <Form.Label>{copy.host}</Form.Label>
                  <Form.Control isInvalid={Boolean(errors.host)} {...register("host")} />
                  <Form.Control.Feedback type="invalid">
                    {errors.host?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group controlId="email-username">
                  <Form.Label>{copy.username}</Form.Label>
                  <Form.Control {...register("username")} />
                </Form.Group>
                <Form.Group controlId="email-password">
                  <Form.Label>{copy.password}</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    {...register("password")}
                  />
                  {passwordConfigured && <Form.Text>{copy.passwordConfigured}</Form.Text>}
                </Form.Group>
                <Form.Group controlId="email-base-url">
                  <Form.Label>{copy.baseUrl}</Form.Label>
                  <Form.Control isInvalid={Boolean(errors.baseUrl)} {...register("baseUrl")} />
                  <Form.Control.Feedback type="invalid">
                    {errors.baseUrl?.message}
                  </Form.Control.Feedback>
                </Form.Group>
              </div>
              <div className="d-grid gap-3 my-4">
                <Form.Check type="switch" label={copy.smtpAuth} {...register("smtpAuth")} />
                <Form.Check type="switch" label={copy.starttls} {...register("starttls")} />
                <Form.Check type="switch" label={copy.ssl} {...register("ssl")} />
              </div>
              <div className="admin-form-actions">
                <Button disabled={isSubmitting} type="submit">
                  <AdminActionIcon action="save" />
                  {isSubmitting ? copy.saving : copy.save}
                </Button>
              </div>
            </Form>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}
