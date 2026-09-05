"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "@/routing/navigation";
import { Button, Card, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";

type Role = { name: string };

export function RoleCreateForm({ dictionary }: { dictionary: Dictionary; locale: Locale }) {
  const { accessToken } = useAdminAuth();
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const copy = dictionary.admin.roles;
  const schema = z.object({
    name: z
      .string()
      .trim()
      .min(1, dictionary.admin.common.validation.required)
      .max(50, dictionary.admin.common.validation.max50)
      .regex(/^ROLE_[A-Z0-9_]+$/, dictionary.admin.common.validation.roleFormat),
  });
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { name: "" },
  });

  const submit = async ({ name }: z.infer<typeof schema>) => {
    if (!accessToken) return;
    try {
      const response = await adminRequest<Role>(accessToken, {
        url: "/api/admin/roles",
        method: "POST",
        data: { name },
      });
      if (response.status >= 300) {
        if (problemViolations(response.data).some(({ field }) => field === "name")) {
          setError("name", {
            message:
              problemErrorCode(response.data) === "admin_role_duplicate_name"
                ? dictionary.admin.common.validation.roleDuplicate
                : dictionary.admin.common.validation.roleFormat,
          });
          return;
        }
        throw new Error();
      }
      router.push(`/admin/roles/${encodeURIComponent(response.data.name)}`);
    } catch {
      alerts.addError(copy.operationError);
    }
  };

  return (
    <Card className="admin-panel-card admin-create-card">
      <Card.Body>
        <Form className="admin-create-form" noValidate onSubmit={handleSubmit(submit)}>
          <Form.Group className="mb-3" controlId="role-name">
            <Form.Label>{copy.name}</Form.Label>
            <Form.Control
              autoComplete="off"
              autoFocus
              isInvalid={Boolean(errors.name)}
              maxLength={50}
              placeholder="ROLE_AUDITOR"
              {...register("name")}
            />
            <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
            <Form.Text>{copy.help}</Form.Text>
          </Form.Group>
          <div className="admin-create-actions">
            <Button variant="secondary" onClick={() => router.push(`/admin/roles`)}>
              <AdminActionIcon action="cancel" />
              {dictionary.admin.common.cancel}
            </Button>
            <Button disabled={isSubmitting} type="submit">
              <AdminActionIcon action="add" />
              {isSubmitting ? dictionary.admin.common.saving : copy.create}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
