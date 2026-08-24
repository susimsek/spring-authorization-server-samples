"use client";

import { useEffect, useState } from "react";
import { Badge, Button, Card, Nav, Tab } from "react-bootstrap";
import { useParams } from "next/navigation";

import { ViewHeader } from "@/components/admin/ViewHeader";
import { useAdminAuth } from "@/components/admin/AdminAuthProvider";
import { adminRequest } from "@/lib/admin-api";

type KeySummary = {
  kid: string;
  type: string;
  algorithm: string;
  use: string;
  createdAt: string;
};
type ServerInfo = {
  issuer: string;
  discoveryEndpoint: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  introspectionEndpoint: string;
  revocationEndpoint: string;
  jwksEndpoint: string;
  userInfoEndpoint: string;
  endSessionEndpoint: string;
  sessionTimeout: string | null;
  activeSigningKey: KeySummary | null;
};

function JsonPanel({ value }: { value: unknown }) {
  return (
    <pre className="bg-body-tertiary border rounded-3 p-3 small overflow-auto mb-0">
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}

export default function ServerInfoPage() {
  const { accessToken } = useAdminAuth();
  const params = useParams<{ lang: string }>();
  const tr = params.lang === "tr";
  const [info, setInfo] = useState<ServerInfo | null>(null);
  const [discovery, setDiscovery] = useState<unknown>(null);
  const [jwks, setJwks] = useState<unknown>(null);

  useEffect(() => {
    if (!accessToken) return;
    const controller = new AbortController();
    adminRequest<ServerInfo>(accessToken, {
      url: "/api/admin/server-info",
      signal: controller.signal,
    })
      .then(async (response) => {
        if (response.status >= 300) return;
        setInfo(response.data);
        const [discoveryResponse, jwksResponse] = await Promise.all([
          fetch(response.data.discoveryEndpoint, { signal: controller.signal }),
          fetch(response.data.jwksEndpoint, { signal: controller.signal }),
        ]);
        if (discoveryResponse.ok) setDiscovery(await discoveryResponse.json());
        if (jwksResponse.ok) setJwks(await jwksResponse.json());
      })
      .catch(() => {});
    return () => controller.abort();
  }, [accessToken]);

  const copy = async (value: string) => {
    await navigator.clipboard.writeText(value);
  };

  const endpointRows = info
    ? [
        ["Issuer", info.issuer],
        [tr ? "Yetkilendirme" : "Authorization", info.authorizationEndpoint],
        ["Token", info.tokenEndpoint],
        [tr ? "Introspection" : "Introspection", info.introspectionEndpoint],
        [tr ? "Revocation" : "Revocation", info.revocationEndpoint],
        ["UserInfo", info.userInfoEndpoint],
        ["JWKS", info.jwksEndpoint],
        [tr ? "Oturum sonlandırma" : "End session", info.endSessionEndpoint],
      ]
    : [];

  return (
    <div className="d-grid gap-4">
      <ViewHeader
        title={tr ? "Sunucu bilgisi" : "Server info"}
        description={
          tr
            ? "Authorization Server çalışma zamanı, OIDC discovery ve imzalama anahtarlarını görüntüleyin."
            : "Inspect Authorization Server runtime settings, OIDC discovery and signing keys."
        }
      />

      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5">{tr ? "Çalışma zamanı" : "Runtime"}</h2>
          <dl className="row mb-0">
            <dt className="col-lg-3">{tr ? "SSO session timeout" : "SSO session timeout"}</dt>
            <dd className="col-lg-9">{info?.sessionTimeout ?? "—"}</dd>
            <dt className="col-lg-3">{tr ? "Aktif imzalama anahtarı" : "Active signing key"}</dt>
            <dd className="col-lg-9">
              {info?.activeSigningKey ? (
                <div className="d-flex flex-wrap gap-2 align-items-center">
                  <code>{info.activeSigningKey.kid}</code>
                  <Badge bg="success">active</Badge>
                  <Badge bg="light" text="dark" className="border">
                    {info.activeSigningKey.algorithm}
                  </Badge>
                  <span className="text-body-secondary small">
                    {new Date(info.activeSigningKey.createdAt).toLocaleString(params.lang)}
                  </span>
                </div>
              ) : (
                "—"
              )}
            </dd>
          </dl>
        </Card.Body>
      </Card>

      <Card className="admin-panel-card">
        <Card.Body>
          <h2 className="h5">
            {tr ? "OIDC / OAuth 2.0 endpointleri" : "OIDC / OAuth 2.0 endpoints"}
          </h2>
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <tbody>
                {endpointRows.map(([label, value]) => (
                  <tr key={label}>
                    <th className="text-nowrap">{label}</th>
                    <td className="font-monospace small text-break">{value}</td>
                    <td className="text-end">
                      <Button
                        size="sm"
                        variant="outline-secondary"
                        onClick={() => void copy(value)}
                      >
                        {tr ? "Kopyala" : "Copy"}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card.Body>
      </Card>

      <Tab.Container defaultActiveKey="discovery">
        <Card className="admin-panel-card">
          <Card.Header className="bg-body">
            <Nav variant="tabs">
              <Nav.Item>
                <Nav.Link eventKey="discovery">OpenID Configuration</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="jwks">JWKS</Nav.Link>
              </Nav.Item>
            </Nav>
          </Card.Header>
          <Card.Body>
            <Tab.Content>
              <Tab.Pane eventKey="discovery">
                <div className="d-flex justify-content-between align-items-center mb-3 gap-2">
                  <div className="small text-body-secondary text-break">
                    {info?.discoveryEndpoint ?? "—"}
                  </div>
                  {info && (
                    <Button
                      size="sm"
                      variant="outline-secondary"
                      onClick={() => void copy(JSON.stringify(discovery, null, 2))}
                    >
                      {tr ? "JSON kopyala" : "Copy JSON"}
                    </Button>
                  )}
                </div>
                <JsonPanel value={discovery ?? {}} />
              </Tab.Pane>
              <Tab.Pane eventKey="jwks">
                <div className="d-flex justify-content-between align-items-center mb-3 gap-2">
                  <div className="small text-body-secondary text-break">
                    {info?.jwksEndpoint ?? "—"}
                  </div>
                  {info && (
                    <Button
                      size="sm"
                      variant="outline-secondary"
                      onClick={() => void copy(JSON.stringify(jwks, null, 2))}
                    >
                      {tr ? "JSON kopyala" : "Copy JSON"}
                    </Button>
                  )}
                </div>
                <JsonPanel value={jwks ?? {}} />
              </Tab.Pane>
            </Tab.Content>
          </Card.Body>
        </Card>
      </Tab.Container>
    </div>
  );
}
