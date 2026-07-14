import { describe, it, expect, vi, beforeEach } from "vitest";
import { withBasePath, apiFetch, ApiError } from "@/lib/api-client";

describe("withBasePath", () => {
  beforeEach(() => {
    vi.stubGlobal("window", {
      location: { pathname: "/" },
    });
  });

  it("通常環境ではパスをそのまま返す", () => {
    expect(withBasePath("/api/auth/login")).toBe("/api/auth/login");
  });

  it("SageMaker環境ではbasePathを付与する", () => {
    vi.stubGlobal("window", {
      location: { pathname: "/codeeditor/default/absports/3000/" },
    });
    expect(withBasePath("/api/auth/login")).toBe(
      "/codeeditor/default/absports/3000/api/auth/login",
    );
  });
});

describe("apiFetch", () => {
  beforeEach(() => {
    vi.stubGlobal("window", {
      location: { pathname: "/" },
    });
  });

  it("正常レスポンスでJSONをパースして返す", async () => {
    const mockData = { employee: { name: "管理者" } };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockData),
      }),
    );

    const result = await apiFetch("/auth/me");
    expect(result).toEqual(mockData);
  });

  it("エラーレスポンスでApiErrorをスローする", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        json: () =>
          Promise.resolve({ message: "認証に失敗しました" }),
      }),
    );

    await expect(apiFetch("/auth/me")).rejects.toThrow(ApiError);
    await expect(apiFetch("/auth/me")).rejects.toMatchObject({
      status: 401,
      message: "認証に失敗しました",
    });
  });

  it("204レスポンスでundefinedを返す", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 204,
        json: () => Promise.resolve(null),
      }),
    );

    const result = await apiFetch("/auth/logout");
    expect(result).toBeUndefined();
  });
});
