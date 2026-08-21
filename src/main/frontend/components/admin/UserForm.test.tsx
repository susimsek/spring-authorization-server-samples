/* eslint-disable @next/next/no-img-element, react/display-name */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import dictionary from "@/i18n/dictionaries/en.json";
import { adminRequest } from "@/lib/admin-api";

import { UserForm } from "./UserForm";

const mockPush = jest.fn();
const mockRefresh = jest.fn();
const mockAdminRequest = adminRequest as jest.MockedFunction<typeof adminRequest>;

jest.mock("@/lib/admin-api", () => ({ adminRequest: jest.fn() }));
jest.mock("./AdminAuthProvider", () => ({
  useAdminAuth: () => ({ accessToken: "token" }),
}));
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, refresh: mockRefresh }),
}));
jest.mock("next/image", () => ({ src, alt }: { src: string; alt: string }) => (
  <img alt={alt} src={src} />
));

describe("UserForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("loads available roles and creates a user", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles")
        return { status: 200, data: [{ name: "ROLE_USER" }] } as never;
      return { status: 201, data: { id: 1, username: "ada" } } as never;
    });

    render(<UserForm dictionary={dictionary} locale="en" />);
    expect((await screen.findAllByRole("checkbox"))[0]).toBeChecked();
    fireEvent.change(document.querySelector('input[name="username"]')!, {
      target: { value: "ada" },
    });
    fireEvent.change(document.querySelector('input[name="password"]')!, {
      target: { value: "password1" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));

    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/en/admin/users"));
    expect(mockRefresh).toHaveBeenCalled();
  });

  it("shows the avatar validation message before uploading an unsupported file", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles") return { status: 200, data: [] } as never;
      return {
        status: 200,
        data: {
          id: 1,
          username: "ada",
          enabled: true,
          authorities: ["ROLE_USER"],
          avatarUrl: null,
        },
      } as never;
    });

    render(<UserForm dictionary={dictionary} id="1" locale="en" />);
    expect(
      await screen.findByLabelText(dictionary.admin.resources.uploadAvatar),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(dictionary.admin.resources.uploadAvatar), {
      target: { files: [new File(["text"], "avatar.txt", { type: "text/plain" })] },
    });

    expect(await screen.findAllByText(dictionary.admin.resources.avatarHelp)).toHaveLength(2);
  });

  it("loads an existing user, toggles roles, updates the password, and removes the avatar", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles") {
        return { status: 200, data: [{ name: "ROLE_USER" }, { name: "ROLE_ADMIN" }] } as never;
      }
      if (config.url === "/api/admin/users/7") {
        return {
          status: 200,
          data: {
            id: 7,
            username: "ada",
            enabled: true,
            authorities: ["ROLE_USER"],
            avatarUrl: "/avatar.png",
          },
        } as never;
      }
      return { status: 200, data: {} } as never;
    });

    render(<UserForm dictionary={dictionary} id="7" locale="en" />);
    expect(await screen.findByDisplayValue("ada")).toBeVisible();
    expect(screen.getByAltText("")).toBeVisible();
    fireEvent.click(screen.getAllByRole("checkbox")[2]);
    fireEvent.change(document.querySelector('input[name="password"]')!, {
      target: { value: "new-password" },
    });
    fireEvent.click(screen.getAllByRole("checkbox")[0]);
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));

    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/users/7/password",
        method: "PUT",
        data: { password: "new-password" },
      }),
    );
    expect(mockPush).toHaveBeenCalledWith("/en/admin/users");

    fireEvent.click(
      screen.getAllByRole("button", { name: dictionary.admin.resources.removeAvatar })[0],
    );
    fireEvent.click(screen.getAllByRole("button", { name: dictionary.admin.common.cancel })[1]);
    fireEvent.click(
      screen.getAllByRole("button", { name: dictionary.admin.resources.removeAvatar })[0],
    );
    fireEvent.click(
      screen.getAllByRole("button", { name: dictionary.admin.resources.removeAvatar })[1],
    );
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/users/7/avatar",
        method: "DELETE",
      }),
    );
  });

  it("validates required password and roles on create", async () => {
    mockAdminRequest.mockResolvedValue({ status: 200, data: [{ name: "ROLE_USER" }] } as never);
    render(<UserForm dictionary={dictionary} locale="en" />);
    await screen.findAllByRole("checkbox");
    fireEvent.change(document.querySelector('input[name="username"]')!, {
      target: { value: "ada" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.cancel }));
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));
    expect(await screen.findByText(dictionary.admin.common.validation.password)).toBeVisible();

    fireEvent.change(document.querySelector('input[name="password"]')!, {
      target: { value: "password1" },
    });
    fireEvent.click(screen.getAllByRole("checkbox")[1]);
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));
    expect(await screen.findByText(dictionary.admin.common.validation.roles)).toBeVisible();
  });

  it("uploads a valid avatar and handles an avatar validation response", async () => {
    const createImageBitmapMock = jest.fn().mockResolvedValue({
      width: 100,
      height: 100,
      close: jest.fn(),
    });
    Object.assign(globalThis, { createImageBitmap: createImageBitmapMock });
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles") return { status: 200, data: [] } as never;
      if (config.url === "/api/admin/users/8") {
        return {
          status: 200,
          data: { id: 8, username: "ada", enabled: true, authorities: [], avatarUrl: null },
        } as never;
      }
      return { status: 200, data: { avatarUrl: "/new-avatar.png" } } as never;
    });

    render(<UserForm dictionary={dictionary} id="8" locale="en" />);
    const input = await screen.findByLabelText(dictionary.admin.resources.uploadAvatar);
    fireEvent.change(input, {
      target: { files: [new File(["image"], "avatar.png", { type: "image/png" })] },
    });
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith(
        "token",
        expect.objectContaining({
          url: "/api/admin/users/8/avatar",
          method: "PUT",
        }),
      ),
    );
    expect(createImageBitmapMock).toHaveBeenCalled();
  });

  it("maps duplicate username and password API violations", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles")
        return { status: 200, data: [{ name: "ROLE_USER" }] } as never;
      return {
        status: 400,
        data: {
          errorCode: "admin_user_duplicate_username",
          violations: [{ field: "username" }, { field: "roles" }],
        },
      } as never;
    });
    render(<UserForm dictionary={dictionary} locale="en" />);
    await screen.findAllByRole("checkbox");
    fireEvent.change(document.querySelector('input[name="username"]')!, {
      target: { value: "ada" },
    });
    fireEvent.blur(document.querySelector('input[name="username"]')!);
    fireEvent.change(document.querySelector('input[name="password"]')!, {
      target: { value: "password1" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));
    expect(
      await screen.findAllByText(dictionary.admin.common.validation.usernameDuplicate),
    ).toHaveLength(2);
  });

  it("rejects oversized images and bitmap decoding failures", async () => {
    const close = jest.fn();
    Object.assign(globalThis, {
      createImageBitmap: jest
        .fn()
        .mockResolvedValueOnce({ width: 5000, height: 100, close })
        .mockRejectedValueOnce(new Error("invalid image")),
    });
    mockAdminRequest.mockImplementation(async (_token, config) => {
      if (config.url === "/api/admin/roles") return { status: 200, data: [] } as never;
      return {
        status: 200,
        data: { id: 9, username: "ada", enabled: true, authorities: [], avatarUrl: null },
      } as never;
    });
    render(<UserForm dictionary={dictionary} id="9" locale="en" />);
    const input = await screen.findByLabelText(dictionary.admin.resources.uploadAvatar);
    const file = new File(["image"], "avatar.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => expect(createImageBitmap).toHaveBeenCalled());
    fireEvent.change(input, { target: { files: [file] } });
    expect(await screen.findByText(dictionary.admin.resources.avatarHelp)).toBeVisible();
  });

  it("maps avatar and password operation failures", async () => {
    mockAdminRequest.mockImplementation(async (_token, config) => {
      const url = config.url ?? "";
      if (url === "/api/admin/roles")
        return { status: 200, data: [{ name: "ROLE_USER" }] } as never;
      if (url === "/api/admin/users/10") {
        return {
          status: 200,
          data: {
            id: 10,
            username: "ada",
            enabled: true,
            authorities: ["ROLE_USER"],
            avatarUrl: "/a.png",
          },
        } as never;
      }
      if (url.endsWith("/avatar")) {
        return { status: 400, data: { violations: [{ field: "avatar" }] } } as never;
      }
      if (url.endsWith("/password")) {
        return { status: 400, data: { violations: [{ field: "password" }] } } as never;
      }
      return { status: 200, data: {} } as never;
    });
    Object.assign(globalThis, {
      createImageBitmap: jest.fn().mockResolvedValue({ width: 100, height: 100, close: jest.fn() }),
    });
    const view = render(<UserForm dictionary={dictionary} id="10" locale="en" />);
    const input = await screen.findByLabelText(dictionary.admin.resources.uploadAvatar);
    fireEvent.change(input, {
      target: { files: [new File(["image"], "avatar.png", { type: "image/png" })] },
    });
    expect(await screen.findByText(dictionary.admin.resources.avatarHelp)).toBeVisible();
    await waitFor(() => {
      const avatarFeedback = [...view.container.querySelectorAll(".invalid-feedback")].find(
        (element) => element.textContent === dictionary.admin.resources.avatarHelp,
      );
      expect(avatarFeedback).toHaveClass("d-block");
    });

    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.resources.removeAvatar }));
    fireEvent.click(
      screen.getAllByRole("button", { name: dictionary.admin.resources.removeAvatar })[1],
    );
    expect(await screen.findByText(dictionary.admin.resources.notFound)).toBeVisible();

    view.unmount();
    mockAdminRequest.mockImplementation(async (_token, config) => {
      const url = config.url ?? "";
      if (url === "/api/admin/roles")
        return { status: 200, data: [{ name: "ROLE_USER" }] } as never;
      if (url === "/api/admin/users/10") {
        return {
          status: 200,
          data: {
            id: 10,
            username: "ada",
            enabled: true,
            authorities: ["ROLE_USER"],
            avatarUrl: null,
          },
        } as never;
      }
      if (url.endsWith("/password")) {
        return { status: 400, data: { violations: [{ field: "password" }] } } as never;
      }
      return { status: 200, data: {} } as never;
    });
    render(<UserForm dictionary={dictionary} id="10" locale="en" />);
    await screen.findByDisplayValue("ada");

    fireEvent.change(document.querySelector('input[name="password"]')!, {
      target: { value: "new-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: dictionary.admin.common.save }));
    await waitFor(() =>
      expect(mockAdminRequest).toHaveBeenCalledWith("token", {
        url: "/api/admin/users/10/password",
        method: "PUT",
        data: { password: "new-password" },
      }),
    );
  });
});
