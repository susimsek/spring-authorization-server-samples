"use client";

import { useEffect, useSyncExternalStore } from "react";
import { usePathname } from "next/navigation";

type TableState = {
  query: string;
  page: number;
  size: number;
  status: string;
};

export function useAdminTableState(defaultSize = 20, includesStatus = false) {
  const pathname = usePathname();
  useEffect(() => {
    window.dispatchEvent(new Event("admin-table-state"));
  }, [pathname]);
  const location = useSyncExternalStore(
    subscribe,
    () => `${pathname}${window.location.search}`,
    () => "",
  );
  const state = readState(location, defaultSize, includesStatus);

  const update = (changes: Partial<TableState>) => {
    const next = { ...state, ...changes };
    const params = new URLSearchParams(window.location.search);
    setParam(params, "q", next.query);
    setParam(params, "page", next.page === 0 ? "" : String(next.page));
    setParam(params, "size", next.size === defaultSize ? "" : String(next.size));
    if (includesStatus) setParam(params, "status", next.status);
    const search = params.toString();
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${search ? `?${search}` : ""}${window.location.hash}`,
    );
    window.dispatchEvent(new Event("admin-table-state"));
  };

  return {
    ...state,
    setQuery: (query: string) => update({ query, page: 0 }),
    setPage: (page: number) => update({ page }),
    setSize: (size: number) => update({ size, page: 0 }),
    setStatus: (status: string) => update({ status, page: 0 }),
  };
}

function subscribe(onStoreChange: () => void) {
  window.addEventListener("admin-table-state", onStoreChange);
  window.addEventListener("popstate", onStoreChange);
  return () => {
    window.removeEventListener("admin-table-state", onStoreChange);
    window.removeEventListener("popstate", onStoreChange);
  };
}

function readState(location: string, defaultSize: number, includesStatus: boolean): TableState {
  const params = new URLSearchParams(location.split("?")[1] ?? "");
  const page = Number(params.get("page"));
  const size = Number(params.get("size"));
  return {
    query: params.get("q") ?? "",
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: Number.isInteger(size) && size > 0 ? size : defaultSize,
    status: includesStatus ? (params.get("status") ?? "") : "",
  };
}

function setParam(params: URLSearchParams, name: string, value: string) {
  if (value) {
    params.set(name, value);
  } else {
    params.delete(name);
  }
}
