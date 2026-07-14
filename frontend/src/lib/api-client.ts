const SAGEMAKER_BASE_PATH = "/codeeditor/default/absports/3000";

function getBasePath(): string {
  if (typeof window === "undefined") {
    return "";
  }
  const isSagemaker = window.location.pathname.startsWith("/codeeditor/");
  return isSagemaker ? SAGEMAKER_BASE_PATH : "";
}

export function withBasePath(path: string): string {
  return `${getBasePath()}${path}`;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const url = withBasePath(`/api${path}`);
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    credentials: "include",
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({ message: "通信エラー" }));
    throw new ApiError(res.status, body.message || "エラーが発生しました");
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json();
}
