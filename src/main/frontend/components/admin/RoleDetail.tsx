"use client";

import { useCallback, useEffect, useState } from "react";
import { Badge, Button, Card, Form, ListGroup, Spinner } from "react-bootstrap";
import { useRouter } from "@/routing/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "@/lib/form";
import { z } from "zod";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import type { PageResponse } from "@/lib/api-types";
import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { AdminBreadcrumb } from "./AdminBreadcrumb";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { useAdminTableState } from "./useAdminTableState";

type RoleUser = { id: number; username: string; enabled: boolean };
type RoleDetailData = {
  name: string;
  description?: string | null;
  userCount: number;
  protectedRole: boolean;
  users: PageResponse<RoleUser>;
};

export function RoleDetail({
  dictionary,
  name,
}: {
  locale: Locale;
  dictionary: Dictionary;
  name: string;
}) {
  const { access, accessToken } = useAdminAuth();
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const actualName = name;
  const [detail, setDetail] = useState<RoleDetailData | null>(null);
  const [userQuery, setUserQuery] = useState("");
  const [suggestions, setSuggestions] = useState<RoleUser[]>([]);
  const [selectedUser, setSelectedUser] = useState<RoleUser | null>(null);
  const [searchingUsers, setSearchingUsers] = useState(false);
  const [saving, setSaving] = useState(false);
  const { page, query, setPage, setQuery, setSize, size } = useAdminTableState();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const descriptionSchema = z.object({
    description: z.string().trim().max(500, dictionary.admin.common.validation.max500),
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors: descriptionErrors, isSubmitting: descriptionSaving },
  } = useForm<z.infer<typeof descriptionSchema>>({
    resolver: zodResolver(descriptionSchema),
    mode: "onBlur",
    defaultValues: { description: "" },
  });

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    try {
      const roleResponse = await adminRequest<RoleDetailData>(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
      });
      if (roleResponse.status >= 300) throw new Error();
      setDetail(roleResponse.data);
      reset({ description: roleResponse.data.description ?? "" });
      setError(false);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [accessToken, actualName, page, query, reset, size]);

  const saveDescription = async ({ description }: z.infer<typeof descriptionSchema>) => {
    if (!access?.manageRoles || !accessToken || !detail) return;
    try {
      const response = await adminRequest<RoleDetailData>(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}`,
        method: "PUT",
        data: { name: detail.name, description: description || null },
      });
      if (response.status >= 300) throw new Error();
      setDetail((current) =>
        current ? { ...current, description: response.data.description } : current,
      );
      alerts.addAlert(dictionary.admin.roles.descriptionSaved);
    } catch {
      alerts.addError(dictionary.admin.roles.descriptionSaveError);
    }
  };

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [load]);

  useEffect(() => {
    if (!accessToken || userQuery.trim().length < 2 || selectedUser) {
      return;
    }
    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      setSearchingUsers(true);
      adminRequest<PageResponse<RoleUser>>(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}/available-users?q=${encodeURIComponent(userQuery.trim())}&page=0&size=10`,
        signal: controller.signal,
      })
        .then((response) => {
          if (response.status < 300) setSuggestions(response.data.content);
        })
        .catch(() => setSuggestions([]))
        .finally(() => setSearchingUsers(false));
    }, 300);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [accessToken, actualName, selectedUser, userQuery]);

  const assign = async () => {
    if (!access?.manageRoles || !accessToken || !selectedUser) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}/users`,
        method: "POST",
        data: { userId: selectedUser.id },
      });
      if (response.status >= 300) {
        alerts.addError(dictionary.admin.roles.assignmentSaveError);
        return;
      }
      alerts.addAlert(dictionary.admin.roles.assignmentSaved);
      setSelectedUser(null);
      setUserQuery("");
      setSuggestions([]);
      await load();
    } finally {
      setSaving(false);
    }
  };

  const remove = async (user: RoleUser) => {
    if (!access?.manageRoles || !accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}/users/${user.id}?page=${page}&size=${size}`,
        method: "DELETE",
      });
      if (response.status >= 300) {
        alerts.addError(dictionary.admin.roles.assignmentRemoveError);
        return;
      }
      alerts.addAlert(dictionary.admin.roles.assignmentRemoved);
      await load();
    } finally {
      setSaving(false);
    }
  };

  if (loading && !detail) return <LoadingState />;
  if (error || !detail) return <ErrorState message={dictionary.admin.roles.operationError} />;

  return (
    <div className="d-grid gap-4">
      <AdminBreadcrumb
        items={[
          { label: dictionary.admin.roles.title, href: `/admin/roles` },
          { label: detail.name },
        ]}
      />

      <div className="admin-detail-heading">
        <div>
          <h1 className="h3 mb-1 font-monospace">{detail.name}</h1>
          <div className="text-body-secondary">
            {detail.userCount} {dictionary.admin.roles.assignedUsers}
          </div>
        </div>
        {detail.protectedRole && <Badge bg="secondary">{dictionary.admin.roles.protected}</Badge>}
      </div>

      <Card className="admin-panel-card">
        <Card.Body>
          <Form noValidate onSubmit={handleSubmit(saveDescription)}>
            <Form.Group controlId="role-description-detail">
              <Form.Label>{dictionary.admin.roles.description}</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                maxLength={500}
                disabled={!access?.manageRoles || descriptionSaving}
                isInvalid={Boolean(descriptionErrors.description)}
                {...register("description")}
              />
              <Form.Control.Feedback type="invalid">
                {descriptionErrors.description?.message}
              </Form.Control.Feedback>
              <Form.Text>{dictionary.admin.roles.descriptionHelp}</Form.Text>
            </Form.Group>
            {access?.manageRoles && (
              <Button className="mt-3" disabled={descriptionSaving} type="submit">
                {descriptionSaving ? (
                  <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                ) : (
                  <AdminActionIcon action="save" />
                )}
                {dictionary.admin.roles.saveDescription}
              </Button>
            )}
          </Form>
        </Card.Body>
      </Card>

      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5">{dictionary.admin.roles.assignUser}</h2>
          <div className="d-flex flex-wrap align-items-start gap-2">
            <div className="position-relative flex-grow-1" style={{ maxWidth: "28rem" }}>
              <Form.Control
                autoComplete="off"
                aria-autocomplete="list"
                aria-controls="role-user-suggestions"
                aria-expanded={suggestions.length > 0}
                aria-label={dictionary.admin.roles.searchUsers}
                placeholder={dictionary.admin.roles.searchUsersPlaceholder}
                role="combobox"
                value={selectedUser?.username ?? userQuery}
                onChange={(event) => {
                  setSelectedUser(null);
                  setSuggestions([]);
                  setSearchingUsers(false);
                  setUserQuery(event.target.value);
                }}
              />
              {searchingUsers && (
                <Spinner
                  animation="border"
                  size="sm"
                  className="position-absolute end-0 top-0 mt-2 me-2"
                  aria-label={dictionary.admin.roles.searchingUsers}
                />
              )}
              {!selectedUser && suggestions.length > 0 && (
                <ListGroup
                  id="role-user-suggestions"
                  className="position-absolute start-0 end-0 mt-1 shadow-sm z-3"
                >
                  {suggestions.map((user) => (
                    <ListGroup.Item
                      action
                      type="button"
                      key={user.id}
                      onClick={() => {
                        setSelectedUser(user);
                        setSuggestions([]);
                      }}
                    >
                      <div className="d-flex justify-content-between align-items-center gap-2">
                        <span>{user.username}</span>
                        <Badge bg={user.enabled ? "success" : "secondary"}>
                          {user.enabled
                            ? dictionary.admin.resources.enabled
                            : dictionary.admin.resources.disabled}
                        </Badge>
                      </div>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              )}
            </div>
            {access?.manageRoles && (
              <Button disabled={!selectedUser || saving} onClick={() => void assign()}>
                {saving ? (
                  <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                ) : (
                  <AdminActionIcon action="assign" />
                )}
                {dictionary.admin.roles.assign}
              </Button>
            )}
          </div>
          <Form.Text className="text-body-secondary">
            {dictionary.admin.roles.searchUsersHelp}
          </Form.Text>
        </Card.Body>
      </Card>

      <ResourceFilters
        query={query}
        searchLabel={dictionary.admin.roles.searchAssignedUsers}
        onQueryChange={setQuery}
      />

      <DataTable
        isEmpty={detail.users.content.length === 0}
        emptyMessage={dictionary.admin.roles.noAssignedUsers}
        footer={
          detail.users.totalElements > 0 ? (
            <PaginationControls
              page={page}
              totalPages={detail.users.totalPages}
              totalElements={detail.users.totalElements}
              size={size}
              rowsPerPage={dictionary.admin.resources.rowsPerPage}
              pageLabel={dictionary.admin.resources.page}
              previous={dictionary.admin.resources.previous}
              next={dictionary.admin.resources.next}
              first={dictionary.admin.resources.first}
              last={dictionary.admin.resources.last}
              onPageChange={setPage}
              onSizeChange={(nextSize) => {
                setPage(0);
                setSize(nextSize);
              }}
            />
          ) : undefined
        }
      >
        <thead>
          <tr>
            <th>{dictionary.admin.resources.user}</th>
            <th>{dictionary.admin.resources.status}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {detail.users.content.map((user) => (
            <tr key={user.id}>
              <td>
                <Button
                  variant="link"
                  className="p-0 text-decoration-none"
                  onClick={() => router.push(`/admin/users/${user.id}/details`)}
                >
                  {user.username}
                </Button>
              </td>
              <td>
                <Badge bg={user.enabled ? "success" : "secondary"}>
                  {user.enabled
                    ? dictionary.admin.resources.enabled
                    : dictionary.admin.resources.disabled}
                </Badge>
              </td>
              <td className="text-end">
                {access?.manageRoles && (
                  <Button
                    disabled={saving}
                    size="sm"
                    variant="danger"
                    onClick={() => void remove(user)}
                  >
                    {saving ? (
                      <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                    ) : (
                      <AdminActionIcon action="remove" />
                    )}
                    {dictionary.admin.roles.remove}
                  </Button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </div>
  );
}
