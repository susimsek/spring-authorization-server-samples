"use client";

import { useCallback, useEffect, useState } from "react";
import { Badge, Button, Card, Form, ListGroup, Spinner } from "react-bootstrap";
import { useRouter } from "@/routing/navigation";

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
  const { accessToken } = useAdminAuth();
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const actualName = name;
  const [detail, setDetail] = useState<RoleDetailData | null>(null);
  const [userQuery, setUserQuery] = useState("");
  const [suggestions, setSuggestions] = useState<RoleUser[]>([]);
  const [selectedUser, setSelectedUser] = useState<RoleUser | null>(null);
  const [searchingUsers, setSearchingUsers] = useState(false);
  const { page, query, setPage, setQuery, setSize, size } = useAdminTableState();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    try {
      const roleResponse = await adminRequest<RoleDetailData>(accessToken, {
        url: `/api/admin/roles/${encodeURIComponent(actualName)}?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
      });
      if (roleResponse.status >= 300) throw new Error();
      setDetail(roleResponse.data);
      setError(false);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [accessToken, actualName, page, query, size]);

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
    if (!accessToken || !selectedUser) return;
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
  };

  const remove = async (user: RoleUser) => {
    if (!accessToken) return;
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
  };

  if (loading && !detail) return <LoadingState />;
  if (error || !detail)
    return (
      <ErrorState
        message={dictionary.admin.roles.operationError}
        retryLabel={dictionary.admin.roles.retry}
        onRetry={() => void load()}
      />
    );

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
            <Button disabled={!selectedUser} onClick={() => void assign()}>
              <AdminActionIcon action="assign" />
              {dictionary.admin.roles.assign}
            </Button>
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
                <Button size="sm" variant="outline-danger" onClick={() => void remove(user)}>
                  <AdminActionIcon action="remove" />
                  {dictionary.admin.roles.remove}
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </div>
  );
}
