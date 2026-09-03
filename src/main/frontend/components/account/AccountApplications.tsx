"use client";

import { faCube, faShieldHalved } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useState } from "react";
import { Badge, Button, Card } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";
import { useDateTimeFormatter } from "@/i18n/useDateTimeFormatter";
import {
  type AccountApplication,
  useGetAccountApplicationsQuery,
  useRevokeAccountApplicationMutation,
} from "@/store/account-api-slice";
import { DetailLoadingState, EmptyState, ErrorState } from "@/components/admin/AsyncState";
import { ConfirmModal } from "@/components/admin/ConfirmModal";
import { PaginationControls } from "@/components/admin/PaginationControls";
import { useAdminTableState } from "@/components/admin/useAdminTableState";
import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import { useAccountAuth } from "./AccountAuthProvider";

export function AccountApplications({ dictionary }: { dictionary: Dictionary }) {
  const formatDateTime = useDateTimeFormatter();
  const { accessToken } = useAccountAuth();
  const alerts = useConsoleAlerts();
  const copy = dictionary.account;
  const [pending, setPending] = useState<AccountApplication | null>(null);
  const { page, size, setPage, setSize } = useAdminTableState();
  const { data, isError, isLoading, refetch } = useGetAccountApplicationsQuery(
    { accessToken: accessToken ?? "", page, size },
    { skip: !accessToken },
  );
  const [revokeApplication] = useRevokeAccountApplicationMutation();
  const items = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const revoke = async (application: AccountApplication) => {
    if (!accessToken) return;
    try {
      await revokeApplication({ accessToken, clientId: application.clientId }).unwrap();
      if (items.length === 1 && page > 0) setPage(page - 1);
      alerts.addAlert(copy.applications.revoke);
    } catch {
      alerts.addError(copy.common.operationError);
    }
  };

  if (isLoading) return <DetailLoadingState />;
  if (isError)
    return (
      <ErrorState
        message={copy.common.operationError}
        retryLabel={copy.common.retry}
        onRetry={() => void refetch()}
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
                        {copy.applications.grantedAt}: {formatDateTime(application.createdAt)}
                      </span>
                      <span>
                        {copy.applications.updatedAt}: {formatDateTime(application.updatedAt)}
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
          <div className="px-4 pb-4">
            <PaginationControls
              first={dictionary.admin.resources.first}
              last={dictionary.admin.resources.last}
              next={dictionary.admin.resources.next}
              onPageChange={setPage}
              onSizeChange={setSize}
              page={page}
              pageLabel={dictionary.admin.resources.page}
              previous={dictionary.admin.resources.previous}
              rowsPerPage={dictionary.admin.resources.rowsPerPage}
              size={size}
              totalElements={totalElements}
              totalPages={totalPages}
            />
          </div>
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
