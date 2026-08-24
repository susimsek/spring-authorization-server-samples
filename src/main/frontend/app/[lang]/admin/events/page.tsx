"use client";

import { useEffect, useMemo, useState } from "react";
import { Badge, Button, Form, Offcanvas } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faFilter, faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import { useParams } from "next/navigation";
import { adminRequest } from "@/lib/admin-api";
import { useAdminAuth } from "@/components/admin/AdminAuthProvider";
import { DataTable } from "@/components/admin/DataTable";

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
  const tr = params.lang === "tr";
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
          <h1>{tr ? "Olaylar" : "Events"}</h1>
          <p>
            {tr
              ? "Yönetim ve hesap işlemlerinin denetim geçmişi."
              : "Audit history for administration and account actions."}
          </p>
        </div>
        <Badge bg="light" text="dark" className="border">
          {total} {tr ? "olay" : "events"}
        </Badge>
      </div>
      <div className="admin-event-toolbar mb-3">
        <div className="input-group admin-search-input">
          <span className="input-group-text">
            <FontAwesomeIcon icon={faMagnifyingGlass} />
          </span>
          <Form.Control
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={tr ? "Olaylarda ara" : "Search events"}
          />
        </div>
        <div className="input-group admin-event-type">
          <span className="input-group-text">
            <FontAwesomeIcon icon={faFilter} />
          </span>
          <Form.Select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">{tr ? "Tüm işlem tipleri" : "All event types"}</option>
            {types.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </Form.Select>
        </div>
        <Form.Select
          aria-label={tr ? "Hedef tipi" : "Target type"}
          value={targetType}
          onChange={(e) => setTargetType(e.target.value)}
        >
          <option value="">{tr ? "Tüm hedef tipleri" : "All target types"}</option>
          <option value="client">client</option>
          <option value="user">user</option>
          <option value="session">session</option>
          <option value="consent">consent</option>
          <option value="key">key</option>
          <option value="role">role</option>
          <option value="client-scope">client-scope</option>
        </Form.Select>
        <Form.Control
          value={targetId}
          onChange={(e) => setTargetId(e.target.value)}
          placeholder={tr ? "Hedef ID" : "Target ID"}
          aria-label={tr ? "Hedef ID" : "Target ID"}
        />
        <Form.Control
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          aria-label={tr ? "Başlangıç tarihi" : "From date"}
        />
        <Form.Control
          type="date"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          aria-label={tr ? "Bitiş tarihi" : "To date"}
        />
        {(query || type || targetType || targetId || from || to) && (
          <Button
            variant="link"
            onClick={() => {
              setQuery("");
              setType("");
              setTargetType("");
              setTargetId("");
              setFrom("");
              setTo("");
            }}
          >
            {tr ? "Filtreleri temizle" : "Clear filters"}
          </Button>
        )}
      </div>
      <DataTable
        isEmpty={filtered.length === 0}
        emptyMessage={tr ? "Olay bulunamadı." : "No events found."}
      >
        <thead>
          <tr>
            <th>{tr ? "Zaman" : "Time"}</th>
            <th>{tr ? "İşlem" : "Action"}</th>
            <th>{tr ? "Yapan" : "Actor"}</th>
            <th>{tr ? "Hedef" : "Target"}</th>
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
          <Offcanvas.Title>{tr ? "Olay ayrıntıları" : "Event details"}</Offcanvas.Title>
        </Offcanvas.Header>
        <Offcanvas.Body>
          {selected && (
            <dl className="admin-event-details">
              <dt>ID</dt>
              <dd className="font-monospace text-break">{selected.id}</dd>
              <dt>{tr ? "Zaman" : "Time"}</dt>
              <dd>{new Date(selected.occurredAt).toLocaleString(params.lang)}</dd>
              <dt>{tr ? "İşlem" : "Action"}</dt>
              <dd>
                <code>{selected.action}</code>
              </dd>
              <dt>{tr ? "Yapan" : "Actor"}</dt>
              <dd>{selected.actor}</dd>
              <dt>{tr ? "Hedef tipi" : "Target type"}</dt>
              <dd>{selected.targetType}</dd>
              <dt>{tr ? "Hedef" : "Target"}</dt>
              <dd className="text-break">{selected.targetId}</dd>
            </dl>
          )}
        </Offcanvas.Body>
      </Offcanvas>
    </>
  );
}
