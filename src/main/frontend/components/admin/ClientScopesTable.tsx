"use client";

import { useEffect, useState } from "react";
import { Button, Form, Modal } from "react-bootstrap";

import { useConsoleAlerts } from "@/components/auth/ConsoleAlerts";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminPageHeader } from "./AdminPageHeader";
import { ConfirmModal } from "./ConfirmModal";
import { DataTable } from "./DataTable";
import { ErrorState, LoadingState } from "./AsyncState";
import { PaginationControls } from "./PaginationControls";
import { RowActions } from "./RowActions";

export type ClientScope = {
  id: string;
  name: string;
  displayName: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
};

type PageData<T> = {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
};

type Values = { name: string; displayName: string; description: string };

const EMPTY: Values = { name: "", displayName: "", description: "" };

export function ClientScopesTable({ dictionary }: { dictionary: Dictionary }) {
  const copy = dictionary.admin.clientScopes;
  const common = dictionary.admin.common;
  const { access, accessToken } = useAdminAuth();
  const { addAlert, addError } = useConsoleAlerts();
  const [items, setItems] = useState<ClientScope[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState<ClientScope | null>(null);
  const [showEditor, setShowEditor] = useState(false);
  const [values, setValues] = useState<Values>(EMPTY);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<ClientScope | null>(null);
  const scopeValidation = {
    name: values.name.trim().length > 0 && values.name.length <= 100,
    displayName: values.displayName.length <= 200,
    description: values.description.length <= 500,
  };
  const scopeValid =
    scopeValidation.name && scopeValidation.displayName && scopeValidation.description;

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<PageData<ClientScope>>(accessToken, {
      url: `/api/admin/client-scopes?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setItems(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setFailed(false);
      })
      .catch(() => setFailed(true))
      .finally(() => setLoading(false));
  }, [accessToken, page, query, reload, size]);

  const openCreate = () => {
    setEditing(null);
    setValues(EMPTY);
    setShowEditor(true);
  };
  const openEdit = (scope: ClientScope) => {
    setEditing(scope);
    setValues({
      name: scope.name,
      displayName: scope.displayName ?? "",
      description: scope.description ?? "",
    });
    setShowEditor(true);
  };
  const save = async () => {
    if (!accessToken || !scopeValid) return;
    setSaving(true);
    const response = await adminRequest<ClientScope>(accessToken, {
      url: editing
        ? `/api/admin/client-scopes/${encodeURIComponent(editing.id)}`
        : "/api/admin/client-scopes",
      method: editing ? "PUT" : "POST",
      data: values,
    });
    setSaving(false);
    if (response.status >= 300) {
      addError(copy.operationError);
      return;
    }
    setShowEditor(false);
    addAlert(copy.saved);
    setReload((current) => current + 1);
  };
  const remove = async () => {
    if (!accessToken || !deleting) return;
    const response = await adminRequest(accessToken, {
      url: `/api/admin/client-scopes/${encodeURIComponent(deleting.id)}`,
      method: "DELETE",
    });
    setDeleting(null);
    if (response.status >= 300) {
      addError(response.status === 400 ? copy.assignedDeleteError : copy.operationError);
      return;
    }
    addAlert(copy.deleted);
    setReload((current) => current + 1);
  };

  if (loading && items.length === 0) return <LoadingState />;
  if (failed) return <ErrorState message={copy.operationError} />;

  return (
    <>
      <AdminPageHeader
        title={copy.title}
        description={copy.subtitle}
        actions={
          access?.manageClients ? <Button onClick={openCreate}>{copy.create}</Button> : undefined
        }
      />
      <Form
        className="admin-resource-toolbar"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          setQuery(search.trim());
        }}
      >
        <Form.Control
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={copy.search}
          aria-label={copy.search}
        />
        <Button type="submit" variant="outline-secondary">
          {dictionary.admin.resources.search}
        </Button>
      </Form>
      <DataTable
        isEmpty={items.length === 0}
        emptyMessage={copy.empty}
        footer={
          <PaginationControls
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            size={size}
            rowsPerPage={dictionary.admin.resources.rowsPerPage}
            pageLabel={dictionary.admin.resources.page}
            previous={dictionary.admin.resources.previous}
            next={dictionary.admin.resources.next}
            first={dictionary.admin.resources.first}
            last={dictionary.admin.resources.last}
            onPageChange={setPage}
            onSizeChange={(nextSize) => {
              setPage(0);
              setSize(nextSize);
            }}
          />
        }
      >
        <thead>
          <tr>
            <th>{copy.name}</th>
            <th>{copy.displayName}</th>
            <th>{copy.description}</th>
            <th className="admin-actions-column" />
          </tr>
        </thead>
        <tbody>
          {items.map((scope) => (
            <tr key={scope.id}>
              <td className="font-monospace fw-semibold">{scope.name}</td>
              <td>{scope.displayName || "—"}</td>
              <td className="text-body-secondary">{scope.description || "—"}</td>
              <td className="text-end">
                {access?.manageClients && (
                  <RowActions label={`${scope.name} actions`}>
                    <button className="dropdown-item" type="button" onClick={() => openEdit(scope)}>
                      {copy.edit}
                    </button>
                    <div className="dropdown-divider" />
                    <button
                      className="dropdown-item text-danger"
                      type="button"
                      onClick={() => setDeleting(scope)}
                    >
                      {copy.delete}
                    </button>
                  </RowActions>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>

      <Modal show={showEditor} onHide={() => setShowEditor(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>{editing ? copy.edit : copy.create}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>{copy.name}</Form.Label>
            <Form.Control
              value={values.name}
              isInvalid={!scopeValidation.name}
              onChange={(event) =>
                setValues((current) => ({ ...current, name: event.target.value }))
              }
              autoFocus
            />
            <Form.Control.Feedback type="invalid">
              {!values.name.trim() ? common.validation.required : common.validation.max100}
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>{copy.displayName}</Form.Label>
            <Form.Control
              value={values.displayName}
              isInvalid={!scopeValidation.displayName}
              onChange={(event) =>
                setValues((current) => ({ ...current, displayName: event.target.value }))
              }
            />
            <Form.Control.Feedback type="invalid">{common.validation.max200}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group>
            <Form.Label>{copy.description}</Form.Label>
            <Form.Control
              as="textarea"
              isInvalid={!scopeValidation.description}
              rows={3}
              value={values.description}
              onChange={(event) =>
                setValues((current) => ({ ...current, description: event.target.value }))
              }
            />
            <Form.Control.Feedback type="invalid">{common.validation.max500}</Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setShowEditor(false)}>
            {common.cancel}
          </Button>
          <Button disabled={saving || !scopeValid} onClick={() => void save()}>
            {saving ? common.saving : common.save}
          </Button>
        </Modal.Footer>
      </Modal>

      <ConfirmModal
        show={deleting !== null}
        message={copy.deleteConfirm}
        cancelLabel={common.cancel}
        confirmLabel={copy.delete}
        onCancel={() => setDeleting(null)}
        onConfirm={() => void remove()}
      />
    </>
  );
}
