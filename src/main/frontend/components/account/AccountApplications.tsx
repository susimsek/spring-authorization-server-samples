"use client";

import { faCube, faShieldHalved } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useCallback, useEffect, useState } from "react";
import { Badge, Button, Card } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import { accountRequest } from "@/lib/account-api";
import { DetailLoadingState, EmptyState, ErrorState } from "@/components/admin/AsyncState";
import { ConfirmModal } from "@/components/admin/ConfirmModal";
import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import { useAccountAuth } from "./AccountAuthProvider";

type Application = {
  clientId: string;
  clientName: string;
  scopes: string[];
  createdAt: string;
  updatedAt: string;
};

export function AccountApplications({ dictionary }: { dictionary: Dictionary }) {
  const { accessToken } = useAccountAuth();
  const alerts = useConsoleAlerts();
  const copy = dictionary.account;
  const [items, setItems] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [pending, setPending] = useState<Application | null>(null);

  const load = useCallback(async () => {
    if (!accessToken) return;
    setLoading(true);
    const response = await accountRequest<Application[]>(accessToken, {
      url: "/api/account/applications",
    });
    if (response.status < 300) {
      setItems(response.data);
      setFailed(false);
    } else {
      setFailed(true);
    }
    setLoading(false);
  }, [accessToken]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const revoke = async (application: Application) => {
    if (!accessToken) return;
    const response = await accountRequest(accessToken, {
      method: "DELETE",
      url: `/api/account/applications/${encodeURIComponent(application.clientId)}`,
    });
    if (response.status < 300) {
      setItems((current) => current.filter((item) => item.clientId !== application.clientId));
      alerts.addAlert(copy.applications.revoke);
    } else alerts.addError(copy.common.operationError);
  };

  if (loading) return <DetailLoadingState />;
  if (failed)
    return (
      <ErrorState
        message={copy.common.operationError}
        retryLabel={copy.common.retry}
        onRetry={() => void load()}
      />
    );

  return (
    <>
      <Card className="admin-panel-card account-panel-card">
        <Card.Body className="p-0">
          <div className="p-4 border-bottom">
            <h2 className="h5 mb-1">{copy.applications.title}</h2>
            <div className="text-body-secondary small">{copy.applications.help}</div>
          </div>
          {items.length === 0 ? (
            <EmptyState message={copy.common.empty} />
          ) : (
            <div className="account-application-list">
              {items.map((application) => (
                <div
                  className="account-application-card"
                  key={application.clientId}
                  data-cy="application-row"
                >
                  <div className="account-application-icon">
                    <FontAwesomeIcon icon={faCube} />
                  </div>
                  <div className="flex-grow-1 min-w-0">
                    <div className="fw-semibold">{application.clientName}</div>
                    <div className="small text-body-secondary font-monospace text-break">
                      {application.clientId}
                    </div>
                    <div className="small text-body-secondary mt-3 mb-2">
                      <FontAwesomeIcon className="me-2" icon={faShieldHalved} />
                      {copy.applications.access}
                    </div>
                    <div className="d-flex flex-wrap gap-1">
                      {application.scopes.map((scope) => (
                        <Badge key={scope} bg="light" text="dark" className="border">
                          {scope.replace(/^SCOPE_/, "")}
                        </Badge>
                      ))}
                    </div>
                    <div className="account-session-meta mt-3">
                      <span>
                        {copy.applications.grantedAt}:{" "}
                        {new Date(application.createdAt).toLocaleString()}
                      </span>
                      <span>
                        {copy.applications.updatedAt}:{" "}
                        {new Date(application.updatedAt).toLocaleString()}
                      </span>
                    </div>
                  </div>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={() => setPending(application)}
                  >
                    {copy.applications.revoke}
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card.Body>
      </Card>

      <ConfirmModal
        cancelLabel={dictionary.admin.common.cancel}
        confirmLabel={copy.applications.revoke}
        message={copy.applications.revokeConfirm}
        onCancel={() => setPending(null)}
        onConfirm={() => {
          const application = pending;
          setPending(null);
          if (application) void revoke(application);
        }}
        show={pending !== null}
      />
    </>
  );
}
