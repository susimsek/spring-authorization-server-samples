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
import { problemErrorCode, problemViolations } from "@/lib/problem-detail";

import { useAdminAuth } from "./AdminAuthProvider";
import { ErrorState, LoadingState } from "./AsyncState";
import { ConfirmModal } from "./ConfirmModal";

type User = {
  id: number;
  username: string;
  enabled: boolean;
  avatarUrl: string | null;
  authorities: string[];
};
type Role = { name: string };
type UserFormValues = { username: string; password: string; enabled: boolean; roles: string[] };

export function UserForm({
  locale,
  dictionary,
  id,
}: {
  locale: Locale;
  dictionary: Dictionary;
  id?: string | null;
}) {
  const router = useRouter();
  const { accessToken } = useAdminAuth();
  const copy = dictionary.admin.resources;
  const editing = Boolean(id);
  const [availableRoles, setAvailableRoles] = useState<Role[]>([]);
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [avatarSaving, setAvatarSaving] = useState(false);
  const [avatarError, setAvatarError] = useState<string | null>(null);
  const [showAvatarDeleteConfirm, setShowAvatarDeleteConfirm] = useState(false);
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const validation = dictionary.admin.common.validation;
  const schema = z.object({
    username: z.string().trim().min(1, validation.required),
    password: editing
      ? z.string().refine((value) => value === "" || value.length >= 8, validation.password)
      : z.string().min(8, validation.password),
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
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [accessToken, editing, id, reset]);

  useEffect(() => {
    if (!accessToken) return;
    adminRequest<Role[]>(accessToken, { url: "/api/admin/roles" })
      .then((response) => {
        if (response.status >= 300) throw new Error();
        setAvailableRoles(response.data);
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
      router.push(`/${locale}/admin/users`);
      router.refresh();
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState />;
  if (editing && error) return <ErrorState message={copy.notFound} />;
  return (
    <>
      <Form onSubmit={handleSubmit(submit)}>
        {error && <Alert variant="danger">{copy.saveError}</Alert>}
        <Card className="border-0 shadow-sm">
          <Card.Body className="d-grid gap-3">
            <Form.Group>
              <Form.Label>{copy.username}</Form.Label>
              <Form.Control isInvalid={Boolean(errors.username)} {...register("username")} />
              <Form.Control.Feedback type="invalid">
                {errors.username?.message}
              </Form.Control.Feedback>
            </Form.Group>
            {editing && (
              <Form.Group>
                <Form.Label>{copy.avatar}</Form.Label>
                <div className="d-flex align-items-center gap-2">
                  {avatarUrl && (
                    <Image
                      alt=""
                      className="rounded-circle object-fit-cover"
                      height={48}
                      src={avatarUrl}
                      unoptimized
                      width={48}
                    />
                  )}
                  <Form.Control
                    accept="image/jpeg,image/png"
                    aria-label={copy.uploadAvatar}
                    disabled={avatarSaving}
                    isInvalid={Boolean(avatarError)}
                    onChange={(event) =>
                      uploadAvatar((event.target as HTMLInputElement).files?.[0])
                    }
                    type="file"
                  />
                  {avatarUrl && (
                    <Button
                      disabled={avatarSaving}
                      onClick={() => setShowAvatarDeleteConfirm(true)}
                      size="sm"
                      type="button"
                      variant="outline-danger"
                    >
                      {copy.removeAvatar}
                    </Button>
                  )}
                </div>
                <Form.Text>{copy.avatarHelp}</Form.Text>
                <Form.Control.Feedback type="invalid">{avatarError}</Form.Control.Feedback>
              </Form.Group>
            )}
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
            <Form.Check
              type="switch"
              label={copy.enabled}
              checked={enabled}
              onChange={(event) => setValue("enabled", event.target.checked, { shouldDirty: true })}
            />
            <Form.Group>
              <Form.Label>{copy.roles}</Form.Label>
              <div>
                {availableRoles.map((role) => (
                  <Form.Check
                    inline
                    key={role.name}
                    type="checkbox"
                    label={role.name}
                    checked={roles.includes(role.name)}
                    onChange={() => toggleRole(role.name)}
                  />
                ))}
              </div>
              <Form.Text>{copy.rolesHelp}</Form.Text>
              {errors.roles && (
                <div className="invalid-feedback d-block">{errors.roles.message}</div>
              )}
            </Form.Group>
          </Card.Body>
        </Card>
        <div className="d-flex justify-content-end gap-2 mt-3">
          <Button variant="outline-secondary" onClick={() => router.push(`/${locale}/admin/users`)}>
            {dictionary.admin.common.cancel}
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? dictionary.admin.common.saving : dictionary.admin.common.save}
          </Button>
        </div>
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
