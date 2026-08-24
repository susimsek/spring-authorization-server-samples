import type { ReactNode } from "react";
import { Table } from "react-bootstrap";
import { EmptyState } from "./AsyncState";

export function DataTable({
  children,
  isEmpty,
  emptyMessage,
  footer,
}: {
  children: ReactNode;
  isEmpty: boolean;
  emptyMessage: string;
  footer?: ReactNode;
}) {
  return (
    <section className="console-table-surface">
      {isEmpty ? (
        <EmptyState message={emptyMessage} />
      ) : (
        <div className="table-responsive">
          <Table hover className="admin-data-table mb-0 align-middle">
            {children}
          </Table>
        </div>
      )}
      {footer && <div className="console-table-footer">{footer}</div>}
    </section>
  );
}
