import { render, screen } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";

import { ClientDetailRoute } from "./ClientDetailRoute";
import { ClientFormRoute } from "./ClientFormRoute";
import { UserFormRoute } from "./UserFormRoute";

jest.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams("id=client-1"),
}));
jest.mock("./ClientForm", () => ({
  ClientForm: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));
jest.mock("./ClientDetail", () => ({
  ClientDetail: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));
jest.mock("./UserForm", () => ({
  UserForm: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));

describe("admin route wrappers", () => {
  it("passes the client id and edit mode to ClientForm", () => {
    render(<ClientFormRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/client-1/)).toHaveTextContent('"mode":"edit"');
    expect(screen.getByText(/client-1/)).toHaveTextContent('"id":"client-1"');
  });

  it("passes the id to ClientDetail", () => {
    render(<ClientDetailRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/client-1/)).toHaveTextContent('"id":"client-1"');
  });

  it("passes the id to UserForm", () => {
    render(<UserFormRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/client-1/)).toHaveTextContent('"id":"client-1"');
  });
});
