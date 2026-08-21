"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Card, Form } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { ConfirmModal } from "./ConfirmModal";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { useAdminTableState } from "./useAdminTableState";

type Role = { name: string };

export function RolesTable({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAdminAuth();
  const copy = dictionary.admin.roles;
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [roleToDelete, setRoleToDelete] = useState<string | null>(null);
  const { page, query, setPage, setQuery, setSize, size } = useAdminTableState();
  const roleSchema = z.object({
    name: z
      .string()
      .trim()
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
    adminRequest<Role[]>(accessToken, { url: "/api/admin/roles" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setRoles(response.data);
        setError(false);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken]);

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
      const rolesResponse = await adminRequest<Role[]>(accessToken, { url: "/api/admin/roles" });
      if (rolesResponse.status >= 300) throw new Error();
      reset({ name: "" });
      setRoles(rolesResponse.data);
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
      setRoles((current) => current.filter((currentRole) => currentRole.name !== role));
      setError(false);
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState />;
  const filteredRoles = roles.filter((role) =>
    role.name.toLowerCase().includes(query.toLowerCase()),
  );
  const totalPages = Math.ceil(filteredRoles.length / size);
  const visibleRoles = filteredRoles.slice(page * size, (page + 1) * size);
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
      >
        <Form.Select
          aria-label={dictionary.admin.resources.records}
          onChange={(event) => setSize(Number(event.target.value))}
          style={{ maxWidth: "6rem" }}
          value={size}
        >
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </Form.Select>
      </ResourceFilters>
      <DataTable
        emptyMessage={dictionary.admin.resources.empty}
        footer={
          <PaginationControls
            next={dictionary.admin.resources.next}
            onPageChange={setPage}
            page={page}
            previous={dictionary.admin.resources.previous}
            totalPages={totalPages}
          />
        }
        isEmpty={filteredRoles.length === 0}
      >
        <thead>
          <tr>
            <th>{copy.name}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {visibleRoles.map((role) => (
            <tr key={role.name}>
              <td className="font-monospace" data-label={copy.name}>
                {role.name}
              </td>
              <td className="text-end">
                <Button
                  disabled={saving || role.name === "ROLE_ADMIN" || role.name === "ROLE_USER"}
                  onClick={() => setRoleToDelete(role.name)}
                  size="sm"
                  variant="outline-danger"
                >
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
