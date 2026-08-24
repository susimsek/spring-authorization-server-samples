import { useEffect, useEffectEvent, useState } from "react";
import { Form, InputGroup } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";

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
    <div className="admin-resource-filters p-3 border-bottom d-flex flex-wrap align-items-center gap-2">
      <InputGroup className="admin-search-input">
        <InputGroup.Text>
          <FontAwesomeIcon icon={faMagnifyingGlass} />
        </InputGroup.Text>
        <Form.Control
          aria-label={searchLabel}
          onChange={(event) => setValue(event.target.value)}
          placeholder={searchLabel}
          value={value}
        />
      </InputGroup>
      <div className="ms-auto d-flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
