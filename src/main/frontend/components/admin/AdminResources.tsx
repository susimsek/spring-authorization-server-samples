"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Badge, Button, Form } from "react-bootstrap";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";

import { useAdminAuth } from "./AdminAuthProvider";
import { ConfirmModal } from "./ConfirmModal";
import { AdminPageHeader } from "./AdminPageHeader";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { ResultModal } from "./ResultModal";
import { useAdminTableState } from "./useAdminTableState";

type User = {
  id: number;
  username: string;
  enabled: boolean;
  avatarUrl: string | null;
  authorities: string[];
};
type Session = {
  id: string;
  username: string | null;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
  authorizationCount: number;
};
type Consent = {
  clientId: string;
  clientName: string;
  principalName: string;
  authorities: string[];
};
type Key = {
  id: string;
  kid: string;
  type: string;
  algorithm: string;
  use: string;
  active: boolean;
  createdAt: string;
};

type Resource = "users" | "sessions" | "consents" | "keys";
type Copy = Dictionary["admin"]["resources"];
type PageData<T> = { content: T[]; number: number; totalPages: number; totalElements: number };

export function AdminResources({
  resource,
  copy,
  locale,
}: {
  resource: Resource;
  copy: Copy;
  locale?: Locale;
}) {
  return <AdminResourcesContent copy={copy} key={resource} locale={locale} resource={resource} />;
}

function AdminResourcesContent({
  resource,
  copy,
  locale,
}: {
  resource: Resource;
  copy: Copy;
  locale?: Locale;
}) {
  const { access, accessToken } = useAdminAuth();
  const [items, setItems] = useState<User[] | Session[] | Consent[] | Key[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [result, setResult] = useState<{ title: string; message: string; value?: string } | null>(
    null,
  );
  const { page, query, setPage, setQuery, setSize, setStatus, size, status } = useAdminTableState(
    20,
    resource === "users" || resource === "keys",
  );

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<PageData<User | Session | Consent | Key>>(accessToken, {
      url: `/api/admin/${resource}?q=${encodeURIComponent(query)}&page=${page}&size=${size}${resource === "users" ? `&enabled=${status}` : resource === "keys" ? `&active=${status}` : ""}`,
    })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setError(false);
        setItems(response.data.content as User[] | Session[] | Consent[] | Key[]);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, reloadVersion, resource, size, status]);

  const request = async <T,>(
    url: string,
    method: "DELETE" | "POST" | "PUT",
    data?: unknown,
  ): Promise<T | undefined> => {
    if (!accessToken) return undefined;
    setLoading(true);
    try {
      const response = await adminRequest(accessToken, { url, method, data });
      if (response.status >= 300) {
        throw new Error("Administration operation failed");
      }
      setReloadVersion((current) => current + 1);
      return response.data as T;
    } catch {
      setError(true);
      setLoading(false);
      return undefined;
    }
  };

  const rotateKey = async () => {
    const key = await request<Key>("/api/admin/keys/rotate", "POST");
    if (!key) return;
    setResult({
      title: copy.keyRotatedTitle,
      message: copy.keyRotatedHelp,
      value: key.kid,
    });
  };

  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={copy.operationError} />;

  return (
    <>
      <AdminPageHeader
        title={copy[resource]}
        description={copy.description}
        actions={
          <>
            {resource === "users" && access?.manageUsers && locale && (
              <Link className="btn btn-primary" href={`/${locale}/admin/users/new`}>
                {copy.createUser}
              </Link>
            )}
            {resource === "keys" && access?.manageKeys && (
              <Button variant="primary" onClick={() => void rotateKey()}>
                {copy.rotateKey}
              </Button>
            )}
          </>
        }
      />
      <ResourceFilters
        key={query}
        query={query}
        searchLabel={copy.search}
        onQueryChange={(value) => {
          setLoading(true);
          setQuery(value);
        }}
      >
        {(resource === "users" || resource === "keys") && (
          <Form.Select
            value={status}
            onChange={(event) => {
              setLoading(true);
              setStatus(event.target.value);
            }}
            style={{ maxWidth: "10rem" }}
          >
            <option value="">{copy.all}</option>
            <option value="true">{resource === "users" ? copy.enabled : copy.active}</option>
            <option value="false">{resource === "users" ? copy.disabled : copy.passive}</option>
          </Form.Select>
        )}
        <Form.Select
          value={size}
          onChange={(event) => {
            setLoading(true);
            setSize(Number(event.target.value));
          }}
          style={{ maxWidth: "6rem" }}
        >
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </Form.Select>
      </ResourceFilters>
      <DataTable isEmpty={items.length === 0} emptyMessage={copy.empty}>
        {resource === "users" && (
          <UsersTable
            items={items as User[]}
            request={request}
            copy={copy}
            canManage={access?.manageUsers ?? false}
            locale={locale}
          />
        )}
        {resource === "sessions" && (
          <SessionsTable
            items={items as Session[]}
            request={request}
            copy={copy}
            canManage={access?.manageSessions ?? false}
          />
        )}
        {resource === "consents" && (
          <ConsentsTable
            items={items as Consent[]}
            request={request}
            copy={copy}
            canManage={access?.manageConsents ?? false}
          />
        )}
        {resource === "keys" && <KeysTable items={items as Key[]} copy={copy} />}
      </DataTable>
      {totalPages > 0 && (
        <div className="d-flex justify-content-end align-items-center gap-2 mt-3">
          <span className="small text-body-secondary">
            {totalElements} {copy.records} · {copy.page} {page + 1} {copy.of} {totalPages}
          </span>
          <PaginationControls
            page={page}
            totalPages={totalPages}
            previous={copy.previous}
            next={copy.next}
            onPageChange={(nextPage) => {
              setLoading(true);
              setPage(nextPage);
            }}
          />
        </div>
      )}
      <ResultModal
        closeLabel={copy.cancel}
        message={result?.message ?? ""}
        onClose={() => setResult(null)}
        show={result !== null}
        title={result?.title ?? ""}
        value={result?.value}
      />
    </>
  );
}

