import type { ReactNode } from "react";
import { Alert, Button } from "react-bootstrap";
import { useParams } from "next/navigation";

import { getDictionary } from "@/i18n/get-dictionary";

function useAdminCopy() {
  const params = useParams<{ lang: string }>();
  return getDictionary(params.lang === "tr" ? "tr" : "en").admin.common;
}

export function LoadingState() {
  const copy = useAdminCopy();
  return (
    <div className="console-skeleton" role="status" aria-label={copy.loading}>
      <div className="console-skeleton-title" />
      <div className="console-skeleton-toolbar" />
      {Array.from({ length: 5 }).map((_, index) => (
        <div className="console-skeleton-row" key={index} />
      ))}
    </div>
  );
}

export function DetailLoadingState() {
  const copy = useAdminCopy();
  return (
    <div
      className="console-skeleton console-skeleton-detail"
      role="status"
      aria-label={copy.loading}
    >
      <div className="console-skeleton-title" />
      <div className="console-skeleton-detail-line" />
      <div className="console-skeleton-detail-card" />
      <div className="console-skeleton-detail-card" />
    </div>
  );
}

export function ErrorState({
  message,
  onRetry,
  retryLabel,
}: {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  const copy = useAdminCopy();
  return (
    <Alert
      variant="danger"
      className="d-flex flex-wrap align-items-center justify-content-between gap-2"
    >
      <span>{message}</span>
      {onRetry && (
        <Button size="sm" variant="outline-danger" onClick={onRetry}>
          {retryLabel ?? copy.retry}
        </Button>
      )}
    </Alert>
  );
}

export function EmptyState({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className="console-empty-state text-center text-body-secondary">
      <div>{message}</div>
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}
