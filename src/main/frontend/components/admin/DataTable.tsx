import type { ReactNode } from "react";
import { Card, Table } from "react-bootstrap";

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
    <Card className="border-0 shadow-sm">
      <Card.Body className="p-0">
        {isEmpty ? (
          <EmptyState message={emptyMessage} />
        ) : (
          <div className="table-responsive">
            <Table hover className="admin-data-table mb-0 align-middle">
              {children}
            </Table>
          </div>
        )}
      </Card.Body>
      {footer && <Card.Footer>{footer}</Card.Footer>}
    </Card>
  );
}
