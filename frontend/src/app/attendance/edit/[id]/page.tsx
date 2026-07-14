"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navigation } from "@/components/Navigation";
import { updateRecord } from "@/lib/attendance-api";
import { apiFetch } from "@/lib/api-client";
import type { AttendanceRecordResponse } from "@/types/api";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

function toDatetimeLocal(datetime: string | null): string {
  if (!datetime) return "";
  return datetime.slice(0, 16);
}

function EditRecordPage() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);

  const [record, setRecord] = useState<AttendanceRecordResponse | null>(null);
  const [clockIn, setClockIn] = useState("");
  const [clockOut, setClockOut] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch<AttendanceRecordResponse>(`/attendance/records/${id}`)
      .then((r) => {
        setRecord(r);
        setClockIn(toDatetimeLocal(r.clockIn));
        setClockOut(toDatetimeLocal(r.clockOut));
      })
      .catch(() => setError("勤怠記録が見つかりません"))
      .finally(() => setLoading(false));
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!clockIn) {
      setError("出勤時刻は必須です");
      return;
    }

    if (clockOut && clockOut <= clockIn) {
      setError("退勤時刻は出勤時刻より後にしてください");
      return;
    }

    setSubmitting(true);
    try {
      await updateRecord(id, {
        clockIn: `${clockIn}:00`,
        clockOut: clockOut ? `${clockOut}:00` : null,
      });
      router.push("/attendance");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "エラーが発生しました");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p className="text-gray-500 text-center mt-8">読み込み中...</p>;
  }

  if (!record && error) {
    return <p className="text-red-600 text-center mt-8">{error}</p>;
  }

  return (
    <div className="max-w-md mx-auto mt-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">打刻修正</h1>

      <div className="bg-white shadow rounded-lg p-6">
        <p className="text-sm text-gray-500 mb-4">
          対象日: <span className="font-medium text-gray-900">{record?.date}</span>
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="clockIn"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              出勤時刻
            </label>
            <input
              id="clockIn"
              type="datetime-local"
              value={clockIn}
              onChange={(e) => setClockIn(e.target.value)}
              required
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm"
            />
          </div>

          <div>
            <label
              htmlFor="clockOut"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              退勤時刻
            </label>
            <input
              id="clockOut"
              type="datetime-local"
              value={clockOut}
              onChange={(e) => setClockOut(e.target.value)}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm"
            />
          </div>

          {error && <p className="text-red-600 text-sm">{error}</p>}

          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-md font-medium
                         disabled:opacity-50 hover:bg-blue-700 transition-colors"
            >
              保存
            </button>
            <button
              type="button"
              onClick={() => router.push("/attendance")}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md font-medium
                         hover:bg-gray-300 transition-colors"
            >
              キャンセル
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function EditPage() {
  return (
    <AuthGuard>
      <Navigation />
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <EditRecordPage />
      </main>
    </AuthGuard>
  );
}
