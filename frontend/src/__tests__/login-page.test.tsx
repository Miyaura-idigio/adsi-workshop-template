import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import LoginPage from "@/app/login/page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: vi.fn(),
    push: vi.fn(),
  }),
}));

const mockLogin = vi.fn();
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => ({
    login: mockLogin,
    user: null,
    loading: false,
    logout: vi.fn(),
  }),
}));

vi.mock("@/lib/api-client", () => ({
  withBasePath: (path: string) => path,
  ApiError: class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
}));

describe("LoginPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("メールアドレスとパスワードの入力フィールドが表示される", () => {
    render(<LoginPage />);
    expect(screen.getByLabelText("メールアドレス")).toBeInTheDocument();
    expect(screen.getByLabelText("パスワード")).toBeInTheDocument();
  });

  it("必須バリデーション: 空送信でエラー表示", async () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));
    expect(
      await screen.findByText("メールアドレスを入力してください"),
    ).toBeInTheDocument();
  });

  it("メール形式バリデーション", async () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText("メールアドレス"), {
      target: { value: "invalid" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));
    expect(
      await screen.findByText("正しいメールアドレス形式で入力してください"),
    ).toBeInTheDocument();
  });

  it("パスワード8文字未満でバリデーションエラー", async () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText("メールアドレス"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "short" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));
    expect(
      await screen.findByText("パスワードは8文字以上で入力してください"),
    ).toBeInTheDocument();
  });

  it("正常な入力でlogin関数が呼ばれる", async () => {
    mockLogin.mockResolvedValue(undefined);
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText("メールアドレス"), {
      target: { value: "admin@example.com" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "admin1234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログイン" }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: "admin@example.com",
        password: "admin1234",
      });
    });
  });
});
