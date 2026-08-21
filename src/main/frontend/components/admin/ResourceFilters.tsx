import { useEffect, useEffectEvent, useState } from "react";
import { Form } from "react-bootstrap";

export function ResourceFilters({
  query,
  searchLabel,
  onQueryChange,
  children,
}: {
  query: string;
  searchLabel: string;
  onQueryChange: (value: string) => void;
  children?: React.ReactNode;
}) {
  const [value, setValue] = useState(query);
  const notifyQueryChange = useEffectEvent(onQueryChange);

  useEffect(() => {
    if (value === query) return;
    const timeout = window.setTimeout(() => notifyQueryChange(value), 300);
    return () => window.clearTimeout(timeout);
  }, [query, value]);

  return (
    <div className="p-3 border-bottom d-flex flex-wrap gap-2">
      <Form.Control
        aria-label={searchLabel}
        className="flex-grow-1"
        onChange={(event) => setValue(event.target.value)}
        placeholder={searchLabel}
        value={value}
      />
      {children}
    </div>
  );
}