function UsersTable({
  items,
  request,
  copy,
  canManage,
  locale,
}: {
  items: User[];
  request: AdminRequest;
  copy: Copy;
  canManage: boolean;
  locale?: Locale;
}) {
  const [userToDelete, setUserToDelete] = useState<User | null>(null);
  return (
    <>
      <thead>
        <tr>
          <th>{copy.username}</th>
          <th>{copy.roles}</th>
          <th>{copy.status}</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {items.map((user) => (
          <tr key={user.id}>
            <td data-label={copy.username}>
              <div className="d-flex align-items-center gap-2">
                {user.avatarUrl ? (
                  <Image
                    alt=""
                    className="rounded-circle object-fit-cover"
                    height={32}
                    src={user.avatarUrl}
                    unoptimized
                    width={32}
                  />
                ) : (
                  <span className="avatar-placeholder">
                    {user.username.slice(0, 1).toUpperCase()}
                  </span>
                )}
                {user.username}
              </div>
            </td>
            <td data-label={copy.roles}>
              {user.authorities.map((role) => (
                <Badge bg="secondary" className="me-1" key={role}>
                  {role}
                </Badge>
              ))}
            </td>
            <td data-label={copy.status}>
              <Badge bg={user.enabled ? "success" : "secondary"}>
                {user.enabled ? copy.enabled : copy.disabled}
              </Badge>
            </td>
            {canManage && (
              <td className="text-end">
                {locale && (
                  <Link
                    className="btn btn-sm btn-outline-primary me-2"
                    href={`/${locale}/admin/users/edit?id=${user.id}`}
                  >
                    {copy.edit}
                  </Link>
                )}
                <Button
                  size="sm"
                  variant="outline-secondary"
                  onClick={() =>
                    void request(`/api/admin/users/${user.id}/enabled`, "PUT", {
                      enabled: !user.enabled,
                    })
                  }
                >
                  {user.enabled ? copy.disable : copy.enable}
                </Button>
                <Button
                  size="sm"
                  variant="outline-danger"
                  className="ms-2"
                  onClick={() => setUserToDelete(user)}
                >
                  {copy.delete}
                </Button>
              </td>
            )}
          </tr>
        ))}
      </tbody>
      <ConfirmModal
        cancelLabel={copy.cancel}
        confirmLabel={copy.delete}
        message={copy.deleteUserConfirm}
        onCancel={() => setUserToDelete(null)}
        onConfirm={() => {
          if (userToDelete) void request(`/api/admin/users/${userToDelete.id}`, "DELETE");
          setUserToDelete(null);
        }}
        show={userToDelete !== null}
      />
    </>
  );
}

