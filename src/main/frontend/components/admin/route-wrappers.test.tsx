import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import dictionary from "@/locales/en/common.json";
import { ClientEntityRoute } from "./ClientEntityRoute";
import { UserEntityRoute } from "./UserEntityRoute";

jest.mock("./ClientDetail", () => ({
  ClientDetail: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));
jest.mock("./UserForm", () => ({
  UserForm: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));

describe("runtime entity routing", () => {
  it.each([
    ["/admin/clients/abc", "abc", "settings"],
    ["/admin/clients/client-1/credentials", "client-1", "credentials"],
    ["/admin/clients/client%20with%20spaces/sessions", "client with spaces", "sessions"],
    ["/admin/clients/client-2/unknown", "client-2", "settings"],
    ["/admin/users/123", "123", "details"],
    ["/admin/users/42/sessions", "42", "sessions"],
  ])("resolves %s without any build-time parameters", (path, id, tab) => {
    render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="/admin/clients/:id/:section?"
            element={<ClientEntityRoute locale="en" dictionary={dictionary} />}
          />
          <Route
            path="/admin/users/:id/:section?"
            element={<UserEntityRoute locale="en" dictionary={dictionary} />}
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText(new RegExp('"id":"' + id + '"'))).toHaveTextContent(
      '"tab":"' + tab + '"',
    );
  });
});
