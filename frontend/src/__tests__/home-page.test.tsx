import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Home from "@/app/page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: vi.fn(),
    push: vi.fn(),
  }),
}));

vi.mock("@/lib/auth-context", () => ({
  useAuth: () => ({
    user: { id: 1, name: "テスト", email: "test@example.com", role: "EMPLOYEE", active: true },
    loading: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

vi.mock("@/lib/api-client", () => ({
  withBasePath: (path: string) => path,
  apiFetch: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
}));

const mockGetTodayRecord = vi.fn();
const mockClockIn = vi.fn();
const mockClockOut = vi.fn();

vi.mock("@/lib/attendance-api", () => ({
  getTodayRecord: (...args: unknown[]) => mockGetTodayRecord(...args),
  clockIn: (...args: unknown[]) => mockClockIn(...args),
  clockOut: (...args: unknown[]) => mockClockOut(...args),
}));

describe("Home (打刻画面)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("未出勤: 出勤ボタンのみ活性", async () => {
    mockGetTodayRecord.mockResolvedValue(null);

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("未出勤")).toBeInTheDocument();
    });

    const clockInBtn = screen.getByRole("button", { name: "出勤" });
    const clockOutBtn = screen.getByRole("button", { name: "退勤" });
    expect(clockInBtn).not.toBeDisabled();
    expect(clockOutBtn).toBeDisabled();
  });

  it("出勤済み: 退勤ボタンのみ活性", async () => {
    mockGetTodayRecord.mockResolvedValue({
      id: 1,
      employeeId: 1,
      date: "2026-07-14",
      clockIn: "2026-07-14T09:00:00",
      clockOut: null,
      workingMinutes: null,
      overtimeMinutes: null,
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("出勤済み")).toBeInTheDocument();
    });

    const clockInBtn = screen.getByRole("button", { name: "出勤" });
    const clockOutBtn = screen.getByRole("button", { name: "退勤" });
    expect(clockInBtn).toBeDisabled();
    expect(clockOutBtn).not.toBeDisabled();
  });

  it("退勤済み: 両ボタン非活性", async () => {
    mockGetTodayRecord.mockResolvedValue({
      id: 1,
      employeeId: 1,
      date: "2026-07-14",
      clockIn: "2026-07-14T09:00:00",
      clockOut: "2026-07-14T18:00:00",
      workingMinutes: 480,
      overtimeMinutes: 0,
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("退勤済み")).toBeInTheDocument();
    });

    const clockInBtn = screen.getByRole("button", { name: "出勤" });
    const clockOutBtn = screen.getByRole("button", { name: "退勤" });
    expect(clockInBtn).toBeDisabled();
    expect(clockOutBtn).toBeDisabled();
  });

  it("出勤ボタン押下 → API 呼び出し → 状態更新", async () => {
    mockGetTodayRecord.mockResolvedValue(null);
    mockClockIn.mockResolvedValue({
      id: 1,
      employeeId: 1,
      date: "2026-07-14",
      clockIn: "2026-07-14T09:00:00",
      clockOut: null,
      workingMinutes: null,
      overtimeMinutes: null,
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("未出勤")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "出勤" }));

    await waitFor(() => {
      expect(mockClockIn).toHaveBeenCalledTimes(1);
      expect(screen.getByText("出勤済み")).toBeInTheDocument();
    });
  });

  it("退勤ボタン押下 → API 呼び出し → 状態更新", async () => {
    mockGetTodayRecord.mockResolvedValue({
      id: 1,
      employeeId: 1,
      date: "2026-07-14",
      clockIn: "2026-07-14T09:00:00",
      clockOut: null,
      workingMinutes: null,
      overtimeMinutes: null,
    });
    mockClockOut.mockResolvedValue({
      id: 1,
      employeeId: 1,
      date: "2026-07-14",
      clockIn: "2026-07-14T09:00:00",
      clockOut: "2026-07-14T18:00:00",
      workingMinutes: 480,
      overtimeMinutes: 0,
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("出勤済み")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "退勤" }));

    await waitFor(() => {
      expect(mockClockOut).toHaveBeenCalledTimes(1);
      expect(screen.getByText("退勤済み")).toBeInTheDocument();
    });
  });
});
