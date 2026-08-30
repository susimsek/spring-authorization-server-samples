import { useEffect, useEffectEvent, useState } from "react";
import { Badge, Button, Form, InputGroup } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";

export function ResourceFilters({
  query,
  searchLabel,
  onQueryChange,
  children,
  sort,
  activeFilters = [],
  clearFiltersLabel,
  onClearFilters,
  resultCount,
  recordsLabel,
}: {
  query: string;
  searchLabel: string;
  onQueryChange: (value: string) => void;
  children?: React.ReactNode;
  sort?: {
    label: string;
    value: string;
    options: { value: string; label: string }[];
    onChange: (value: string) => void;
  };
  activeFilters?: { label: string; value: string; onRemove: () => void }[];
  clearFiltersLabel?: string;
  onClearFilters?: () => void;
  resultCount?: number;
  recordsLabel?: string;
}) {
  const [value, setValue] = useState(query);
  const notifyQueryChange = useEffectEvent(onQueryChange);
  useEffect(() => {
    if (value === query) return;
    const timeout = window.setTimeout(() => notifyQueryChange(value), 300);
    return () => window.clearTimeout(timeout);
  }, [query, value]);
  return (
    <div className="admin-resource-filters p-3 border-bottom">
      <div className="d-flex flex-wrap align-items-center gap-2">
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
        {sort && (
          <Form.Select
            aria-label={sort.label}
            className="admin-resource-filter-control"
            value={sort.value}
            onChange={(event) => sort.onChange(event.target.value)}
          >
            {sort.options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Form.Select>
        )}
        <div className="ms-auto d-flex flex-wrap gap-2">{children}</div>
      </div>
      {(activeFilters.length > 0 || resultCount !== undefined) && (
        <div className="admin-active-filters mt-3">
          {resultCount !== undefined && (
            <span className="text-body-secondary small">
              {resultCount} {recordsLabel}
            </span>
          )}
          {activeFilters.map((filter) => (
            <Badge
              bg="light"
              text="dark"
              className="border"
              key={`${filter.label}-${filter.value}`}
            >
              {filter.label}: {filter.value}
              <button
                aria-label={`${filter.label}: ${filter.value}`}
                className="admin-filter-remove"
                type="button"
                onClick={filter.onRemove}
              >
                ×
              </button>
            </Badge>
          ))}
          {activeFilters.length > 0 && clearFiltersLabel && onClearFilters && (
            <Button size="sm" variant="link" className="p-0" onClick={onClearFilters}>
              {clearFiltersLabel}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
