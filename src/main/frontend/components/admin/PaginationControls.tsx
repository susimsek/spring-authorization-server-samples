import { Button } from "react-bootstrap";

export function PaginationControls({
  page,
  totalPages,
  previous,
  next,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  previous: string;
  next: string;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="d-flex justify-content-end gap-2">
      <Button
        size="sm"
        variant="outline-secondary"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        {previous}
      </Button>
      <Button
        size="sm"
        variant="outline-secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        {next}
      </Button>
    </div>
  );
}
