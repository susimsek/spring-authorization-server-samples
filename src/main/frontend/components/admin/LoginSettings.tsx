"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "@/lib/form";
import type { UseFormRegisterReturn } from "react-hook-form";
import { z } from "zod";
import { Alert, Button, Card, Form, Spinner } from "react-bootstrap";

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
  bruteForceMaxSecondaryFailures: number;
  mfaVerificationTimeoutSeconds: number;
  passwordMaximumLength: number;
  passwordMinimumUppercase: number;
  passwordMinimumLowercase: number;
  passwordMinimumDigits: number;
  passwordMinimumSpecialCharacters: number;
  passwordRejectUsername: boolean;
  passwordRejectEmail: boolean;
  passwordRejectCommonPasswords: boolean;
  passwordHistorySize: number;
  passwordExpirationDays: number;
  passwordCommonPasswords: string;
  bruteForceQuickLoginWindowMillis: number;
  bruteForceMinimumQuickLoginWaitSeconds: number;
  bruteForceWaitIncrementSeconds: number;
  bruteForceMaxWaitSeconds: number;
  bruteForceFailureResetTimeSeconds: number;
  bruteForceMaxTemporaryLockouts: number;
  bruteForcePermanentLockout: boolean;
  bruteForceIpRequestsPerMinute: number;
  bruteForceUsernameIpRequestsPerMinute: number;
  otpEnabled: boolean;
  otpRequired: boolean;
  otpIssuer: string;
  otpAlgorithm: "SHA1" | "SHA256" | "SHA512";
  otpDigits: number;
  otpPeriodSeconds: number;
  otpLookAheadWindow: number;
  otpCodeReusable: boolean;
  otpAddRecoveryCodes: boolean;
  recoveryCodeWarningThreshold: number;
};

export type LoginSettingsSection =
  "login" | "password-policy" | "otp-policy" | "brute-force" | "sessions";

