"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useCallback, useEffect, useState } from "react";
import { Button, Card, Col, Form, Row } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { DetailLoadingState, ErrorState } from "@/components/admin/AsyncState";
import { ReadOnlyMetadata } from "@/components/admin/ReadOnlyMetadata";
import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import type { Dictionary } from "@/i18n/get-dictionary";
import { accountRequest } from "@/lib/account-api";
import { problemViolations } from "@/lib/problem-detail";

import { useAccountAuth } from "./AccountAuthProvider";

type Profile = {
  username: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  createdAt: string;
  updatedAt: string;
};
type Values = { firstName: string; lastName: string; email: string };

export function AccountProfileForm({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAccountAuth();
  const alerts = useConsoleAlerts();
  const copy = dictionary.account;
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const schema = z.object({
    firstName: z.string().trim().max(100, copy.validation.max100),
    lastName: z.string().trim().max(100, copy.validation.max100),
    email: z
      .string()
      .trim()
      .max(200, copy.validation.max200)
      .refine((value) => value === "" || z.email().safeParse(value).success, copy.validation.email),
  });
  const {
    register,
    handleSubmit,
    reset,
    setError,
    setFocus,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: { firstName: "", lastName: "", email: "" },
  });

  const applyProfile = useCallback(
    (value: Profile) => {
      setProfile(value);
      reset({
        firstName: value.firstName ?? "",
        lastName: value.lastName ?? "",
        email: value.email ?? "",
      });
    },
    [reset],
  );

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    const response = await accountRequest<Profile>(accessToken, { url: "/api/account/profile" });
    if (response.status < 300) {
      applyProfile(response.data);
      setFailed(false);
    } else setFailed(true);
    setLoading(false);
  }, [accessToken, applyProfile]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const submit = handleSubmit(async (values) => {
    if (!accessToken) return;
    setFailed(false);
    const response = await accountRequest<Profile>(accessToken, {
      method: "PUT",
      url: "/api/account/profile",
      data: {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
      },
    });
    if (response.status >= 300) {
      const violations = problemViolations(response.data);
      let firstInvalid: keyof Values | undefined;
      violations.forEach(({ field }) => {
        if (field === "firstName" || field === "lastName" || field === "email") {
          firstInvalid ??= field;
          setError(field, { message: copy.validation.invalid });
        }
      });
      if (firstInvalid) setFocus(firstInvalid);
      setFailed(true);
      return;
    }
    applyProfile(response.data);
    alerts.addAlert(copy.profile.saved);
  });

  if (loading) return <DetailLoadingState />;
  if (failed && !profile)
    return (
      <ErrorState
        message={copy.common.operationError}
        retryLabel={copy.common.retry}
        onRetry={() => void load()}
      />
    );
  if (!profile) return null;

  return (
    <Card className="admin-panel-card account-panel-card">
      <Card.Header className="bg-body p-4 border-bottom">
        <h2 className="h5 mb-1">{copy.profile.sectionTitle}</h2>
        <div className="small text-body-secondary">{copy.profile.sectionHelp}</div>
      </Card.Header>
      <Card.Body className="p-4">
        <Form onSubmit={submit} noValidate>
          <Row className="g-4">
            <Col xs={12}>
              <Form.Group controlId="account-username">
                <Form.Label>{copy.profile.username}</Form.Label>
                <Form.Control value={profile.username} readOnly aria-readonly="true" />
                <Form.Text id="account-username-help">{copy.profile.usernameHelp}</Form.Text>
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group controlId="account-first-name">
                <Form.Label>{copy.profile.firstName}</Form.Label>
                <Form.Control
                  {...register("firstName")}
                  isInvalid={Boolean(errors.firstName)}
                  aria-describedby={errors.firstName ? "first-name-error" : undefined}
                />
                <Form.Control.Feedback id="first-name-error" type="invalid">
                  {errors.firstName?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group controlId="account-last-name">
                <Form.Label>{copy.profile.lastName}</Form.Label>
                <Form.Control
                  {...register("lastName")}
                  isInvalid={Boolean(errors.lastName)}
                  aria-describedby={errors.lastName ? "last-name-error" : undefined}
                />
                <Form.Control.Feedback id="last-name-error" type="invalid">
                  {errors.lastName?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Group controlId="account-email">
                <Form.Label>{copy.profile.email}</Form.Label>
                <Form.Control
                  type="email"
                  autoComplete="email"
                  {...register("email")}
                  isInvalid={Boolean(errors.email)}
                  aria-describedby={errors.email ? "email-error" : undefined}
                />
                <Form.Control.Feedback id="email-error" type="invalid">
                  {errors.email?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>
            <Col xs={12}>
              <div className="account-metadata-panel">
                <div className="small fw-semibold mb-3">{copy.profile.metadata}</div>
                <ReadOnlyMetadata
                  items={[
                    {
                      label: copy.profile.createdAt,
                      value: new Date(profile.createdAt).toLocaleString(),
                    },
                    {
                      label: copy.profile.updatedAt,
                      value: new Date(profile.updatedAt).toLocaleString(),
                    },
                  ]}
                />
              </div>
            </Col>
          </Row>
          <div className="account-form-actions mt-4 pt-4 border-top">
            <Button type="submit" disabled={!isDirty || isSubmitting} data-cy="save-profile">
              {isSubmitting ? copy.common.saving : copy.common.save}
            </Button>
            <Button
              type="button"
              variant="outline-secondary"
              disabled={!isDirty || isSubmitting}
              onClick={() =>
                reset({
                  firstName: profile.firstName ?? "",
                  lastName: profile.lastName ?? "",
                  email: profile.email ?? "",
                })
              }
            >
              {copy.common.cancel}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
