"use client";

import { useEffect, useState } from "react";
import { Alert, Badge, Button, Card, Col, Row } from "react-bootstrap";
import Link from "next/link";
import { useRouter } from "next/navigation";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";

import { useAdminAuth } from "./AdminAuthProvider";
import { ConfirmModal } from "./ConfirmModal";
import { ErrorState, LoadingState } from "./AsyncState";
import { ResultModal } from "./ResultModal";
import type { AdminClient } from "./ClientsTable";

type Detail = AdminClient & {
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  clientIdIssuedAt: string | null;
  clientSecretExpiresAt: string | null;
  authorizationCodeTimeToLive: string;
  accessTokenTimeToLive: string;
  refreshTokenTimeToLive: string;
};

export function ClientDetail({
  locale,
  dictionary,
  id,
}: {
  locale: Locale;
  dictionary: Dictionary;
  id: string | null;
}) {
  const router = useRouter();
  const { accessToken } = useAdminAuth();
  const [client, setClient] = useState<Detail | null>(null);
  const [error, setError] = useState(false);
  const [secret, setSecret] = useState<string | null>(null);
  const [secretError, setSecretError] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (!id || !accessToken) {
      return;
    }

    adminRequest<Detail>(accessToken, { url: `/api/admin/clients/${encodeURIComponent(id)}` })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setClient(response.data);
      })
      .catch(() => setError(true));
  }, [accessToken, id]);

  const remove = async () => {
    if (!id || !accessToken) return;
    const response = await adminRequest(accessToken, {
      url: `/api/admin/clients/${encodeURIComponent(id)}`,
      method: "DELETE",
    });
    if (response.status < 300) {
      router.push(`/${locale}/admin/clients`);
      router.refresh();
    } else {
      setError(true);
    }
  };

  const regenerateSecret = async () => {
    if (!id || !accessToken) return;
    setSecretError(false);
    const response = await adminRequest<{ clientSecret: string }>(accessToken, {
      url: `/api/admin/clients/${encodeURIComponent(id)}/secret`,
      method: "POST",
    });
    if (response.status >= 300) {
      setSecretError(true);
      return;
    }
    setSecret(response.data.clientSecret);
  };

  if (!id || error) {
    return <ErrorState message={dictionary.admin.clients.notFound} />;
  }

  if (!client) {
    return <LoadingState />;
  }

  const group = (title: string, items: string[]) => (
    <Card className="border-0 shadow-sm h-100">
      <Card.Body>
        <h2 className="h6">{title}</h2>
        {items.length ? (
          items.map((item) => (
            <div className="font-monospace small text-break py-1" key={item}>
              {item}
            </div>
          ))
        ) : (
          <span className="text-body-secondary">—</span>
        )}
      </Card.Body>
    </Card>
  );

  return (
    <>
      <div className="mb-4 d-flex flex-wrap justify-content-between gap-3">
        <div>
          <div className="text-body-secondary small">{dictionary.admin.clients.client}</div>
          <h1 className="h3 mb-1">{client.clientName}</h1>
          <div className="font-monospace text-body-secondary">{client.clientId}</div>
        </div>
        <div className="d-flex gap-2 align-items-start">
          <Link
            href={`/${locale}/admin/clients/edit?id=${encodeURIComponent(client.id)}`}
            className="btn btn-outline-primary"
          >
            {dictionary.admin.clients.edit}
          </Link>
          <Button variant="outline-secondary" onClick={regenerateSecret}>
            {dictionary.admin.clients.regenerateSecret}
          </Button>
          <Button variant="outline-danger" onClick={() => setShowDeleteConfirm(true)}>
            {dictionary.admin.clients.delete}
          </Button>
        </div>
      </div>

      {secretError && <Alert variant="danger">{dictionary.admin.clients.secretError}</Alert>}

      <Row className="g-3">
        <Col lg={6}>{group(dictionary.admin.clients.redirectUris, client.redirectUris)}</Col>
        <Col lg={6}>
          {group(dictionary.admin.clients.postLogoutUris, client.postLogoutRedirectUris)}
        </Col>
        <Col lg={6}>{group(dictionary.admin.clients.grants, client.authorizationGrantTypes)}</Col>
        <Col lg={6}>
          {group(dictionary.admin.clients.authMethods, client.clientAuthenticationMethods)}
        </Col>
        <Col lg={12}>
          <Card className="border-0 shadow-sm">
            <Card.Body>
              <h2 className="h6">{dictionary.admin.clients.security}</h2>
              <div className="d-flex flex-wrap gap-2">
                <Badge bg={client.requireProofKey ? "success" : "secondary"}>
                  PKCE{" "}
                  {client.requireProofKey
                    ? dictionary.admin.common.on
                    : dictionary.admin.common.off}
                </Badge>
                <Badge bg={client.requireAuthorizationConsent ? "success" : "secondary"}>
                  Consent{" "}
                  {client.requireAuthorizationConsent
                    ? dictionary.admin.common.on
                    : dictionary.admin.common.off}
                </Badge>
                {client.scopes.map((scope) => (
                  <Badge bg="light" text="dark" className="border" key={scope}>
                    {scope}
                  </Badge>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <ResultModal
        closeLabel={dictionary.admin.common.close}
        message={dictionary.admin.clients.secretHelp}
        onClose={() => setSecret(null)}
        show={secret !== null}
        title={dictionary.admin.clients.secretTitle}
        value={secret}
      />
      <ConfirmModal
        cancelLabel={dictionary.admin.common.cancel}
        confirmLabel={dictionary.admin.clients.delete}
        message={dictionary.admin.clients.deleteConfirm}
        onCancel={() => setShowDeleteConfirm(false)}
        onConfirm={() => {
          setShowDeleteConfirm(false);
          void remove();
        }}
        show={showDeleteConfirm}
      />
    </>
  );
}
