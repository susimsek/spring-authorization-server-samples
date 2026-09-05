"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { z } from "zod";
import { Alert, Button, Card, Form } from "react-bootstrap";

import { useDictionary } from "@/i18n/client";
import { adminRequest } from "@/lib/admin-api";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { ViewHeader } from "./ViewHeader";

type Settings = {
  userRegistration: boolean;
  forgotPassword: boolean;
  rememberMe: boolean;
  loginWithEmail: boolean;
  verifyEmail: boolean;
  sessionTimeoutMinutes: number;
  passwordMinimumLength: number;
  bruteForceEnabled: boolean;
  bruteForceMaxFailures: number;
  otpEnabled: boolean;
  otpRequired: boolean;
  otpIssuer: string;
  otpAlgorithm: "SHA1" | "SHA256" | "SHA512";
  otpDigits: number;
  otpPeriodSeconds: number;
  otpLookAheadWindow: number;
};

export default function LoginSettingsPage({ embedded = false }: { embedded?: boolean }) {
  const dictionary = useDictionary();
  const copy = dictionary.admin.loginSettings;
  const validation = dictionary.admin.common.validation;
  const { accessToken } = useAdminAuth();
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(false);
  const [saved, setSaved] = useState(false);
  const schema = z.object({
    userRegistration: z.boolean(),
    forgotPassword: z.boolean(),
    rememberMe: z.boolean(),
    loginWithEmail: z.boolean(),
    verifyEmail: z.boolean(),
    sessionTimeoutMinutes: z.number().int().min(1, validation.positiveNumber),
    passwordMinimumLength: z.number().int().min(8, validation.minimumPasswordLength),
    bruteForceEnabled: z.boolean(),
    bruteForceMaxFailures: z.number().int().min(1, validation.positiveNumber),
    otpEnabled: z.boolean(),
    otpRequired: z.boolean(),
    otpIssuer: z.string().trim().min(1, validation.required).max(100),
    otpAlgorithm: z.enum(["SHA1", "SHA256", "SHA512"]),
    otpDigits: z
      .number()
      .int()
      .refine((value) => value === 6 || value === 8, validation.positiveNumber),
    otpPeriodSeconds: z.number().int().min(15, validation.positiveNumber),
    otpLookAheadWindow: z.number().int().min(0, validation.positiveNumber),
  });
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Settings>({
    resolver: zodResolver(schema),
    mode: "onBlur",
  });

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<Settings>(accessToken, { url: "/api/admin/settings/login" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        reset(response.data);
        setLoaded(true);
      })
      .catch(() => setError(true));
  }, [accessToken, reset]);

  const submit = handleSubmit(async (values) => {
    if (!accessToken) return;
    setSaved(false);
    setError(false);
    try {
      const response = await adminRequest<Settings>(accessToken, {
        method: "PUT",
        url: "/api/admin/settings/login",
        data: values,
      });
      if (response.status >= 300) throw new Error();
      reset(response.data);
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
                label={copy.userRegistration}
                {...register("userRegistration")}
              />
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.forgotPassword}
                {...register("forgotPassword")}
              />
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.rememberMe}
                {...register("rememberMe")}
              />
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.loginWithEmail}
                {...register("loginWithEmail")}
              />
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.verifyEmail}
                {...register("verifyEmail")}
              />
              <div className="d-grid gap-3 mb-4">
                <NumberField
                  id="login-session-timeout"
                  label={copy.sessionTimeout}
                  error={errors.sessionTimeoutMinutes?.message}
                  registration={register("sessionTimeoutMinutes", { valueAsNumber: true })}
                />
                <NumberField
                  id="login-password-minimum"
                  label={copy.passwordMinimumLength}
                  error={errors.passwordMinimumLength?.message}
                  registration={register("passwordMinimumLength", { valueAsNumber: true })}
                />
                <NumberField
                  id="login-brute-force-failures"
                  label={copy.bruteForceMaxFailures}
                  error={errors.bruteForceMaxFailures?.message}
                  registration={register("bruteForceMaxFailures", { valueAsNumber: true })}
                />
              </div>
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.bruteForceEnabled}
                {...register("bruteForceEnabled")}
              />
              <hr className="my-4" />
              <Form.Check
                className="mb-3"
                type="switch"
                label={copy.otpEnabled}
                {...register("otpEnabled")}
              />
              <Form.Check
                className="mb-4"
                type="switch"
                label={copy.otpRequired}
                {...register("otpRequired")}
              />
              <div className="d-grid gap-3 mb-4">
                <Form.Group controlId="login-otp-issuer">
                  <Form.Label>{copy.otpIssuer}</Form.Label>
                  <Form.Control isInvalid={Boolean(errors.otpIssuer)} {...register("otpIssuer")} />
                  <Form.Control.Feedback type="invalid">
                    {errors.otpIssuer?.message}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group controlId="login-otp-algorithm">
                  <Form.Label>{copy.otpAlgorithm}</Form.Label>
                  <Form.Select {...register("otpAlgorithm")}>
                    <option value="SHA1">SHA-1</option>
                    <option value="SHA256">SHA-256</option>
                    <option value="SHA512">SHA-512</option>
                  </Form.Select>
                </Form.Group>
                <NumberField
                  id="login-otp-digits"
                  label={copy.otpDigits}
                  error={errors.otpDigits?.message}
                  registration={register("otpDigits", { valueAsNumber: true })}
                />
                <NumberField
                  id="login-otp-period"
                  label={copy.otpPeriodSeconds}
                  error={errors.otpPeriodSeconds?.message}
                  registration={register("otpPeriodSeconds", { valueAsNumber: true })}
                />
                <NumberField
                  id="login-otp-look-ahead"
                  label={copy.otpLookAheadWindow}
                  error={errors.otpLookAheadWindow?.message}
                  registration={register("otpLookAheadWindow", { valueAsNumber: true })}
                />
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

function NumberField({
  id,
  label,
  error,
  registration,
}: {
  id: string;
  label: string;
  error?: string;
  registration: UseFormRegisterReturn;
}) {
  return (
    <Form.Group controlId={id}>
      <Form.Label>{label}</Form.Label>
      <Form.Control type="number" isInvalid={Boolean(error)} {...registration} />
      <Form.Control.Feedback type="invalid">{error}</Form.Control.Feedback>
    </Form.Group>
  );
}