export default function LoginSettingsPage({
  embedded = false,
  focusSection,
}: {
  embedded?: boolean;
  focusSection?: LoginSettingsSection;
}) {
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
    bruteForceMaxSecondaryFailures: z.number().int().min(0, validation.positiveNumber),
    mfaVerificationTimeoutSeconds: z.number().int().min(1, validation.positiveNumber),
    passwordMaximumLength: z.number().int().min(8, validation.maximumPasswordLength),
    passwordMinimumUppercase: z.number().int().min(0, validation.positiveNumber),
    passwordMinimumLowercase: z.number().int().min(0, validation.positiveNumber),
    passwordMinimumDigits: z.number().int().min(0, validation.positiveNumber),
    passwordMinimumSpecialCharacters: z.number().int().min(0, validation.positiveNumber),
    passwordRejectUsername: z.boolean(),
    passwordRejectEmail: z.boolean(),
    passwordRejectCommonPasswords: z.boolean(),
    passwordHistorySize: z.number().int().min(0, validation.positiveNumber),
    passwordExpirationDays: z.number().int().min(0, validation.positiveNumber),
    passwordCommonPasswords: z.string().trim().min(1, validation.required).max(4000),
    bruteForceQuickLoginWindowMillis: z.number().int().min(0, validation.positiveNumber),
    bruteForceMinimumQuickLoginWaitSeconds: z.number().int().min(0, validation.positiveNumber),
    bruteForceWaitIncrementSeconds: z.number().int().min(0, validation.positiveNumber),
    bruteForceMaxWaitSeconds: z.number().int().min(0, validation.positiveNumber),
    bruteForceFailureResetTimeSeconds: z.number().int().min(0, validation.positiveNumber),
    bruteForceMaxTemporaryLockouts: z.number().int().min(0, validation.positiveNumber),
    bruteForcePermanentLockout: z.boolean(),
    bruteForceIpRequestsPerMinute: z.number().int().min(1, validation.positiveNumber),
    bruteForceUsernameIpRequestsPerMinute: z.number().int().min(1, validation.positiveNumber),
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
    otpCodeReusable: z.boolean(),
    otpAddRecoveryCodes: z.boolean(),
    recoveryCodeWarningThreshold: z.number().int().min(0, validation.positiveNumber),
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

  useEffect(() => {
    if (!loaded || !focusSection) return;
    const element = document.getElementById(`login-settings-${focusSection}`);
    if (element && typeof element.scrollIntoView === "function") {
      element.scrollIntoView({ block: "start" });
    }
  }, [focusSection, loaded]);

  const activeSection = focusSection ?? "login";

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
              <section hidden={activeSection !== "login"}>
                <h2 className="h5 mb-3" id="login-settings-login">
                  {copy.sectionLogin}
                </h2>
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
                <SaveButton copy={copy.save} isSubmitting={isSubmitting} />
              </section>

              <section hidden={activeSection !== "sessions"}>
                <h2 className="h5 mb-3" id="login-settings-sessions">
                  {copy.sectionSessions}
                </h2>
                <NumberField
                  id="login-session-timeout"
                  label={copy.sessionTimeout}
                  error={errors.sessionTimeoutMinutes?.message}
                  registration={register("sessionTimeoutMinutes", { valueAsNumber: true })}
                />
                <SaveButton copy={copy.save} isSubmitting={isSubmitting} />
              </section>

              <section hidden={activeSection !== "password-policy"}>
                <h2 className="h5 mb-3" id="login-settings-password-policy">
                  {copy.sectionPasswordPolicy}
                </h2>
                <div className="d-grid gap-3">
                  <NumberField
                    id="login-password-minimum"
                    label={copy.passwordMinimumLength}
                    error={errors.passwordMinimumLength?.message}
                    registration={register("passwordMinimumLength", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-maximum"
                    label={copy.passwordMaximumLength}
                    error={errors.passwordMaximumLength?.message}
                    registration={register("passwordMaximumLength", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-uppercase"
                    label={copy.passwordMinimumUppercase}
                    error={errors.passwordMinimumUppercase?.message}
                    registration={register("passwordMinimumUppercase", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-lowercase"
                    label={copy.passwordMinimumLowercase}
                    error={errors.passwordMinimumLowercase?.message}
                    registration={register("passwordMinimumLowercase", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-digits"
                    label={copy.passwordMinimumDigits}
                    error={errors.passwordMinimumDigits?.message}
                    registration={register("passwordMinimumDigits", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-special"
                    label={copy.passwordMinimumSpecialCharacters}
                    error={errors.passwordMinimumSpecialCharacters?.message}
                    registration={register("passwordMinimumSpecialCharacters", {
                      valueAsNumber: true,
                    })}
                  />
                  <Form.Check
                    type="switch"
                    label={copy.passwordRejectUsername}
                    {...register("passwordRejectUsername")}
                  />
                  <Form.Check
                    type="switch"
                    label={copy.passwordRejectEmail}
                    {...register("passwordRejectEmail")}
                  />
                  <Form.Check
                    type="switch"
                    label={copy.passwordRejectCommonPasswords}
                    {...register("passwordRejectCommonPasswords")}
                  />
                  <NumberField
                    id="login-password-history"
                    label={copy.passwordHistorySize}
                    error={errors.passwordHistorySize?.message}
                    registration={register("passwordHistorySize", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-password-expiration"
                    label={copy.passwordExpirationDays}
                    error={errors.passwordExpirationDays?.message}
                    registration={register("passwordExpirationDays", { valueAsNumber: true })}
                  />
                  <Form.Group controlId="login-password-common-list">
                    <Form.Label>{copy.passwordCommonPasswords}</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      isInvalid={Boolean(errors.passwordCommonPasswords)}
                      {...register("passwordCommonPasswords")}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.passwordCommonPasswords?.message}
                    </Form.Control.Feedback>
                  </Form.Group>
                </div>
                <SaveButton copy={copy.save} isSubmitting={isSubmitting} />
              </section>

              <section hidden={activeSection !== "brute-force"}>
                <h2 className="h5 mb-3" id="login-settings-brute-force">
                  {copy.sectionBruteForce}
                </h2>
                <Form.Check
                  className="mb-4"
                  type="switch"
                  label={copy.bruteForceEnabled}
                  {...register("bruteForceEnabled")}
                />
                <div className="d-grid gap-3">
                  <NumberField
                    id="login-brute-force-failures"
                    label={copy.bruteForceMaxFailures}
                    error={errors.bruteForceMaxFailures?.message}
                    registration={register("bruteForceMaxFailures", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-mfa-brute-force-failures"
                    label={copy.bruteForceMaxSecondaryFailures}
                    error={errors.bruteForceMaxSecondaryFailures?.message}
                    registration={register("bruteForceMaxSecondaryFailures", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-mfa-verification-timeout"
                    label={copy.mfaVerificationTimeout}
                    error={errors.mfaVerificationTimeoutSeconds?.message}
                    registration={register("mfaVerificationTimeoutSeconds", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-quick-window"
                    label={copy.bruteForceQuickLoginWindowMillis}
                    error={errors.bruteForceQuickLoginWindowMillis?.message}
                    registration={register("bruteForceQuickLoginWindowMillis", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-quick-wait"
                    label={copy.bruteForceMinimumQuickLoginWaitSeconds}
                    error={errors.bruteForceMinimumQuickLoginWaitSeconds?.message}
                    registration={register("bruteForceMinimumQuickLoginWaitSeconds", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-wait-increment"
                    label={copy.bruteForceWaitIncrementSeconds}
                    error={errors.bruteForceWaitIncrementSeconds?.message}
                    registration={register("bruteForceWaitIncrementSeconds", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-max-wait"
                    label={copy.bruteForceMaxWaitSeconds}
                    error={errors.bruteForceMaxWaitSeconds?.message}
                    registration={register("bruteForceMaxWaitSeconds", { valueAsNumber: true })}
                  />
                  <NumberField
                    id="login-failure-reset"
                    label={copy.bruteForceFailureResetTimeSeconds}
                    error={errors.bruteForceFailureResetTimeSeconds?.message}
                    registration={register("bruteForceFailureResetTimeSeconds", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-max-temporary-lockouts"
                    label={copy.bruteForceMaxTemporaryLockouts}
                    error={errors.bruteForceMaxTemporaryLockouts?.message}
                    registration={register("bruteForceMaxTemporaryLockouts", {
                      valueAsNumber: true,
                    })}
                  />
                  <Form.Check
                    type="switch"
                    label={copy.bruteForcePermanentLockout}
                    {...register("bruteForcePermanentLockout")}
                  />
                  <NumberField
                    id="login-ip-rate-limit"
                    label={copy.bruteForceIpRequestsPerMinute}
                    error={errors.bruteForceIpRequestsPerMinute?.message}
                    registration={register("bruteForceIpRequestsPerMinute", {
                      valueAsNumber: true,
                    })}
                  />
                  <NumberField
                    id="login-user-ip-rate-limit"
                    label={copy.bruteForceUsernameIpRequestsPerMinute}
                    error={errors.bruteForceUsernameIpRequestsPerMinute?.message}
                    registration={register("bruteForceUsernameIpRequestsPerMinute", {
                      valueAsNumber: true,
                    })}
                  />
                </div>
                <SaveButton copy={copy.save} isSubmitting={isSubmitting} />
              </section>

              <section hidden={activeSection !== "otp-policy"}>
                <h2 className="h5 mb-3" id="login-settings-otp-policy">
                  {copy.sectionOtpPolicy}
                </h2>
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
                <div className="d-grid gap-3">
                  <Form.Group controlId="login-otp-issuer">
                    <Form.Label>{copy.otpIssuer}</Form.Label>
                    <Form.Control
                      isInvalid={Boolean(errors.otpIssuer)}
                      {...register("otpIssuer")}
                    />
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
                  <Form.Check
                    type="switch"
                    label={copy.otpCodeReusable}
                    {...register("otpCodeReusable")}
                  />
                  <Form.Check
                    type="switch"
                    label={copy.otpAddRecoveryCodes}
                    {...register("otpAddRecoveryCodes")}
                  />
                  <NumberField
                    id="login-recovery-code-warning-threshold"
                    label={copy.recoveryCodeWarningThreshold}
                    error={errors.recoveryCodeWarningThreshold?.message}
                    registration={register("recoveryCodeWarningThreshold", { valueAsNumber: true })}
                  />
                </div>
                <SaveButton copy={copy.save} isSubmitting={isSubmitting} />
              </section>
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

function SaveButton({ copy, isSubmitting }: { copy: string; isSubmitting: boolean }) {
  return (
    <div className="admin-form-actions mt-4">
      <Button disabled={isSubmitting} type="submit">
        {isSubmitting ? (
          <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
        ) : (
          <AdminActionIcon action="save" />
        )}
        {copy}
      </Button>
    </div>
  );
}
