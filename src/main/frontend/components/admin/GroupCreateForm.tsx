"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Card, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import type { PageResponse } from "@/lib/api-types";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";

type Group = { id: number; name: string; path: string };

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
  const [groups, setGroups] = useState<Group[]>([]);
  const schema = z.object({
    name: z.string().trim().min(1, dictionary.admin.common.validation.required).max(100),
    parentId: z.string(),
  });
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { name: "", parentId: "" },
  });

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<PageResponse<Group>>(accessToken, { url: "/api/admin/groups?page=0&size=100" })
      .then((response) => setGroups(response.status < 300 ? response.data.content : []))
      .catch(() => setGroups([]));
  }, [accessToken]);

  const submit = async ({ name, parentId }: z.infer<typeof schema>) => {
    if (!accessToken) return;
    try {
      const response = await adminRequest<Group>(accessToken, {
        url: "/api/admin/groups",
        method: "POST",
        data: { name, parentId: parentId ? Number(parentId) : null },
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
    <Card className="admin-panel-card admin-create-card">
      <Card.Body>
        <Form className="admin-create-form" noValidate onSubmit={handleSubmit(submit)}>
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
          <Form.Group className="mb-3" controlId="group-parent">
            <Form.Label>{copy.parent}</Form.Label>
            <Form.Select {...register("parentId")}>
              <option value="">{copy.rootGroup}</option>
              {groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.path}
                </option>
              ))}
            </Form.Select>
            <Form.Text>{copy.parentHelp}</Form.Text>
          </Form.Group>
          <div className="admin-create-actions">
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
