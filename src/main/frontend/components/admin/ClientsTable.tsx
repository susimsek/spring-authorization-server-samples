"use client";

import { useEffect, useState } from "react";
import { Badge, Form } from "react-bootstrap";
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
  const { query, page, setPage, setQuery, setSize, size } = useAdminTableState();

  useEffect(() => {
    if (!accessToken) return;

    adminRequest<{ content: AdminClient[]; totalPages: number }>(accessToken, {
      url: `/api/admin/clients?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setClients(response.data.content);
        setTotalPages(response.data.totalPages);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, size]);

  if (loading) {
    return <LoadingState />;
  }

  if (error) {
    return <ErrorState message={dictionary.admin.clients.loadError} />;
  }

  return (
    <>
      <ResourceFilters
        key={query}
        query={query}
        searchLabel={dictionary.admin.clients.search}
        onQueryChange={(value) => {
          setLoading(true);
          setQuery(value);
        }}
      >
        <Form.Select
          aria-label={dictionary.admin.resources.records}
          onChange={(event) => {
            setLoading(true);
            setSize(Number(event.target.value));
          }}
          style={{ maxWidth: "6rem" }}
          value={size}
        >
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </Form.Select>
        <Link href={`/${locale}/admin/clients/new`} className="btn btn-primary text-nowrap">
          {dictionary.admin.clients.create}
        </Link>
      </ResourceFilters>
      <DataTable
        isEmpty={clients.length === 0}
        emptyMessage={dictionary.admin.clients.empty}
        footer={
          <PaginationControls
            page={page}
            totalPages={totalPages}
            previous={dictionary.admin.resources.previous}
            next={dictionary.admin.resources.next}
            onPageChange={setPage}
          />
        }
      >
        <thead className="table-light">
          <tr>
            <th>{dictionary.admin.clients.client}</th>
            <th>{dictionary.admin.clients.grants}</th>
            <th>{dictionary.admin.clients.scopes}</th>
            <th>{dictionary.admin.clients.security}</th>
          </tr>
        </thead>
        <tbody>
          {clients.map((client) => (
            <tr key={client.id}>
              <td data-label={dictionary.admin.clients.client}>
                <Link
                  className="fw-semibold text-decoration-none"
                  href={`/${locale}/admin/clients/detail?id=${encodeURIComponent(client.id)}`}
                >
                  {client.clientName}
                </Link>
                <div className="small text-body-secondary font-monospace">{client.clientId}</div>
              </td>
              <td data-label={dictionary.admin.clients.grants}>
                {client.authorizationGrantTypes.map((grant) => (
                  <Badge bg="secondary" className="me-1" key={grant}>
                    {grant}
                  </Badge>
                ))}
              </td>
              <td data-label={dictionary.admin.clients.scopes}>
                {client.scopes.map((scope) => (
                  <Badge bg="light" text="dark" className="border me-1" key={scope}>
                    {scope}
                  </Badge>
                ))}
              </td>
              <td data-label={dictionary.admin.clients.security}>
                <div className="small">
                  PKCE:{" "}
                  <strong>
                    {client.requireProofKey
                      ? dictionary.admin.common.yes
                      : dictionary.admin.common.no}
                  </strong>
                </div>
                <div className="small">
                  Consent:{" "}
                  <strong>
                    {client.requireAuthorizationConsent
                      ? dictionary.admin.common.yes
                      : dictionary.admin.common.no}
                  </strong>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </>
  );
}
