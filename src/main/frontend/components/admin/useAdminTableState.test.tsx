import { act, renderHook } from "@testing-library/react";
import { renderToString } from "react-dom/server";
const pathname = { value: "/en/admin/clients" };

jest.mock("next/navigation", () => ({
  usePathname: () => pathname.value,
}));

import { useAdminTableState } from "./useAdminTableState";

describe("useAdminTableState", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "/en/admin/clients?q=demo&page=2&size=50&status=active");
  });

  it("reads state from the URL and writes normalized changes", () => {
    const { result } = renderHook(() => useAdminTableState(20, true));

    expect(result.current).toMatchObject({ query: "demo", page: 2, size: 50, status: "active" });

    act(() => result.current.setQuery("next"));
    expect(window.location.search).toBe("?q=next&size=50&status=active");

    act(() => result.current.setPage(3));
    expect(Object.fromEntries(new URLSearchParams(window.location.search))).toEqual({
      q: "next",
      page: "3",
      size: "50",
      status: "active",
    });

    act(() => result.current.setSize(20));
    act(() => result.current.setStatus(""));
    expect(window.location.search).toBe("?q=next");
  });

  it("falls back to defaults for invalid query values", () => {
    window.history.replaceState(null, "", "/en/admin/clients?page=-1&size=invalid");

    const { result } = renderHook(() => useAdminTableState());

    expect(result.current).toMatchObject({ query: "", page: 0, size: 20, status: "" });
  });

  it("does not persist status when status filters are disabled", () => {
    window.history.replaceState(null, "", "/en/admin/clients?q=demo#details");

    const { result } = renderHook(() => useAdminTableState(20, false));

    act(() => result.current.setStatus("active"));

    expect(window.location.search).toBe("?q=demo");
    expect(window.location.hash).toBe("#details");
  });

  it("provides a server snapshot and cleans up subscriptions", () => {
    function State() {
      const state = useAdminTableState();
      return <span>{state.query}</span>;
    }
    const removeEventListener = jest.spyOn(window, "removeEventListener");

    expect(renderToString(<State />)).toBe("<span></span>");

    const { unmount } = renderHook(() => useAdminTableState());
    unmount();
    expect(removeEventListener).toHaveBeenCalledWith("admin-table-state", expect.any(Function));
    expect(removeEventListener).toHaveBeenCalledWith("popstate", expect.any(Function));
  });
});
