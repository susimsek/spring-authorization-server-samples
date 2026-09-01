"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Badge, Button, Dropdown, Form, Modal } from "react-bootstrap";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import type { PageResponse } from "@/lib/api-types";
import { encodeConsentRouteKey } from "@/lib/consent-route";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { ConfirmModal } from "./ConfirmModal";
import { AdminPageHeader } from "./AdminPageHeader";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { ResultModal } from "./ResultModal";
import { useAdminTableState } from "./useAdminTableState";
import { RowActions } from "./RowActions";

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
  active: boolean;
};
type SessionAuthorization = {
  id: string;
  clientId: string;
  clientName: string;
  grantType: string;
  scopes: string[];
  accessTokenIssuedAt: string | null;
  accessTokenExpiresAt: string | null;
  refreshTokenExpiresAt: string | null;
};
type SessionDetail = { session: Session; authorizations: SessionAuthorization[] };
type Consent = {
  clientId: string;
  clientName: string;
  principalName: string;
  userId: number | null;
  authorities: string[];
  createdAt: string;
  updatedAt: string;
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
  const {
    clientId,
    clearFilters,
    page,
    query,
    setClientId,
    setPage,
    setQuery,
    setSize,
    setSort,
    setStatus,
    setUsername,
    setScope,
    size,
    sort,
    status,
    username,
    scope,
  } = useAdminTableState(
    20,
    resource === "users" || resource === "keys" || resource === "sessions",
    resource === "sessions"
      ? "lastAccessTime,desc"
      : resource === "consents"
        ? "id.principalName,asc"
        : resource === "keys"
          ? "createdAt,desc"
          : "username,asc",
  );

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<PageResponse<User | Session | Consent | Key>>(accessToken, {
      url: `/api/admin/${resource}?q=${encodeURIComponent(query)}&page=${page}&size=${size}&sort=${encodeURIComponent(sort)}${resource === "users" ? `&enabled=${status}` : resource === "keys" ? `&active=${status}` : resource === "sessions" ? `&status=${status || "active"}&clientId=${encodeURIComponent(clientId)}` : resource === "consents" ? `&clientId=${encodeURIComponent(clientId)}&username=${encodeURIComponent(username)}&scope=${encodeURIComponent(scope)}` : ""}`,
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
  }, [
    accessToken,
    clientId,
    scope,
    username,
    page,
    query,
    reloadVersion,
    resource,
    size,
    sort,
    status,
  ]);

  const activeFilters = [
    query && { label: copy.search, value: query, onRemove: () => setQuery("") },
    status && {
      label: copy.status,
      value: status === "true" ? copy.active : status === "false" ? copy.passive : status,
      onRemove: () => setStatus(""),
    },
    clientId && { label: copy.client, value: clientId, onRemove: () => setClientId("") },
    username && { label: copy.user, value: username, onRemove: () => setUsername("") },
    scope && { label: copy.scope, value: scope, onRemove: () => setScope("") },
  ].filter(Boolean) as { label: string; value: string; onRemove: () => void }[];
  const sortOptions =
    resource === "sessions"
      ? [
          { value: "lastAccessTime,desc", label: copy.newest },
          { value: "lastAccessTime,asc", label: copy.oldest },
        ]
      : resource === "consents"
        ? [
            { value: "id.principalName,asc", label: `${copy.user} · ${copy.ascending}` },
            { value: "id.principalName,desc", label: `${copy.user} · ${copy.descending}` },
          ]
        : resource === "keys"
          ? [
              { value: "createdAt,desc", label: copy.newest },
              { value: "createdAt,asc", label: copy.oldest },
            ]
          : [
              { value: "username,asc", label: `${copy.username} · ${copy.ascending}` },
              { value: "username,desc", label: `${copy.username} · ${copy.descending}` },
            ];

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
  if (error)
    return (
      <ErrorState
        message={copy.operationError}
        onRetry={() => {
          setError(false);
          setLoading(true);
          setReloadVersion((current) => current + 1);
        }}
      />
    );

  return (
    <>
      <AdminPageHeader
        title={copy[resource]}
        description={copy.description}
        actions={
          <>
            {resource === "users" && access?.manageUsers && locale && (
              <Link className="btn btn-primary" href={`/${locale}/admin/users/new`}>
                <AdminActionIcon action="add" />
                {copy.createUser}
              </Link>
            )}
            {resource === "keys" && access?.manageKeys && (
              <Button variant="primary" onClick={() => void rotateKey()}>
                <AdminActionIcon action="rotate" />
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
        sort={{ label: copy.sort, value: sort, options: sortOptions, onChange: setSort }}
        activeFilters={activeFilters}
        clearFiltersLabel={copy.clearFilters}
        onClearFilters={() => {
          setLoading(true);
          clearFilters();
        }}
        resultCount={totalElements}
        recordsLabel={copy.records}
        onQueryChange={(value) => {
          setLoading(true);
          setQuery(value);
        }}
      >
        {resource === "sessions" && (
          <>
            <Form.Control
              aria-label={copy.clientId}
              className="admin-resource-filter-control"
              placeholder={copy.clientId}
              value={clientId}
              onChange={(event) => {
                setLoading(true);
                setClientId(event.target.value);
              }}
            />
            <Form.Select
              aria-label={copy.sessionStatus}
              className="admin-resource-filter-control"
              value={status || "active"}
              onChange={(event) => {
                setLoading(true);
                setStatus(event.target.value);
              }}
            >
              <option value="active">{copy.active}</option>
              <option value="expired">{copy.expired}</option>
              <option value="all">{copy.all}</option>
            </Form.Select>
          </>
        )}
        {resource === "consents" && (
          <>
            <Form.Control
              aria-label={copy.clientId}
              className="admin-resource-filter-control"
              placeholder={copy.clientId}
              value={clientId}
              onChange={(event) => {
                setLoading(true);
                setClientId(event.target.value);
              }}
            />
            <Form.Control
              aria-label={copy.user}
              className="admin-resource-filter-control"
              placeholder={copy.user}
              value={username}
              onChange={(event) => {
                setLoading(true);
                setUsername(event.target.value);
              }}
            />
            <Form.Control
              aria-label={copy.grantedScopes}
              className="admin-resource-filter-control"
              placeholder={copy.grantedScopes}
              value={scope}
              onChange={(event) => {
                setLoading(true);
                setScope(event.target.value);
              }}
            />
          </>
        )}
        {(resource === "users" || resource === "keys") && (
          <Form.Select
            className="admin-resource-filter-control"
            value={status}
            onChange={(event) => {
              setLoading(true);
              setStatus(event.target.value);
            }}
          >
            <option value="">{copy.all}</option>
            <option value="true">{resource === "users" ? copy.enabled : copy.active}</option>
            <option value="false">{resource === "users" ? copy.disabled : copy.passive}</option>
          </Form.Select>
        )}
      </ResourceFilters>
      <DataTable
        isEmpty={items.length === 0}
        emptyMessage={copy.empty}
        footer={
          totalElements > 0 ? (
            <PaginationControls
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              size={size}
              rowsPerPage={copy.rowsPerPage}
              pageLabel={copy.page}
              previous={copy.previous}
              next={copy.next}
              first={copy.first}
              last={copy.last}
              onPageChange={(nextPage) => {
                setLoading(true);
                setPage(nextPage);
              }}
              onSizeChange={(nextSize) => {
                setLoading(true);
                setSize(nextSize);
              }}
            />
          ) : undefined
        }
      >
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
            accessToken={accessToken}
          />
        )}
        {resource === "consents" && (
          <ConsentsTable
            items={items as Consent[]}
            request={request}
            copy={copy}
            canManage={access?.manageConsents ?? false}
            locale={locale}
          />
        )}
        {resource === "keys" && <KeysTable items={items as Key[]} copy={copy} />}
      </DataTable>
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
                <RowActions label={`${user.username} actions`}>
                  {locale && (
                    <Dropdown.Item
                      as={Link}
                      href={`/${locale}/admin/users/${encodeURIComponent(String(user.id))}/details`}
                    >
                      <AdminActionIcon action="edit" />
                      {copy.edit}
                    </Dropdown.Item>
                  )}
                  <Dropdown.Item
                    onClick={() =>
                      void request(`/api/admin/users/${user.id}/enabled`, "PUT", {
                        enabled: !user.enabled,
                      })
                    }
                  >
                    {user.enabled ? copy.disable : copy.enable}
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  <Dropdown.Item className="text-danger" onClick={() => setUserToDelete(user)}>
                    <AdminActionIcon action="delete" />
                    {copy.delete}
                  </Dropdown.Item>
                </RowActions>
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
  accessToken,
}: {
  items: Session[];
  request: AdminRequest;
  copy: Copy;
  canManage: boolean;
  accessToken: string | null;
}) {
  const [sessionAction, setSessionAction] = useState<{
    url: string;
    label: string;
    message: string;
  } | null>(null);
  const [detail, setDetail] = useState<SessionDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const showDetail = async (id: string) => {
    if (!accessToken) return;
    setDetailLoading(true);
    try {
      const response = await adminRequest<SessionDetail>(accessToken, {
        url: `/api/admin/sessions/${encodeURIComponent(id)}`,
      });
      if (response.status < 300) setDetail(response.data);
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <>
      <thead>
        <tr>
          <th>{copy.sessionId}</th>
          <th>{copy.user}</th>
          <th>{copy.created}</th>
          <th>{copy.lastActive}</th>
          <th>{copy.expires}</th>
          <th>{copy.authorizations}</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {items.map((session) => {
          const expired = !session.active;
          return (
            <tr key={session.id}>
              <td className="font-monospace small" data-label={copy.sessionId}>
                <Button
                  variant="link"
                  className="font-monospace p-0 text-decoration-none"
                  onClick={() => void showDetail(session.id)}
                >
                  {session.id.slice(0, 12)}…
                </Button>
              </td>
              <td data-label={copy.user}>
                <div className="fw-medium">{session.username ?? "-"}</div>
                <Badge bg={expired ? "secondary" : "success"}>
                  {expired ? copy.expired : copy.active}
                </Badge>
              </td>
              <td data-label={copy.created}>{date(session.createdAt)}</td>
              <td data-label={copy.lastActive}>{date(session.lastAccessedAt)}</td>
              <td data-label={copy.expires}>{date(session.expiresAt)}</td>
              <td data-label={copy.authorizations}>{session.authorizationCount}</td>
              <td className="text-end">
                <RowActions label={copy.sessionActions}>
                  <Dropdown.Item onClick={() => void showDetail(session.id)}>
                    View details
                  </Dropdown.Item>
                  {canManage && !expired && (
                    <>
                      <Dropdown.Divider />
                      <Dropdown.Item
                        className="text-danger"
                        onClick={() =>
                          setSessionAction({
                            url: `/api/admin/sessions/${encodeURIComponent(session.id)}`,
                            label: copy.signOut,
                            message: copy.signOutConfirm,
                          })
                        }
                      >
                        <AdminActionIcon action="remove" />
                        {copy.signOut}
                      </Dropdown.Item>
                      {session.username && (
                        <Dropdown.Item
                          className="text-danger"
                          onClick={() =>
                            setSessionAction({
                              url: `/api/admin/users/${encodeURIComponent(session.username ?? "")}/sessions`,
                              label: copy.signOutAll,
                              message: copy.signOutAllConfirm,
                            })
                          }
                        >
                          <AdminActionIcon action="remove" />
                          {copy.signOutAll}
                        </Dropdown.Item>
                      )}
                    </>
                  )}
                </RowActions>
              </td>
            </tr>
          );
        })}
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
      <Modal
        show={detail !== null || detailLoading}
        onHide={() => setDetail(null)}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>{copy.sessionDetails}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detailLoading && !detail ? (
            <LoadingState />
          ) : detail ? (
            <div className="d-grid gap-4">
              <dl className="row mb-0">
                <dt className="col-sm-4">{copy.sessionId}</dt>
                <dd className="col-sm-8 font-monospace text-break">{detail.session.id}</dd>
                <dt className="col-sm-4">{copy.user}</dt>
                <dd className="col-sm-8">{detail.session.username ?? "-"}</dd>
                <dt className="col-sm-4">{copy.created}</dt>
                <dd className="col-sm-8">{date(detail.session.createdAt)}</dd>
                <dt className="col-sm-4">{copy.lastActive}</dt>
                <dd className="col-sm-8">{date(detail.session.lastAccessedAt)}</dd>
                <dt className="col-sm-4">{copy.expires}</dt>
                <dd className="col-sm-8">{date(detail.session.expiresAt)}</dd>
              </dl>
              <div>
                <h3 className="h6">{copy.authorizations}</h3>
                {detail.authorizations.length === 0 ? (
                  <div className="text-body-secondary">{copy.noAuthorizations}</div>
                ) : (
                  <div className="table-responsive">
                    <table className="table table-sm align-middle mb-0">
                      <thead>
                        <tr>
                          <th>{copy.client}</th>
                          <th>{copy.grantType}</th>
                          <th>{copy.grantedScopes}</th>
                          <th>{copy.accessTokenExpires}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detail.authorizations.map((authorization) => (
                          <tr key={authorization.id}>
                            <td>
                              <div>{authorization.clientName}</div>
                              <div className="small text-body-secondary font-monospace">
                                {authorization.clientId}
                              </div>
                            </td>
                            <td>{authorization.grantType}</td>
                            <td>
                              {authorization.scopes.map((scope) => (
                                <Badge bg="secondary" className="me-1" key={scope}>
                                  {scope}
                                </Badge>
                              ))}
                            </td>
                            <td>
                              {authorization.accessTokenExpiresAt
                                ? date(authorization.accessTokenExpiresAt)
                                : "-"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          ) : null}
        </Modal.Body>
      </Modal>
    </>
  );
}

function ConsentsTable({
  items,
  request,
  copy,
  canManage,
  locale,
}: {
  items: Consent[];
  request: AdminRequest;
  copy: Copy;
  canManage: boolean;
  locale?: Locale;
}) {
  const [consentToRevoke, setConsentToRevoke] = useState<Consent | null>(null);
  const formatDate = (value: string) => new Date(value).toLocaleString(locale);
  return (
    <>
      <thead>
        <tr>
          <th>{copy.user}</th>
          <th>{copy.client}</th>
          <th>{copy.grantedScopes}</th>
          <th>{copy.created}</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {items.map((consent) => {
          const detailHref = locale
            ? `/${locale}/admin/consents/${encodeConsentRouteKey(consent.clientId, consent.principalName)}`
            : undefined;
          return (
            <tr key={`${consent.clientId}-${consent.principalName}`}>
              <td data-label={copy.user}>
                {locale && consent.userId ? (
                  <Link href={`/${locale}/admin/users/${consent.userId}/details`}>
                    {consent.principalName}
                  </Link>
                ) : (
                  consent.principalName
                )}
              </td>
              <td data-label={copy.client}>
                <div>
                  {locale ? (
                    <Link
                      href={`/${locale}/admin/clients/${encodeURIComponent(consent.clientId)}/settings`}
                    >
                      {consent.clientName}
                    </Link>
                  ) : (
                    consent.clientName
                  )}
                </div>
                <div className="small text-body-secondary font-monospace">{consent.clientId}</div>
              </td>
              <td data-label={copy.grantedScopes}>
                <div className="d-flex flex-wrap gap-1">
                  {consent.authorities.map((scope) => (
                    <Badge bg="light" text="dark" className="border" key={scope}>
                      {scope.replace("SCOPE_", "")}
                    </Badge>
                  ))}
                </div>
              </td>
              <td data-label={copy.created}>{formatDate(consent.createdAt)}</td>
              <td className="text-end">
                <RowActions label={`${consent.clientName} consent actions`}>
                  {detailHref && (
                    <Dropdown.Item as={Link} href={detailHref}>
                      View details
                    </Dropdown.Item>
                  )}
                  {canManage && (
                    <>
                      {detailHref && <Dropdown.Divider />}
                      <Dropdown.Item
                        className="text-danger"
                        onClick={() => setConsentToRevoke(consent)}
                      >
                        <AdminActionIcon action="revoke" />
                        {copy.revoke}
                      </Dropdown.Item>
                    </>
                  )}
                </RowActions>
              </td>
            </tr>
          );
        })}
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
