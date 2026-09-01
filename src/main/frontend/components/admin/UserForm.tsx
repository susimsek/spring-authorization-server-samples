"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { Alert, Button, Card, Form } from "react-bootstrap";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/get-dictionary";
import { adminRequest } from "@/lib/admin-api";
import type { PageResponse } from "@/lib/api-types";
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { AdminActionIcon } from "./AdminActionIcon";
import { ErrorState, LoadingState } from "./AsyncState";
import { ConfirmModal } from "./ConfirmModal";
import { DetailTabs } from "./DetailTabs";
import { EntityRelatedData } from "./EntityRelatedData";
import { AdminBreadcrumb } from "./AdminBreadcrumb";
import { ReadOnlyMetadata } from "./ReadOnlyMetadata";
import { UserGroups } from "./UserGroups";

type User = {
  id: number;
  username: string;
  enabled: boolean;
  avatarUrl: string | null;
  authorities: string[];
  createdAt: string;
  updatedAt: string;
};
type Role = { name: string };
type UserFormValues = { username: string; password: string; enabled: boolean; roles: string[] };

const USER_DETAIL_TABS = [
  "details",
  "credentials",
  "roles",
  "groups",
  "sessions",
  "consents",
  "events",
] as const;

export function UserForm({
  locale,
  dictionary,
  id,
  tab = "details",
}: {
  locale: Locale;
  dictionary: Dictionary;
  id?: string | null;
  tab?: string;
}) {
  const router = useRouter();
  const { access, accessToken } = useAdminAuth();
  const copy = dictionary.admin.resources;
  const editing = Boolean(id);
  const [availableRoles, setAvailableRoles] = useState<Role[]>([]);
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [createdAt, setCreatedAt] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [avatarSaving, setAvatarSaving] = useState(false);
  const [avatarError, setAvatarError] = useState<string | null>(null);
  const [showAvatarDeleteConfirm, setShowAvatarDeleteConfirm] = useState(false);
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const activeTab = USER_DETAIL_TABS.includes(tab as (typeof USER_DETAIL_TABS)[number])
    ? tab
    : "details";
  const validation = dictionary.admin.common.validation;
  const schema = z.object({
    username: z.string().trim().min(1, validation.required).max(100, validation.max100),
    password: editing
      ? z
          .string()
          .max(200, validation.max200)
          .refine((value) => value === "" || value.length >= 8, validation.password)
      : z.string().min(8, validation.password).max(200, validation.max200),
    enabled: z.boolean(),
    roles: z.array(z.string()).min(1, validation.roles),
  });
  const {
    register,
    handleSubmit,
    getValues,
    reset,
    setValue,
    setError: setFieldError,
    control,
    formState: { errors },
  } = useForm<UserFormValues>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { username: "", password: "", enabled: true, roles: ["ROLE_USER"] },
  });
  const enabled = useWatch({ control, name: "enabled", defaultValue: true });
  const roles = useWatch({ control, name: "roles", defaultValue: ["ROLE_USER"] });

  useEffect(() => {
    if (!editing || !id || !accessToken) return;
    adminRequest<User>(accessToken, { url: `/api/admin/users/${encodeURIComponent(id)}` })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        reset({
          username: response.data.username,
          password: "",
          enabled: response.data.enabled,
          roles: response.data.authorities,
        });
        setAvatarUrl(response.data.avatarUrl);
        setCreatedAt(response.data.createdAt);
        setUpdatedAt(response.data.updatedAt);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, editing, id, reset]);

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<PageResponse<Role>>(accessToken, { url: "/api/admin/roles?page=0&size=100" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setAvailableRoles(response.data.content);
      })
      .catch(() => setError(true));
  }, [accessToken]);

  const toggleRole = (role: string) => {
    const roles = getValues("roles");
    setValue(
      "roles",
      roles.includes(role) ? roles.filter((value) => value !== role) : [...roles, role],
      { shouldDirty: true, shouldValidate: true },
    );
  };

  const uploadAvatar = async (file: File | undefined) => {
    if (!file || !accessToken || !id) return;
    setAvatarError(null);
    if (!["image/jpeg", "image/png"].includes(file.type) || file.size > 2 * 1024 * 1024) {
      setAvatarError(copy.avatarHelp);
      return;
    }
    try {
      const image = await createImageBitmap(file);
      const tooLarge =
        image.width > 4096 || image.height > 4096 || image.width * image.height > 4_000_000;
      image.close();
      if (tooLarge) {
        setAvatarError(copy.avatarHelp);
        return;
      }
    } catch {
      setAvatarError(copy.avatarHelp);
      return;
    }
    setAvatarSaving(true);
    setError(false);
    try {
      const data = new FormData();
      data.append("file", file);
      const response = await adminRequest<User>(accessToken, {
        url: `/api/admin/users/${encodeURIComponent(id)}/avatar`,
        method: "PUT",
        data,
      });
      if (response.status >= 300) {
        if (problemViolations(response.data).some(({ field }) => field === "avatar")) {
          setAvatarError(copy.avatarHelp);
          return;
        }
        throw new Error();
      }
      setAvatarUrl(response.data.avatarUrl);
    } catch {
      setError(true);
    } finally {
      setAvatarSaving(false);
    }
  };

  const removeAvatar = async () => {
    if (!accessToken || !id) return;
    setAvatarSaving(true);
    setError(false);
    try {
      const response = await adminRequest(accessToken, {
        url: `/api/admin/users/${encodeURIComponent(id)}/avatar`,
        method: "DELETE",
      });
      if (response.status >= 300) throw new Error();
      setAvatarUrl(null);
    } catch {
      setError(true);
    } finally {
      setAvatarSaving(false);
    }
  };

  const submit = async (values: UserFormValues) => {
    if (!accessToken) return;
    setSaving(true);
    setError(false);
    try {
      const response = await adminRequest<User>(accessToken, {
        url: editing ? `/api/admin/users/${encodeURIComponent(id ?? "")}` : "/api/admin/users",
        method: editing ? "PUT" : "POST",
        data: {
          username: values.username,
          password: editing ? undefined : values.password,
          enabled: values.enabled,
          roles: values.roles,
        },
      });
      if (response.status >= 300) {
        const errorCode = problemErrorCode(response.data);
        problemViolations(response.data).forEach(({ field }) => {
          const message =
            errorCode === "admin_user_duplicate_username"
              ? validation.usernameDuplicate
              : field === "password"
                ? validation.password
                : field === "roles"
                  ? validation.roles
                  : validation.required;
          setFieldError(field as keyof UserFormValues, { message });
        });
        throw new Error();
      }
      if (editing && values.password) {
        const passwordResponse = await adminRequest(accessToken, {
          url: `/api/admin/users/${encodeURIComponent(id ?? "")}/password`,
          method: "PUT",
          data: { password: values.password },
        });
        if (passwordResponse.status >= 300) {
          if (problemViolations(passwordResponse.data).some(({ field }) => field === "password")) {
            setFieldError("password", { message: validation.password });
          }
          throw new Error();
        }
      }
      router.push(
        editing && id
          ? `/${locale}/admin/users/${encodeURIComponent(id)}/${tab}`
          : `/${locale}/admin/users`,
      );
      router.refresh();
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState />;
  if (editing && error) return <ErrorState message={copy.notFound} />;
  const userBaseUrl = `/${locale}/admin/users/${encodeURIComponent(id ?? "")}`;
  const userTabs = [
    { key: "details", label: copy.details, href: `${userBaseUrl}/details` },
    {
      key: "credentials",
      label: copy.credentials,
      href: `${userBaseUrl}/credentials`,
    },
    { key: "roles", label: copy.roleMappings, href: `${userBaseUrl}/roles` },
    { key: "groups", label: dictionary.admin.nav.groups, href: `${userBaseUrl}/groups` },
    { key: "sessions", label: copy.sessions, href: `${userBaseUrl}/sessions` },
    { key: "consents", label: copy.consents, href: `${userBaseUrl}/consents` },
    { key: "events", label: dictionary.admin.events.title, href: `${userBaseUrl}/events` },
  ];
  return (
    <>
      {editing && (
        <div className="admin-user-detail-header">
          <AdminBreadcrumb
            items={[
              { label: copy.users, href: `/${locale}/admin/users` },
              { label: getValues("username") },
            ]}
          />
          <div className="admin-detail-heading">
            <div>
              <h1 className="h3 mb-1">{getValues("username")}</h1>
              <div className="text-body-secondary">{copy.user}</div>
            </div>
          </div>
          <DetailTabs tabs={userTabs} active={activeTab} />
        </div>
      )}
      <Form className={editing ? undefined : "admin-create-form"} onSubmit={handleSubmit(submit)}>
        {error && <Alert variant="danger">{copy.saveError}</Alert>}

        {!editing && (
          <Card className="admin-panel-card admin-create-card">
            <Card.Body className="d-grid gap-4">
              <Form.Group>
                <Form.Label>{copy.username}</Form.Label>
                <Form.Control isInvalid={Boolean(errors.username)} {...register("username")} />
                <Form.Control.Feedback type="invalid">
                  {errors.username?.message}
                </Form.Control.Feedback>
              </Form.Group>
              <Form.Check
                type="switch"
                label={copy.enabled}
                checked={enabled}
                onChange={(e) => setValue("enabled", e.target.checked, { shouldDirty: true })}
              />
              <Form.Group>
                <Form.Label>{copy.password}</Form.Label>
                <Form.Control
                  type="password"
                  isInvalid={Boolean(errors.password)}
                  {...register("password")}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.password?.message}
                </Form.Control.Feedback>
              </Form.Group>
              <Form.Group>
                <Form.Label>{copy.roles}</Form.Label>
                <div className="admin-role-grid">
                  {availableRoles.map((role) => (
                    <label
                      className={`admin-role-option ${roles.includes(role.name) ? "selected" : ""}`}
                      key={role.name}
                    >
                      <Form.Check
                        type="checkbox"
                        checked={roles.includes(role.name)}
                        onChange={() => toggleRole(role.name)}
                      />
                      <span className="font-monospace">{role.name}</span>
                    </label>
                  ))}
                </div>
                <Form.Text>{copy.rolesHelp}</Form.Text>
                {errors.roles && (
                  <div className="invalid-feedback d-block">{errors.roles.message}</div>
                )}
              </Form.Group>
            </Card.Body>
          </Card>
        )}

        {editing && activeTab === "details" && (
          <Card className="admin-panel-card">
            <Card.Body className="d-grid gap-4">
              {editing && (
                <div className="d-flex align-items-center gap-3 pb-3 border-bottom">
                  {avatarUrl ? (
                    <Image
                      alt=""
                      className="rounded-circle object-fit-cover admin-user-avatar"
                      height={64}
                      src={avatarUrl}
                      unoptimized
                      width={64}
                    />
                  ) : (
                    <span className="avatar-placeholder admin-user-avatar">
                      {getValues("username").slice(0, 1).toUpperCase()}
                    </span>
                  )}
                  <div className="flex-grow-1 min-w-0">
                    <div className="fw-semibold">{getValues("username")}</div>
                    <div className="small text-body-secondary">{copy.avatarHelp}</div>
                  </div>
                  <label className="btn btn-sm btn-outline-primary mb-0">
                    {copy.uploadAvatar}
                    <input
                      className="visually-hidden"
                      accept="image/jpeg,image/png"
                      disabled={avatarSaving}
                      onChange={(e) => uploadAvatar(e.target.files?.[0])}
                      type="file"
                    />
                  </label>
                  {avatarUrl && (
                    <Button
                      size="sm"
                      variant="outline-danger"
                      disabled={avatarSaving}
                      onClick={() => setShowAvatarDeleteConfirm(true)}
                      type="button"
                    >
                      <AdminActionIcon action="delete" />
                      {copy.removeAvatar}
                    </Button>
                  )}
                </div>
              )}
              {avatarError && (
                <Alert variant="danger" className="mb-0">
                  {avatarError}
                </Alert>
              )}
              <Form.Group>
                <Form.Label>{copy.username}</Form.Label>
                <Form.Control isInvalid={Boolean(errors.username)} {...register("username")} />
                <Form.Control.Feedback type="invalid">
                  {errors.username?.message}
                </Form.Control.Feedback>
              </Form.Group>
              <Form.Check
                type="switch"
                label={copy.enabled}
                checked={enabled}
                onChange={(e) => setValue("enabled", e.target.checked, { shouldDirty: true })}
              />
              {editing && createdAt && updatedAt && (
                <div className="account-metadata-panel">
                  <ReadOnlyMetadata
                    items={[
                      { label: copy.createdAt, value: new Date(createdAt).toLocaleString(locale) },
                      { label: copy.updatedAt, value: new Date(updatedAt).toLocaleString(locale) },
                    ]}
                  />
                </div>
              )}
            </Card.Body>
          </Card>
        )}

        {editing && activeTab === "credentials" && (
          <Card className="admin-panel-card">
            <Card.Body>
              <h2 className="h5 mb-1">{editing ? copy.resetPassword : copy.password}</h2>
              <p className="small text-body-secondary mb-3">
                {editing ? copy.resetPasswordHelp : ""}
              </p>
              <Form.Group>
                <Form.Label>{editing ? copy.newPassword : copy.password}</Form.Label>
                <Form.Control
                  type="password"
                  isInvalid={Boolean(errors.password)}
                  {...register("password")}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.password?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Card.Body>
          </Card>
        )}

        {editing && activeTab === "roles" && (
          <Card className="admin-panel-card">
            <Card.Body>
              <h2 className="h5 mb-3">{copy.roles}</h2>
              <div className="admin-role-grid">
                {availableRoles.map((role) => (
                  <label
                    className={`admin-role-option ${roles.includes(role.name) ? "selected" : ""}`}
                    key={role.name}
                  >
                    <Form.Check
                      type="checkbox"
                      checked={roles.includes(role.name)}
                      onChange={() => toggleRole(role.name)}
                    />
                    <span className="font-monospace">{role.name}</span>
                  </label>
                ))}
              </div>
              <Form.Text>{copy.rolesHelp}</Form.Text>
              {errors.roles && (
                <div className="invalid-feedback d-block">{errors.roles.message}</div>
              )}
            </Card.Body>
          </Card>
        )}

        {editing && id && activeTab === "groups" && (
          <UserGroups dictionary={dictionary} userId={id} />
        )}

        {editing && id && activeTab === "sessions" && (
          <EntityRelatedData
            resource="sessions"
            url={`/api/admin/users/${encodeURIComponent(id)}/sessions`}
            locale={locale}
            dictionary={dictionary}
            canManage={access?.manageSessions ?? false}
          />
        )}
        {editing && id && activeTab === "consents" && (
          <EntityRelatedData
            resource="consents"
            url={`/api/admin/users/${encodeURIComponent(id)}/consents`}
            locale={locale}
            dictionary={dictionary}
            canManage={access?.manageConsents ?? false}
          />
        )}
        {editing && id && activeTab === "events" && (
          <EntityRelatedData
            resource="events"
            url={`/api/admin/users/${encodeURIComponent(id)}/events`}
            locale={locale}
            dictionary={dictionary}
          />
        )}

        {(!editing || ["details", "credentials", "roles"].includes(activeTab)) && (
          <div className={`admin-create-actions${editing ? " mt-3" : ""}`}>
            <Button
              type="button"
              variant="outline-secondary"
              onClick={() => router.push(`/${locale}/admin/users`)}
            >
              {dictionary.admin.common.cancel}
            </Button>
            <Button type="submit" disabled={saving}>
              <AdminActionIcon action="save" />
              {saving ? dictionary.admin.common.saving : dictionary.admin.common.save}
            </Button>
          </div>
        )}
      </Form>
      <ConfirmModal
        busy={avatarSaving}
        cancelLabel={dictionary.admin.common.cancel}
        confirmLabel={copy.removeAvatar}
        message={copy.removeAvatarConfirm}
        onCancel={() => setShowAvatarDeleteConfirm(false)}
        onConfirm={() => {
          setShowAvatarDeleteConfirm(false);
          void removeAvatar();
        }}
        show={showAvatarDeleteConfirm}
      />
    </>
  );
}
