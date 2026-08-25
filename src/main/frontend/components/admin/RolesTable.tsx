"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Card, Form } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { ConfirmModal } from "./ConfirmModal";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { useAdminTableState } from "./useAdminTableState";

type Role = { name: string };
type RolePage = { content: Role[]; totalPages: number; totalElements: number };

export function RolesTable({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAdminAuth();
  const params = useParams<{ lang: string }>();
  const lang = params?.lang ?? "en";
  const copy = dictionary.admin.roles;
  const [roles, setRoles] = useState<Role[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [roleToDelete, setRoleToDelete] = useState<string | null>(null);
  const { page, query, setPage, setQuery, setSize, size } = useAdminTableState();
  const roleSchema = z.object({
    name: z
      .string()
      .trim()
      .max(50, dictionary.admin.common.validation.max50)
      .regex(/^ROLE_[A-Z0-9_]+$/, dictionary.admin.common.validation.roleFormat),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setError: setFieldError,
  } = useForm<{ name: string }>({
    resolver: zodResolver(roleSchema),
    mode: "onBlur",
    defaultValues: { name: "" },
  });

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<RolePage>(accessToken, {
      url: `/api/admin/roles?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setRoles(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setError(false);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, refresh, size]);

  const createRole = async ({ name }: { name: string }) => {
    if (!accessToken || !name) return;
    setSaving(true);
    try {
      const response = await adminRequest<Role>(accessToken, {
        url: "/api/admin/roles",
        method: "POST",
        data: { name },
      });
      if (response.status >= 300) {
        const errorCode = problemErrorCode(response.data);
        if (problemViolations(response.data).some(({ field }) => field === "name")) {
          setFieldError("name", {
            message:
              errorCode === "admin_role_duplicate_name"
                ? dictionary.admin.common.validation.roleDuplicate
                : dictionary.admin.common.validation.roleFormat,
          });
        }
        throw new Error();
      }
      reset({ name: "" });
      setLoading(true);
      setRefresh((current) => current + 1);
      setError(false);
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  const deleteRole = async (role: string) => {
    if (!accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(role)}`,
        method: "DELETE",
      });
      if (response.status >= 300) throw new Error();
      setLoading(true);
      if (roles.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        setRefresh((current) => current + 1);
      }
      setError(false);
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState />;
  return (
    <>
      {error && <ErrorState message={copy.operationError} />}
      <Card className="border-0 shadow-sm mb-3">
        <Card.Body>
          <Form className="d-flex gap-2" onSubmit={handleSubmit(createRole)}>
            <Form.Control
              aria-label={copy.name}
              isInvalid={Boolean(errors.name)}
              {...register("name")}
              placeholder="ROLE_AUDITOR"
            />
            <Button disabled={saving} type="submit">
              <AdminActionIcon action="add" />
              {copy.create}
            </Button>
          </Form>
          {errors.name && <div className="invalid-feedback d-block">{errors.name.message}</div>}
          <Form.Text>{copy.help}</Form.Text>
        </Card.Body>
      </Card>
      <ResourceFilters
        key={query}
        onQueryChange={setQuery}
        query={query}
        searchLabel={dictionary.admin.resources.search}
      />
      <DataTable
        emptyMessage={dictionary.admin.resources.empty}
        footer={
          totalElements > 0 ? (
            <PaginationControls
              next={dictionary.admin.resources.next}
              previous={dictionary.admin.resources.previous}
              first={dictionary.admin.resources.first}
              last={dictionary.admin.resources.last}
              rowsPerPage={dictionary.admin.resources.rowsPerPage}
              pageLabel={dictionary.admin.resources.page}
              onPageChange={setPage}
              onSizeChange={setSize}
              page={page}
              size={size}
              totalElements={totalElements}
              totalPages={totalPages}
            />
          ) : undefined
        }
        isEmpty={roles.length === 0}
      >
        <thead>
          <tr>
            <th>{copy.name}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {roles.map((role) => (
            <tr key={role.name}>
              <td className="font-monospace" data-label={copy.name}>
                <Link
                  className="text-decoration-none"
                  href={`/${lang}/admin/roles/${encodeURIComponent(role.name)}`}
                >
                  {role.name}
                </Link>
              </td>
              <td className="text-end">
                <Button
                  disabled={saving || role.name === "ROLE_ADMIN" || role.name === "ROLE_USER"}
                  onClick={() => setRoleToDelete(role.name)}
                  size="sm"
                  variant="outline-danger"
                >
                  <AdminActionIcon action="delete" />
                  {copy.delete}
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
      <ConfirmModal
        cancelLabel={dictionary.admin.common.cancel}
        confirmLabel={copy.delete}
        message={copy.deleteConfirm}
        onCancel={() => setRoleToDelete(null)}
        onConfirm={() => {
          if (roleToDelete) void deleteRole(roleToDelete);
          setRoleToDelete(null);
        }}
        show={roleToDelete !== null}
      />
    </>
  );
}
