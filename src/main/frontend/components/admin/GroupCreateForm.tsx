"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
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

type Group = { id: number; name: string };

export function GroupCreateForm({
  dictionary,
  locale,
}: {
  dictionary: Dictionary;
  locale: Locale;
}) {
  const { accessToken } = useAdminAuth();
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const copy = dictionary.admin.groups;
  const schema = z.object({
    name: z.string().trim().min(1, dictionary.admin.common.validation.required).max(100),
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
      const response = await adminRequest<Group>(accessToken, {
        url: "/api/admin/groups",
        method: "POST",
        data: { name },
      });
      if (response.status >= 300) {
        if (problemViolations(response.data).some(({ field }) => field === "name")) {
          setError("name", {
            message:
              problemErrorCode(response.data) === "group_duplicate_name"
                ? dictionary.admin.common.validation.groupDuplicate
                : dictionary.admin.common.validation.required,
          });
          return;
        }
        throw new Error();
      }
      router.push(`/${locale}/admin/groups/${response.data.id}`);
    } catch {
      alerts.addError(copy.operationError);
    }
  };

  return (
    <Card className="admin-panel-card">
      <Card.Body>
        <Form noValidate onSubmit={handleSubmit(submit)}>
          <Form.Group className="mb-3" controlId="group-name">
            <Form.Label>{copy.name}</Form.Label>
            <Form.Control
              autoComplete="off"
              autoFocus
              isInvalid={Boolean(errors.name)}
              maxLength={100}
              placeholder="finance-operators"
              {...register("name")}
            />
            <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
            <Form.Text>{copy.help}</Form.Text>
          </Form.Group>
          <div className="d-flex gap-2">
            <Button
              variant="outline-secondary"
              onClick={() => router.push(`/${locale}/admin/groups`)}
            >
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
