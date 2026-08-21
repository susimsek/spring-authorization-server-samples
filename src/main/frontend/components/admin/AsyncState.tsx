import { Alert, Spinner } from "react-bootstrap";

export function LoadingState() {
  return (
    <div className="py-5 text-center">
      <Spinner />
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return <Alert variant="danger">{message}</Alert>;
}

export function EmptyState({ message }: { message: string }) {
  return <div className="py-5 text-center text-body-secondary">{message}</div>;
}
