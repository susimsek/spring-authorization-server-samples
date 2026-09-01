import { render, screen } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";

import { ClientEntityRoute } from "./ClientEntityRoute";
import { UserEntityRoute } from "./UserEntityRoute";

let pathname = "/en/admin/clients/client-1/settings";

jest.mock("next/navigation", () => ({
  useParams: () => ({ lang: "en" }),
  usePathname: () => pathname,
}));
jest.mock("./ClientDetail", () => ({
  ClientDetail: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));
jest.mock("./UserForm", () => ({
  UserForm: (props: unknown) => <pre>{JSON.stringify(props)}</pre>,
}));

describe("admin entity route wrappers", () => {
  it("reads the client id and section from the dynamic path", () => {
    pathname = "/en/admin/clients/client-1/credentials";
    render(<ClientEntityRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/client-1/)).toHaveTextContent('"id":"client-1"');
    expect(screen.getByText(/client-1/)).toHaveTextContent('"tab":"credentials"');
  });

  it("defaults an unknown client section to settings", () => {
    pathname = "/en/admin/clients/client-2/unknown";
    render(<ClientEntityRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/client-2/)).toHaveTextContent('"tab":"settings"');
  });

  it("reads the user id and section from the dynamic path", () => {
    pathname = "/en/admin/users/42/sessions";
    render(<UserEntityRoute locale="en" dictionary={dictionary} />);
    expect(screen.getByText(/42/)).toHaveTextContent('"id":"42"');
    expect(screen.getByText(/42/)).toHaveTextContent('"tab":"sessions"');
  });
});