function SessionsTable({
  items,
  request,
  copy,
  canManage,
}: {
  items: Session[];
  request: AdminRequest;
  copy: Copy;
  canManage: boolean;
}) {
  const [sessionAction, setSessionAction] = useState<{
    url: string;
    label: string;
    message: string;
  } | null>(null);
  return (
    <>
      <thead>
        <tr>
          <th>{copy.user}</th>
          <th>{copy.created}</th>
          <th>{copy.lastActive}</th>
          <th>{copy.expires}</th>
          <th>{copy.authorizations}</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {items.map((session) => (
          <tr key={session.id}>
            <td data-label={copy.user}>{session.username ?? "-"}</td>
            <td data-label={copy.created}>{date(session.createdAt)}</td>
            <td data-label={copy.lastActive}>{date(session.lastAccessedAt)}</td>
            <td data-label={copy.expires}>{date(session.expiresAt)}</td>
            <td data-label={copy.authorizations}>{session.authorizationCount}</td>
            {canManage && (
              <td className="text-end">
                <Button
                  size="sm"
                  variant="outline-danger"
                  onClick={() =>
                    setSessionAction({
                      url: `/api/admin/sessions/${encodeURIComponent(session.id)}`,
                      label: copy.signOut,
                      message: copy.signOutConfirm,
                    })
                  }
                >
                  {copy.signOut}
                </Button>
                {session.username && (
                  <Button
                    size="sm"
                    variant="outline-danger"
                    className="ms-2"
                    onClick={() =>
                      setSessionAction({
                        url: `/api/admin/users/${encodeURIComponent(session.username ?? "")}/sessions`,
                        label: copy.signOutAll,
                        message: copy.signOutAllConfirm,
                      })
                    }
                  >
                    {copy.signOutAll}
                  </Button>
                )}
              </td>
            )}
          </tr>
        ))}
      </tbody>
      <ConfirmModal
        cancelLabel={copy.cancel}
        confirmLabel={sessionAction?.label ?? copy.signOut}
        message={sessionAction?.message ?? ""}
        onCancel={() => setSessionAction(null)}
        onConfirm={() => {
          if (sessionAction) void request(sessionAction.url, "DELETE");
          setSessionAction(null);
        }}
        show={sessionAction !== null}
      />
    </>
  );
}

function ConsentsTable({
  items,
  request,
  copy,
  canManage,
}: {
  items: Consent[];
  request: AdminRequest;
  copy: Copy;
  canManage: boolean;
}) {
  const [consentToRevoke, setConsentToRevoke] = useState<Consent | null>(null);
  return (
    <>
      <thead>
        <tr>
          <th>{copy.user}</th>
          <th>{copy.client}</th>
          <th>{copy.grantedScopes}</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {items.map((consent) => (
          <tr key={`${consent.clientId}-${consent.principalName}`}>
            <td data-label={copy.user}>{consent.principalName}</td>
            <td data-label={copy.client}>{consent.clientName}</td>
            <td data-label={copy.grantedScopes}>
              {consent.authorities.map((scope) => (
                <Badge bg="light" text="dark" className="border me-1" key={scope}>
                  {scope.replace("SCOPE_", "")}
                </Badge>
              ))}
            </td>
            {canManage && (
              <td className="text-end">
                <Button
                  size="sm"
                  variant="outline-danger"
                  onClick={() => setConsentToRevoke(consent)}
                >
                  {copy.revoke}
                </Button>
              </td>
            )}
          </tr>
        ))}
      </tbody>
      <ConfirmModal
        cancelLabel={copy.cancel}
        confirmLabel={copy.revoke}
        message={copy.revokeConfirm}
        onCancel={() => setConsentToRevoke(null)}
        onConfirm={() => {
          if (consentToRevoke) {
            void request(
              `/api/admin/consents/${encodeURIComponent(consentToRevoke.clientId)}/${encodeURIComponent(consentToRevoke.principalName)}`,
              "DELETE",
            );
          }
          setConsentToRevoke(null);
        }}
        show={consentToRevoke !== null}
      />
    </>
  );
}

function KeysTable({ items, copy }: { items: Key[]; copy: Copy }) {
  return (
    <>
      <thead>
        <tr>
          <th>{copy.keyId}</th>
          <th>{copy.type}</th>
          <th>{copy.algorithm}</th>
          <th>{copy.status}</th>
          <th>{copy.created}</th>
        </tr>
      </thead>
      <tbody>
        {items.map((key) => (
          <tr key={key.id}>
            <td className="font-monospace small" data-label={copy.keyId}>
              {key.kid}
            </td>
            <td data-label={copy.type}>{key.type}</td>
            <td data-label={copy.algorithm}>{key.algorithm}</td>
            <td data-label={copy.status}>
              <Badge bg={key.active ? "success" : "secondary"}>
                {key.active ? copy.active : copy.passive}
              </Badge>
            </td>
            <td data-label={copy.created}>{date(key.createdAt)}</td>
          </tr>
        ))}
      </tbody>
    </>
  );
}

type AdminRequest = <T>(
  url: string,
  method: "DELETE" | "POST" | "PUT",
  data?: unknown,
) => Promise<T | undefined>;
const date = (value: string) => new Date(value).toLocaleString();
