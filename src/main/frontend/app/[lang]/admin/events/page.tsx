"use client";

import { useEffect, useMemo, useState } from "react";
import { Badge, Button, Form, Offcanvas } from "react-bootstrap";
import { useParams } from "next/navigation";
import { adminRequest } from "@/lib/admin-api";
import { useAdminAuth } from "@/components/admin/AdminAuthProvider";
import { DataTable } from "@/components/admin/DataTable";
import { ResourceFilters } from "@/components/admin/ResourceFilters";
import { getDictionary } from "@/i18n/get-dictionary";

export type Event = {
  id: string;
  actor: string;
  action: string;
  targetType: string;
  targetId: string;
  occurredAt: string;
};
type Page<T> = { content: T[]; totalElements: number };

export default function AdminEventsPage() {
  const { accessToken } = useAdminAuth();
  const params = useParams<{ lang: string }>();
  const dictionary = getDictionary(params.lang === "tr" ? "tr" : "en");
  const copy = dictionary.admin.events;
  const [events, setEvents] = useState<Event[]>([]);
  const [query, setQuery] = useState("");
  const [type, setType] = useState("");
  const [targetType, setTargetType] = useState("");
  const [targetId, setTargetId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [selected, setSelected] = useState<Event | null>(null);
  const [total, setTotal] = useState(0);
  useEffect(() => {
    if (!accessToken) return;
    const search = new URLSearchParams({
      size: "100",
      sort: "occurredAt,desc",
    });
    if (query.trim()) search.set("q", query.trim());
    if (type) search.set("action", type);
    if (targetType) search.set("targetType", targetType);
    if (targetId.trim()) search.set("targetId", targetId.trim());
    if (from) search.set("from", new Date(`${from}T00:00:00`).toISOString());
    if (to) search.set("to", new Date(`${to}T23:59:59.999`).toISOString());
    adminRequest<Page<Event>>(accessToken, {
      url: `/api/admin/events?${search}`,
    }).then((r) => {
      if (r.status < 300) {
        setEvents(r.data.content);
        setTotal(r.data.totalElements);
      }
    });
  }, [accessToken, query, type, targetType, targetId, from, to]);
  const types = useMemo(() => [...new Set(events.map((event) => event.action))].sort(), [events]);
  const filtered = events;
  return (
    <>
      <div className="admin-page-header d-flex flex-wrap justify-content-between gap-3 align-items-end">
        <div>
          <h1>{copy.title}</h1>
          <p>{copy.subtitle}</p>
        </div>
        <Badge bg="light" text="dark" className="border">
          {total} {copy.records}
        </Badge>
      </div>
      <ResourceFilters query={query} searchLabel={copy.search} onQueryChange={setQuery}>
        <Form.Select
          aria-label={copy.action}
          className="admin-resource-filter-control"
          value={type}
          onChange={(event) => setType(event.target.value)}
        >
          <option value="">{copy.allActions}</option>
          {types.map((item) => (
            <option key={item}>{item}</option>
          ))}
        </Form.Select>
        <Form.Select
          aria-label={copy.targetType}
          className="admin-resource-filter-control"
          value={targetType}
          onChange={(event) => setTargetType(event.target.value)}
        >
          <option value="">{copy.allTargetTypes}</option>
          <option value="client">client</option>
          <option value="user">user</option>
          <option value="session">session</option>
          <option value="consent">consent</option>
          <option value="key">key</option>
          <option value="role">role</option>
          <option value="client-scope">client-scope</option>
        </Form.Select>
        <Form.Control
          className="admin-resource-filter-control"
          value={targetId}
          onChange={(event) => setTargetId(event.target.value)}
          placeholder={copy.targetId}
          aria-label={copy.targetId}
        />
        <Form.Control
          className="admin-resource-filter-control"
          type="date"
          value={from}
          onChange={(event) => setFrom(event.target.value)}
          aria-label={copy.from}
        />
        <Form.Control
          className="admin-resource-filter-control"
          type="date"
          value={to}
          onChange={(event) => setTo(event.target.value)}
          aria-label={copy.to}
        />
        {(query || type || targetType || targetId || from || to) && (
          <Button
            variant="outline-secondary"
            onClick={() => {
              setQuery("");
              setType("");
              setTargetType("");
              setTargetId("");
              setFrom("");
              setTo("");
            }}
          >
            {copy.clearFilters}
          </Button>
        )}
      </ResourceFilters>
      <DataTable isEmpty={filtered.length === 0} emptyMessage={copy.empty}>
        <thead>
          <tr>
            <th>{copy.time}</th>
            <th>{copy.action}</th>
            <th>{copy.actor}</th>
            <th>{copy.target}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {filtered.map((e) => (
            <tr className="admin-clickable-row" key={e.id} onClick={() => setSelected(e)}>
              <td>{new Date(e.occurredAt).toLocaleString(params.lang)}</td>
              <td>
                <Badge bg="light" text="dark" className="border font-monospace">
                  {e.action}
                </Badge>
              </td>
              <td>{e.actor}</td>
              <td>
                <div>{e.targetType}</div>
                <div className="small text-body-secondary text-break">{e.targetId}</div>
              </td>
              <td className="text-end text-body-secondary">›</td>
            </tr>
          ))}
        </tbody>
      </DataTable>
      <Offcanvas
        placement="end"
        show={selected !== null}
        onHide={() => setSelected(null)}
        className="admin-event-drawer"
      >
        <Offcanvas.Header closeButton>
          <Offcanvas.Title>{copy.details}</Offcanvas.Title>
        </Offcanvas.Header>
        <Offcanvas.Body>
          {selected && (
            <dl className="admin-event-details">
              <dt>ID</dt>
              <dd className="font-monospace text-break">{selected.id}</dd>
              <dt>{copy.time}</dt>
              <dd>{new Date(selected.occurredAt).toLocaleString(params.lang)}</dd>
              <dt>{copy.action}</dt>
              <dd>
                <code>{selected.action}</code>
              </dd>
              <dt>{copy.actor}</dt>
              <dd>{selected.actor}</dd>
              <dt>{copy.targetType}</dt>
              <dd>{selected.targetType}</dd>
              <dt>{copy.target}</dt>
              <dd className="text-break">{selected.targetId}</dd>
            </dl>
          )}
        </Offcanvas.Body>
      </Offcanvas>
    </>
  );
}
