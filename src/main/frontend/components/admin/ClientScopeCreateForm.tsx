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

type Values = { name: string; displayName: string; description: string };

export function ClientScopeCreateForm({ dictionary }: { dictionary: Dictionary; locale: Locale }) {
  const { access, accessToken } = useAdminAuth();
  const canManageClients = Boolean(access?.manageClients);
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const copy = dictionary.admin.clientScopes;
  const common = dictionary.admin.common;
  const schema = z.object({
    name: z.string().trim().min(1, common.validation.required).max(100, common.validation.max100),
    displayName: z.string().max(200, common.validation.max200),
    description: z.string().max(500, common.validation.max500),
  });
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<Values>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { name: "", displayName: "", description: "" },
  });

  const submit = async (values: Values) => {
    if (!accessToken || !canManageClients) return;
    try {
      const response = await adminRequest(accessToken, {
        url: "/api/admin/client-scopes",
        method: "POST",
        data: values,
      });
      if (response.status >= 300) {
        if (problemViolations(response.data).some(({ field }) => field === "name")) {
          setError("name", {
            message:
              problemErrorCode(response.data) === "admin_client_scope_duplicate"
                ? common.validation.scopeDuplicate
                : common.validation.required,
          });
          return;
        }
        throw new Error();
      }
      router.push(`/admin/client-scopes`);
    } catch {
      alerts.addError(copy.operationError);
    }
  };

  return (
    <Card className="admin-panel-card admin-create-card">
      <Card.Body>
        <Form className="admin-create-form" noValidate onSubmit={handleSubmit(submit)}>
          <Form.Group className="mb-3" controlId="client-scope-name">
            <Form.Label>{copy.name}</Form.Label>
            <Form.Control
              autoComplete="off"
              autoFocus
              isInvalid={Boolean(errors.name)}
              maxLength={100}
              disabled={!canManageClients}
              {...register("name")}
            />
            <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3" controlId="client-scope-display-name">
            <Form.Label>{copy.displayName}</Form.Label>
            <Form.Control
              isInvalid={Boolean(errors.displayName)}
              maxLength={200}
              disabled={!canManageClients}
              {...register("displayName")}
            />
            <Form.Control.Feedback type="invalid">
              {errors.displayName?.message}
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3" controlId="client-scope-description">
            <Form.Label>{copy.description}</Form.Label>
            <Form.Control
              as="textarea"
              isInvalid={Boolean(errors.description)}
              maxLength={500}
              rows={4}
              disabled={!canManageClients}
              {...register("description")}
            />
            <Form.Control.Feedback type="invalid">
              {errors.description?.message}
            </Form.Control.Feedback>
          </Form.Group>
          <div className="admin-create-actions">
            <Button variant="secondary" onClick={() => router.push(`/admin/client-scopes`)}>
              <AdminActionIcon action="cancel" />
              {common.cancel}
            </Button>
            <Button disabled={!canManageClients || isSubmitting} type="submit">
              <AdminActionIcon action="add" />
              {isSubmitting ? common.saving : copy.create}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
