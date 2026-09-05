"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { DetailLoadingState, ErrorState } from "@/components/admin/AsyncState";
import { ReadOnlyMetadata } from "@/components/admin/ReadOnlyMetadata";
import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import { ActionIcon } from "@/components/shared/ActionIcon";
import type { Dictionary } from "@/i18n/get-dictionary";
import { useDateTimeFormatter } from "@/i18n/useDateTimeFormatter";
import { problemViolations } from "@/lib/problem-detail";
import {
  type AccountApiError,
  useGetAccountProfileQuery,
  useSendAccountVerificationEmailMutation,
  useUpdateAccountProfileMutation,
} from "@/store/account-api-slice";

import { useAccountAuth } from "./AccountAuthProvider";

type Values = { firstName: string; lastName: string; email: string };

export function AccountProfileForm({ dictionary }: { dictionary: Dictionary }) {
  const formatDateTime = useDateTimeFormatter();
  const { accessToken } = useAccountAuth();
  const alerts = useConsoleAlerts();
  const copy = dictionary.account;
  const {
    data: profile,
    isError,
    isLoading,
    refetch,
  } = useGetAccountProfileQuery({ accessToken: accessToken ?? "" }, { skip: !accessToken });
  const [updateProfile] = useUpdateAccountProfileMutation();
  const [sendVerificationEmail, { isLoading: verificationSending }] =
    useSendAccountVerificationEmailMutation();
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

  useEffect(() => {
    if (profile) {
      reset({
        firstName: profile.firstName ?? "",
        lastName: profile.lastName ?? "",
        email: profile.email ?? "",
      });
    }
  }, [profile, reset]);

  const submit = handleSubmit(async (values) => {
    if (!accessToken) return;
    try {
      const updated = await updateProfile({
        accessToken,
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
      }).unwrap();
      reset({
        firstName: updated.firstName ?? "",
        lastName: updated.lastName ?? "",
        email: updated.email ?? "",
      });
      alerts.addAlert(copy.profile.saved);
    } catch (error) {
      const violations = problemViolations((error as AccountApiError).data);
      let firstInvalid: keyof Values | undefined;
      violations.forEach(({ field }) => {
        if (field === "firstName" || field === "lastName" || field === "email") {
          firstInvalid ??= field;
          setError(field, { message: copy.validation.invalid });
        }
      });
      if (firstInvalid) setFocus(firstInvalid);
      alerts.addError(copy.common.operationError);
    }
  });

  const requestVerification = async () => {
    if (!accessToken) return;
    try {
      await sendVerificationEmail({ accessToken }).unwrap();
      alerts.addAlert(copy.profile.verificationSent);
    } catch {
      alerts.addError(copy.common.operationError);
    }
  };

  if (isLoading) return <DetailLoadingState />;
  if (isError && !profile)
    return (
      <ErrorState
        message={copy.common.operationError}
        retryLabel={copy.common.retry}
        onRetry={() => void refetch()}
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
                {profile.email && (
                  <div className="d-flex align-items-center gap-2 mt-2">
                    <Badge bg={profile.emailVerified ? "success" : "warning"}>
                      {profile.emailVerified
                        ? copy.profile.emailVerified
                        : copy.profile.emailUnverified}
                    </Badge>
                    {!profile.emailVerified && (
                      <Button
                        type="button"
                        size="sm"
                        variant="link"
                        className="p-0"
                        disabled={verificationSending}
                        onClick={() => void requestVerification()}
                      >
                        <ActionIcon action="verify" />
                        {copy.profile.sendVerification}
                      </Button>
                    )}
                  </div>
                )}
              </Form.Group>
            </Col>
            <Col xs={12}>
              <div className="account-metadata-panel">
                <div className="small fw-semibold mb-3">{copy.profile.metadata}</div>
                <ReadOnlyMetadata
                  items={[
                    {
                      label: copy.profile.createdAt,
                      value: formatDateTime(profile.createdAt),
                    },
                    {
                      label: copy.profile.updatedAt,
                      value: formatDateTime(profile.updatedAt),
                    },
                  ]}
                />
              </div>
            </Col>
          </Row>
          <div className="account-form-actions mt-4 pt-4 border-top">
            <Button type="submit" disabled={!isDirty || isSubmitting} data-cy="save-profile">
              <ActionIcon action="save" />
              {isSubmitting ? copy.common.saving : copy.common.save}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={!isDirty || isSubmitting}
              onClick={() =>
                reset({
                  firstName: profile.firstName ?? "",
                  lastName: profile.lastName ?? "",
                  email: profile.email ?? "",
                })
              }
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
