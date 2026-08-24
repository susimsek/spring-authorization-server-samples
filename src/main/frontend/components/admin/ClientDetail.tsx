"use client";

import { useEffect, useState } from "react";
import { Alert, Badge, Button, Card, Dropdown } from "react-bootstrap";
import { useRouter } from "next/navigation";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import { useAdminAuth } from "./AdminAuthProvider";
import { ConfirmModal } from "./ConfirmModal";
import { ErrorState, LoadingState } from "./AsyncState";
import { ResultModal } from "./ResultModal";
import { DetailTabs } from "./DetailTabs";
import { RowActions } from "./RowActions";
import { AdminBreadcrumb } from "./AdminBreadcrumb";
import { ClientForm } from "./ClientForm";
import { EntityRelatedData } from "./EntityRelatedData";
import { ClientScopeAssignments } from "./ClientScopeAssignments";
import type { AdminClient } from "./ClientsTable";

const CLIENT_DETAIL_TABS = [
  "settings",
  "credentials",
  "scopes",
  "sessions",
  "consents",
  "events",
] as const;

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
  tab = "settings",
}: {
  locale: Locale;
  dictionary: Dictionary;
  id: string | null;
  tab?: string | null;
}) {
  const router = useRouter();
  const { access, accessToken } = useAdminAuth();
  const [client, setClient] = useState<Detail | null>(null);
  const [error, setError] = useState(false);
  const [secret, setSecret] = useState<string | null>(null);
  const [secretError, setSecretError] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showSecretConfirm, setShowSecretConfirm] = useState(false);
  const activeTab = CLIENT_DETAIL_TABS.includes((tab ?? "") as (typeof CLIENT_DETAIL_TABS)[number])
    ? (tab as string)
    : "settings";
  useEffect(() => {
    if (!id || !accessToken) return;
    adminRequest<Detail>(accessToken, { url: `/api/admin/clients/${encodeURIComponent(id)}` })
      .then((r) => {
        if (r.status >= 300) throw new Error();
        setClient(r.data);
      })
      .catch(() => setError(true));
  }, [accessToken, id]);
  const remove = async () => {
    if (!id || !accessToken) return;
    const r = await adminRequest(accessToken, {
      url: `/api/admin/clients/${encodeURIComponent(id)}`,
      method: "DELETE",
    });
    if (r.status < 300) {
      router.push(`/${locale}/admin/clients`);
      router.refresh();
    } else setError(true);
  };
  const regenerateSecret = async () => {
    if (!id || !accessToken) return;
    setSecretError(false);
    const r = await adminRequest<{ clientSecret: string }>(accessToken, {
      url: `/api/admin/clients/${encodeURIComponent(id)}/secret`,
      method: "POST",
    });
    if (r.status >= 300) {
      setSecretError(true);
      return;
    }
    setSecret(r.data.clientSecret);
  };
  if (!id || error) return <ErrorState message={dictionary.admin.clients.notFound} />;
  if (!client) return <LoadingState />;
  const tr = locale === "tr";
  const detailUrl = `/${locale}/admin/clients/${encodeURIComponent(client.id)}`;
  const tabs = [
    { key: "settings", label: tr ? "Ayarlar" : "Settings", href: `${detailUrl}/settings` },
    {
      key: "credentials",
      label: tr ? "Kimlik bilgileri" : "Credentials",
      href: `${detailUrl}/credentials`,
    },
    {
      key: "scopes",
      label: tr ? "İstemci scope’ları" : "Client scopes",
      href: `${detailUrl}/scopes`,
    },
    { key: "sessions", label: tr ? "Oturumlar" : "Sessions", href: `${detailUrl}/sessions` },
    { key: "consents", label: tr ? "İzinler" : "Consents", href: `${detailUrl}/consents` },
    { key: "events", label: tr ? "Olaylar" : "Events", href: `${detailUrl}/events` },
  ];
  return (
    <>
      <AdminBreadcrumb
        items={[
          { label: dictionary.admin.clients.title, href: `/${locale}/admin/clients` },
          { label: client.clientName },
        ]}
      />
      <div className="admin-detail-heading">
        <div>
          <h1 className="h3 mb-1">{client.clientName}</h1>
          <div className="font-monospace text-body-secondary">{client.clientId}</div>
        </div>
        <RowActions label={`${client.clientName} actions`}>
          <Dropdown.Item
            disabled={!access?.manageClients}
            onClick={() => setShowSecretConfirm(true)}
          >
            {dictionary.admin.clients.regenerateSecret}
          </Dropdown.Item>
          <Dropdown.Divider />
          <Dropdown.Item className="text-danger" onClick={() => setShowDeleteConfirm(true)}>
            {dictionary.admin.clients.delete}
          </Dropdown.Item>
        </RowActions>
      </div>
      <DetailTabs tabs={tabs} active={activeTab} />
      {secretError && <Alert variant="danger">{dictionary.admin.clients.secretError}</Alert>}
      {activeTab === "settings" && (
        <ClientForm locale={locale} dictionary={dictionary} mode="edit" id={client.id} embedded />
      )}
      {activeTab === "credentials" &&
        (() => {
          const methods = Array.from(client.clientAuthenticationMethods);
          const publicClient = methods.includes("none");
          const expiresAt = client.clientSecretExpiresAt
            ? new Date(client.clientSecretExpiresAt)
            : null;
          const methodLabel = publicClient
            ? tr
              ? "Public istemci"
              : "Public client"
            : tr
              ? "Confidential istemci"
              : "Confidential client";
          return (
            <div className="d-grid gap-3">
              <Card className="admin-panel-card">
                <Card.Body>
                  <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
                    <div>
                      <h2 className="h5 mb-1">
                        {tr ? "İstemci kimlik doğrulaması" : "Client authentication"}
                      </h2>
                      <p className="text-body-secondary small mb-0">
                        {tr
                          ? "Token endpoint'inde bu istemcinin nasıl kimlik doğruladığını gösterir."
                          : "How this client authenticates at the token endpoint."}
                      </p>
                    </div>
                    <Badge bg={publicClient ? "secondary" : "primary"}>{methodLabel}</Badge>
                  </div>
                  <dl className="row mt-4 mb-0">
                    <dt className="col-sm-4">
                      {tr ? "Kimlik doğrulama yöntemi" : "Authentication method"}
                    </dt>
                    <dd className="col-sm-8 font-monospace">{methods.join(", ")}</dd>
                    <dt className="col-sm-4">PKCE</dt>
                    <dd className="col-sm-8">
                      {client.requireProofKey ? (
                        <Badge bg="success">S256 required</Badge>
                      ) : (
                        <Badge bg="secondary">{tr ? "Zorunlu değil" : "Not required"}</Badge>
                      )}
                    </dd>
                    <dt className="col-sm-4">
                      {tr ? "İstemci oluşturulma zamanı" : "Client created"}
                    </dt>
                    <dd className="col-sm-8">
                      {client.clientIdIssuedAt
                        ? new Date(client.clientIdIssuedAt).toLocaleString(locale)
                        : "—"}
                    </dd>
                  </dl>
                </Card.Body>
              </Card>

              {!publicClient && (
                <Card className="admin-panel-card">
                  <Card.Body>
                    <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
                      <div>
                        <h2 className="h5 mb-1">{tr ? "Client secret" : "Client secret"}</h2>
                        <p className="text-body-secondary small mb-0">
                          {tr
                            ? "Secret yalnız oluşturulduğu veya yenilendiği anda gösterilir; sunucuda hash'li saklanır."
                            : "The secret is shown only when created or regenerated; the server stores only its hash."}
                        </p>
                      </div>
                      <Badge bg="success">{tr ? "Yapılandırılmış" : "Configured"}</Badge>
                    </div>
                    <dl className="row mt-4">
                      <dt className="col-sm-4">{tr ? "Bitiş zamanı" : "Expires"}</dt>
                      <dd className="col-sm-8">
                        {expiresAt
                          ? expiresAt.toLocaleString(locale)
                          : tr
                            ? "Süresiz"
                            : "Does not expire"}
                      </dd>
                    </dl>
                    <Button
                      variant="outline-primary"
                      disabled={!access?.manageClients}
                      onClick={() => setShowSecretConfirm(true)}
                    >
                      {dictionary.admin.clients.regenerateSecret}
                    </Button>
                  </Card.Body>
                </Card>
              )}
            </div>
          );
        })()}
      {activeTab === "scopes" && (
        <ClientScopeAssignments
          clientId={client.id}
          dictionary={dictionary}
          onChanged={(scopes) =>
            setClient((current) => (current ? { ...current, scopes } : current))
          }
        />
      )}
      {activeTab === "sessions" && (
        <EntityRelatedData
          resource="sessions"
          url={`/api/admin/clients/${encodeURIComponent(client.id)}/sessions`}
          locale={locale}
          dictionary={dictionary}
          canManage={access?.manageSessions ?? false}
        />
      )}
      {activeTab === "consents" && (
        <EntityRelatedData
          resource="consents"
          url={`/api/admin/clients/${encodeURIComponent(client.id)}/consents`}
          locale={locale}
          dictionary={dictionary}
          canManage={access?.manageConsents ?? false}
        />
      )}
      {activeTab === "events" && (
        <EntityRelatedData
          resource="events"
          url={`/api/admin/clients/${encodeURIComponent(client.id)}/events`}
          locale={locale}
          dictionary={dictionary}
        />
      )}
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
        confirmLabel={dictionary.admin.clients.regenerateSecret}
        message={
          tr
            ? "Mevcut client secret hemen geçersiz olacaktır. Yeni secret yalnız bir kez gösterilecektir. Devam edilsin mi?"
            : "The current client secret will become invalid immediately. The new secret will be shown only once. Continue?"
        }
        onCancel={() => setShowSecretConfirm(false)}
        onConfirm={() => {
          setShowSecretConfirm(false);
          void regenerateSecret();
        }}
        show={showSecretConfirm}
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
