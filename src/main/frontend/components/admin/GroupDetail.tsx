"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Badge, Button, Card, Form, ListGroup, Spinner } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useRouter } from "@/routing/navigation";
import { z } from "zod";

import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import type { PageResponse } from "@/lib/api-types";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { AdminBreadcrumb } from "./AdminBreadcrumb";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { useAdminTableState } from "./useAdminTableState";

type User = { id: number; username: string; enabled: boolean };
type Group = {
  id: number;
  name: string;
  parentId: number | null;
  path: string;
  roles: string[];
  userCount: number;
};
type Role = { name: string };

export function GroupDetail({
  dictionary,
  id,
}: {
  locale: Locale;
  dictionary: Dictionary;
  id: string;
}) {
  const { access, accessToken } = useAdminAuth();
  const canManageUsers = Boolean(access?.manageUsers);
  const canManageRoles = Boolean(access?.manageRoles);
  const alerts = useConsoleAlerts();
  const router = useRouter();
  const groupId = id;
  const copy = dictionary.admin.groups;
  const [group, setGroup] = useState<Group | null>(null);
  const groupFormInitialized = useRef(false);
  const [roles, setRoles] = useState<Role[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [members, setMembers] = useState<User[]>([]);
  const [memberTotalPages, setMemberTotalPages] = useState(0);
  const [memberTotalElements, setMemberTotalElements] = useState(0);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [userQuery, setUserQuery] = useState("");
  const [suggestions, setSuggestions] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const {
    page: memberPage,
    query: memberQuery,
    setPage: setMemberPage,
    setQuery: setMemberQuery,
    setSize: setMemberSize,
    size: memberSize,
  } = useAdminTableState();
  const groupSettingsSchema = z.object({
    name: z
      .string()
      .trim()
      .min(1, dictionary.admin.common.validation.required)
      .max(100, dictionary.admin.common.validation.max100),
    parentId: z.string(),
  });
  const {
    register: registerGroupSettings,
    handleSubmit: handleGroupSettingsSubmit,
    reset: resetGroupSettings,
    formState: { errors: groupSettingsErrors, isDirty: isGroupSettingsDirty },
  } = useForm<z.infer<typeof groupSettingsSchema>>({
    resolver: zodResolver(groupSettingsSchema),
    defaultValues: { name: "", parentId: "" },
  });

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    try {
      const [groupResponse, rolesResponse, groupsResponse, membersResponse] = await Promise.all([
        adminRequest<Group>(accessToken, {
          url: `/api/admin/groups/${encodeURIComponent(groupId)}`,
        }),
        adminRequest<PageResponse<Role>>(accessToken, { url: "/api/admin/roles?page=0&size=100" }),
        adminRequest<PageResponse<Group>>(accessToken, {
          url: "/api/admin/groups?page=0&size=100",
        }),
        adminRequest<PageResponse<User>>(accessToken, {
          url: `/api/admin/groups/${encodeURIComponent(groupId)}/users?q=${encodeURIComponent(memberQuery)}&page=${memberPage}&size=${memberSize}`,
        }),
      ]);
      if (
        groupResponse.status >= 300 ||
        rolesResponse.status >= 300 ||
        groupsResponse.status >= 300 ||
        membersResponse.status >= 300
      ) {
        throw new Error();
      }
      setGroup(groupResponse.data);
      if (!groupFormInitialized.current) {
        resetGroupSettings({
          name: groupResponse.data.name,
          parentId: groupResponse.data.parentId?.toString() ?? "",
        });
        groupFormInitialized.current = true;
      }
      setRoles(rolesResponse.data.content);
      setGroups(groupsResponse.data.content);
      setSelectedRoles(groupResponse.data.roles);
      setMembers(membersResponse.data.content);
      setMemberTotalPages(membersResponse.data.totalPages);
      setMemberTotalElements(membersResponse.data.totalElements);
      setError(false);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [accessToken, groupId, memberPage, memberQuery, memberSize, resetGroupSettings]);

  useEffect(() => {
    const timeout = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timeout);
  }, [load]);

  useEffect(() => {
    if (!accessToken || userQuery.trim().length < 2 || selectedUser) return;
    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      adminRequest<PageResponse<User>>(accessToken, {
        url: `/api/admin/groups/${encodeURIComponent(groupId)}/available-users?q=${encodeURIComponent(userQuery.trim())}&page=0&size=10`,
        signal: controller.signal,
      })
        .then((response) => setSuggestions(response.status < 300 ? response.data.content : []))
        .catch(() => setSuggestions([]));
    }, 300);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [accessToken, groupId, selectedUser, userQuery]);

  const saveRoles = async () => {
    if (!canManageRoles || !accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest<Group>(accessToken, {
        url: `/api/admin/groups/${encodeURIComponent(groupId)}/roles`,
        method: "PUT",
        data: { roles: selectedRoles },
      });
      if (response.status >= 300) throw new Error();
      setGroup(response.data);
      alerts.addAlert(copy.mappingSaved);
    } catch {
      alerts.addError(copy.operationError);
    } finally {
      setSaving(false);
    }
  };

  const saveSettings = async ({ name, parentId }: z.infer<typeof groupSettingsSchema>) => {
    if (!canManageUsers || !accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest<Group>(accessToken, {
        url: `/api/admin/groups/${encodeURIComponent(groupId)}`,
        method: "PUT",
        data: { name, parentId: parentId ? Number(parentId) : null },
      });
      if (response.status >= 300) throw new Error();
      setGroup(response.data);
      resetGroupSettings({
        name: response.data.name,
        parentId: response.data.parentId?.toString() ?? "",
      });
      alerts.addAlert(copy.groupUpdated);
    } catch {
      alerts.addError(copy.operationError);
    } finally {
      setSaving(false);
    }
  };

  const addMember = async () => {
    if (!canManageUsers || !accessToken || !selectedUser) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/groups/${encodeURIComponent(groupId)}/users`,
        method: "POST",
        data: { userId: selectedUser.id },
      });
      if (response.status >= 300) throw new Error();
      alerts.addAlert(copy.memberAdded);
      setSelectedUser(null);
      setUserQuery("");
      setSuggestions([]);
      await load();
    } catch {
      alerts.addError(copy.operationError);
    } finally {
      setSaving(false);
    }
  };

  const removeMember = async (user: User) => {
    if (!canManageUsers || !accessToken) return;
    setSaving(true);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/groups/${encodeURIComponent(groupId)}/users/${user.id}`,
        method: "DELETE",
      });
      if (response.status >= 300) throw new Error();
      alerts.addAlert(copy.memberRemoved);
      if (members.length === 1 && memberPage > 0) setMemberPage(memberPage - 1);
      else await load();
    } catch {
      alerts.addError(copy.operationError);
    } finally {
      setSaving(false);
    }
  };

  if (loading && !group) return <LoadingState />;
  if (error || !group) return <ErrorState message={copy.operationError} />;

  return (
    <div className="d-grid gap-4">
      <AdminBreadcrumb
        items={[{ label: copy.title, href: `/admin/groups` }, { label: group.name }]}
      />
      <div className="admin-detail-heading">
        <div>
          <h1 className="h3 mb-1">{group.name}</h1>
          <div className="text-body-secondary">
            {group.userCount} {copy.members.toLowerCase()}
          </div>
        </div>
      </div>
      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5 mb-1">{copy.settings}</h2>
          <p className="small text-body-secondary mb-3">{copy.renameHelp}</p>
          <Form onSubmit={handleGroupSettingsSubmit(saveSettings)}>
            <Form.Group className="mb-3" controlId="group-name">
              <Form.Label>{copy.name}</Form.Label>
              <Form.Control
                isInvalid={Boolean(groupSettingsErrors.name)}
                maxLength={100}
                disabled={!canManageUsers}
                {...registerGroupSettings("name")}
              />
              <Form.Control.Feedback type="invalid">
                {groupSettingsErrors.name?.message}
              </Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-3" controlId="group-parent">
              <Form.Label>{copy.parent}</Form.Label>
              <Form.Select disabled={!canManageUsers} {...registerGroupSettings("parentId")}>
                <option value="">{copy.rootGroup}</option>
                {groups
                  .filter((candidate) => candidate.id !== group.id)
                  .map((candidate) => (
                    <option key={candidate.id} value={candidate.id}>
                      {candidate.path}
                    </option>
                  ))}
              </Form.Select>
              <Form.Text>{copy.parentHelp}</Form.Text>
            </Form.Group>
            <div className="admin-form-actions">
              {canManageUsers && (
                <Button disabled={saving || !isGroupSettingsDirty} type="submit">
                  {saving ? (
                    <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                  ) : (
                    <AdminActionIcon action="save" />
                  )}
                  {dictionary.admin.common.save}
                </Button>
              )}
            </div>
          </Form>
        </Card.Body>
      </Card>
      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5">{copy.roleMappings}</h2>
          <div className="d-flex flex-wrap gap-2 mb-3">
            {roles.map((role) => (
              <Form.Check
                key={role.name}
                checked={selectedRoles.includes(role.name)}
                id={`role-${role.name}`}
                label={role.name}
                type="checkbox"
                disabled={!canManageRoles}
                onChange={() =>
                  setSelectedRoles((current) =>
                    current.includes(role.name)
                      ? current.filter((value) => value !== role.name)
                      : [...current, role.name],
                  )
                }
              />
            ))}
          </div>
          <div className="admin-form-actions">
            {canManageRoles && (
              <Button disabled={saving} onClick={() => void saveRoles()}>
                {saving ? (
                  <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                ) : (
                  <AdminActionIcon action="save" />
                )}
                {copy.saveMappings}
              </Button>
            )}
          </div>
        </Card.Body>
      </Card>
      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5">{copy.assignUser}</h2>
          <div className="d-flex flex-wrap align-items-start gap-2">
            <div className="position-relative flex-grow-1" style={{ maxWidth: "28rem" }}>
              <Form.Control
                aria-label={copy.assignUser}
                placeholder={dictionary.admin.resources.search}
                value={selectedUser?.username ?? userQuery}
                disabled={!canManageUsers}
                onChange={(event) => {
                  setSelectedUser(null);
                  setSuggestions([]);
                  setUserQuery(event.target.value);
                }}
              />
              {!selectedUser && suggestions.length > 0 && (
                <ListGroup className="position-absolute start-0 end-0 mt-1 shadow-sm z-3">
                  {suggestions.map((user) => (
                    <ListGroup.Item action key={user.id} onClick={() => setSelectedUser(user)}>
                      {user.username}
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              )}
            </div>
            {canManageUsers && (
              <Button disabled={!selectedUser || saving} onClick={() => void addMember()}>
                {saving ? (
                  <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                ) : (
                  <AdminActionIcon action="assign" />
                )}
                {copy.assignUser}
              </Button>
            )}
          </div>
        </Card.Body>
      </Card>
      <ResourceFilters
        query={memberQuery}
        searchLabel={copy.searchMembers}
        onQueryChange={setMemberQuery}
      />
      <DataTable
        isEmpty={members.length === 0}
        emptyMessage={dictionary.admin.resources.empty}
        footer={
          memberTotalElements > 0 ? (
            <PaginationControls
              first={dictionary.admin.resources.first}
              last={dictionary.admin.resources.last}
              next={dictionary.admin.resources.next}
              onPageChange={setMemberPage}
              onSizeChange={setMemberSize}
              page={memberPage}
              pageLabel={dictionary.admin.resources.page}
              previous={dictionary.admin.resources.previous}
              rowsPerPage={dictionary.admin.resources.rowsPerPage}
              size={memberSize}
              totalElements={memberTotalElements}
              totalPages={memberTotalPages}
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
          {members.map((user) => (
            <tr key={user.id}>
              <td>
                <Button
                  className="p-0 text-decoration-none"
                  variant="link"
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
                {canManageUsers && (
                  <Button
                    disabled={saving}
                    size="sm"
                    variant="danger"
                    onClick={() => void removeMember(user)}
                  >
                    {saving ? (
                      <Spinner animation="border" aria-hidden="true" className="me-2" size="sm" />
                    ) : (
                      <AdminActionIcon action="remove" />
                    )}
                    {copy.removeUser}
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
