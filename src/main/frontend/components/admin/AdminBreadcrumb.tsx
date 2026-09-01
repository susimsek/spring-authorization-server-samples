"use client";

import Link from "next/link";
import { useParams } from "next/navigation";

import { getDictionary } from "@/i18n/get-dictionary";

export type AdminBreadcrumbItem = {
  label: string;
  href?: string;
};

export function AdminBreadcrumb({ items }: { items: AdminBreadcrumbItem[] }) {
  const params = useParams<{ lang: string }>();
  const breadcrumbLabel = getDictionary(params?.lang === "tr" ? "tr" : "en").admin.common
    .breadcrumb;
  if (items.length === 0) return null;
  return (
    <nav className="admin-breadcrumb" aria-label={breadcrumbLabel}>
      <ol className="breadcrumb mb-0">
        {items.map((item, index) => {
          const current = index === items.length - 1;
          return (
            <li
              className={`breadcrumb-item${current ? " active" : ""}`}
              aria-current={current ? "page" : undefined}
              key={`${item.label}-${index}`}
            >
              {!current && item.href ? (
                <Link href={item.href} className="text-decoration-none">
                  {item.label}
                </Link>
              ) : (
                item.label
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
