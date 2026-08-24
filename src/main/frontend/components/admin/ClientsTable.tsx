"use client";

import { useEffect, useState } from "react";
import { Badge, Dropdown } from "react-bootstrap";
import Link from "next/link";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { useAdminAuth } from "./AdminAuthProvider";
import { PaginationControls } from "./PaginationControls";
import { ResourceFilters } from "./ResourceFilters";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { useAdminTableState } from "./useAdminTableState";
import { RowActions } from "./RowActions";

export type AdminClient = {
  id: string;
  clientId: string;
  clientName: string;
  authorizationGrantTypes: string[];
  clientAuthenticationMethods: string[];
  scopes: string[];
  requireAuthorizationConsent: boolean;
  requireProofKey: boolean;
};

export function ClientsTable({ locale, dictionary }: { locale: Locale; dictionary: Dictionary }) {
  const { accessToken } = useAdminAuth();
  const [clients, setClients] = useState<AdminClient[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const { query, page, setPage, setQuery, setSize, size } = useAdminTableState();
  useEffect(() => {
    if (!accessToken) return;
    adminRequest<{ content: AdminClient[]; totalPages: number; totalElements: number }>(
      accessToken,
      { url: `/api/admin/clients?q=${encodeURIComponent(query)}&page=${page}&size=${size}` },
    )
      .then((r) => {
        if (r.status >= 300) throw new Error();
        setClients(r.data.content);
        setTotalPages(r.data.totalPages);
        setTotalElements(r.data.totalElements);
        setError(false);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, size]);
  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={dictionary.admin.clients.loadError} />;
  return (
    <>
      <ResourceFilters
        key={query}
        query={query}
        searchLabel={dictionary.admin.clients.search}
        onQueryChange={(v) => {
          setLoading(true);
          setQuery(v);
        }}
      >
        <Link href={`/${locale}/admin/clients/new`} className="btn btn-primary text-nowrap">
          {dictionary.admin.clients.create}
        </Link>
      </ResourceFilters>
      <DataTable
        isEmpty={clients.length === 0}
        emptyMessage={dictionary.admin.clients.empty}
        footer={
          totalElements > 0 ? (
            <PaginationControls
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              size={size}
              rowsPerPage={dictionary.admin.resources.rowsPerPage}
              pageLabel={dictionary.admin.resources.page}
              previous={dictionary.admin.resources.previous}
              next={dictionary.admin.resources.next}
              first={dictionary.admin.resources.first}
              last={dictionary.admin.resources.last}
              onPageChange={(p) => {
                setLoading(true);
                setPage(p);
              }}
              onSizeChange={(s) => {
                setLoading(true);
                setSize(s);
              }}
            />
          ) : undefined
        }
      >
        <thead>
          <tr>
            <th>{dictionary.admin.clients.client}</th>
            <th>{dictionary.admin.clients.grants}</th>
            <th>{dictionary.admin.clients.scopes}</th>
            <th>{dictionary.admin.clients.security}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {clients.map((c) => (
            <tr key={c.id}>
              <td>
                <Link
                  className="fw-semibold text-decoration-none"
                  href={`/${locale}/admin/clients/${encodeURIComponent(c.id)}/settings`}
                >
                  {c.clientName}
                </Link>
                <div className="small text-body-secondary font-monospace">{c.clientId}</div>
              </td>
              <td>
                {c.authorizationGrantTypes.map((g) => (
                  <Badge bg="secondary" className="me-1" key={g}>
                    {g}
                  </Badge>
                ))}
              </td>
              <td>
                {c.scopes.slice(0, 4).map((s) => (
                  <Badge bg="light" text="dark" className="border me-1" key={s}>
                    {s}
                  </Badge>
                ))}
                {c.scopes.length > 4 && (
                  <span className="small text-body-secondary">+{c.scopes.length - 4}</span>
                )}
              </td>
              <td>
                <Badge bg={c.requireProofKey ? "success" : "secondary"}>
                  PKCE{" "}
                  {c.requireProofKey ? dictionary.admin.common.on : dictionary.admin.common.off}
                </Badge>
              </td>
              <td className="text-end">
                <RowActions label={`${c.clientName} actions`}>
                  <Dropdown.Item
                    as={Link}
                    href={`/${locale}/admin/clients/${encodeURIComponent(c.id)}/settings`}
                  >
                    {dictionary.admin.clients.edit}
                  </Dropdown.Item>
                </RowActions>
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </>
  );
}
