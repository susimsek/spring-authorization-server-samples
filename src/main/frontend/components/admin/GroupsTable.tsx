"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Card, Form } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { ConfirmModal } from "./ConfirmModal";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { useAdminTableState } from "./useAdminTableState";

type Group = { id: number; name: string; roles: string[]; userCount: number };
type GroupPage = { content: Group[]; totalPages: number; totalElements: number };

export function GroupsTable({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAdminAuth();
  const params = useParams<{ lang: string }>();
  const lang = params?.lang ?? "en";
  const copy = dictionary.admin.groups;
  const [groups, setGroups] = useState<Group[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [groupToDelete, setGroupToDelete] = useState<Group | null>(null);
  const { page, query, setPage, setQuery, setSize, size } = useAdminTableState();
  const schema = z.object({
    name: z.string().trim().min(1, dictionary.admin.common.validation.required).max(100),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setError: setFieldError,
  } = useForm<{ name: string }>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { name: "" },
  });

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<GroupPage>(accessToken, {
      url: `/api/admin/groups?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setGroups(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setError(false);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, refresh, size]);

  const create = async ({ name }: { name: string }) => {
    if (!accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest<Group>(accessToken, {
        url: "/api/admin/groups",
        method: "POST",
        data: { name },
      });
      if (response.status >= 300) {
        if (problemViolations(response.data).some(({ field }) => field === "name")) {
          setFieldError("name", { message: dictionary.admin.common.validation.required });
        }
        throw new Error();
      }
      reset();
      setLoading(true);
      setRefresh((current) => current + 1);
      setError(false);
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (group: Group) => {
    if (!accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/groups/${group.id}`,
        method: "DELETE",
      });
      if (response.status >= 300) throw new Error();
      setLoading(true);
      if (groups.length === 1 && page > 0) setPage(page - 1);
      else setRefresh((current) => current + 1);
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
          <Form className="d-flex gap-2" onSubmit={handleSubmit(create)}>
            <Form.Control
              aria-label={copy.name}
              isInvalid={Boolean(errors.name)}
              placeholder="finance-operators"
              {...register("name")}
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
        isEmpty={groups.length === 0}
      >
        <thead>
          <tr>
            <th>{copy.name}</th>
            <th>{copy.roleMappings}</th>
            <th>{copy.members}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {groups.map((group) => (
            <tr key={group.id}>
              <td data-label={copy.name}>
                <Link className="text-decoration-none" href={`/${lang}/admin/groups/${group.id}`}>
                  {group.name}
                </Link>
              </td>
              <td data-label={copy.roleMappings}>{group.roles.length}</td>
              <td data-label={copy.members}>{group.userCount}</td>
              <td className="text-end">
                <Button
                  disabled={saving}
                  onClick={() => setGroupToDelete(group)}
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
        onCancel={() => setGroupToDelete(null)}
        onConfirm={() => {
          if (groupToDelete) void remove(groupToDelete);
          setGroupToDelete(null);
        }}
        show={groupToDelete !== null}
      />
    </>
  );
}
