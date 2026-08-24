import type { ReactNode } from "react";
import { Alert, Button } from "react-bootstrap";

export function LoadingState() {
  return (
    <div className="console-skeleton" role="status" aria-label="Loading">
      <div className="console-skeleton-title" />
      <div className="console-skeleton-toolbar" />
      {Array.from({ length: 5 }).map((_, index) => (
        <div className="console-skeleton-row" key={index} />
      ))}
    </div>
  );
}

export function DetailLoadingState() {
  return (
    <div className="console-skeleton console-skeleton-detail" role="status" aria-label="Loading">
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
  retryLabel = "Retry",
}: {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <Alert
      variant="danger"
      className="d-flex flex-wrap align-items-center justify-content-between gap-2"
    >
      <span>{message}</span>
      {onRetry && (
        <Button size="sm" variant="outline-danger" onClick={onRetry}>
          {retryLabel}
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
