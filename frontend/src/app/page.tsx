"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navigation } from "@/components/Navigation";
import { clockIn, clockOut, getTodayRecord } from "@/lib/attendance-api";
import type { AttendanceRecordResponse } from "@/types/api";
import { useCallback, useEffect, useState } from "react";

type ClockStatus = "not_clocked_in" | "clocked_in" | "clocked_out";

function getStatus(record: AttendanceRecordResponse | null): ClockStatus {
  if (!record) return "not_clocked_in";
  if (!record.clockOut) return "clocked_in";
  return "clocked_out";
}

function formatTime(datetime: string | null): string {
  if (!datetime) return "--:--";
  return new Date(datetime).toLocaleTimeString("ja-JP", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function ClockPage() {
  const [record, setRecord] = useState<AttendanceRecordResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getTodayRecord()
      .then(setRecord)
      .catch(() => setRecord(null))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const handleClockIn = useCallback(async () => {
    setSubmitting(true);
    setError(null);
    try {
      const result = await clockIn();
      setRecord(result);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "エラーが発生しました");
    } finally {
      setSubmitting(false);
    }
  }, []);

  const handleClockOut = useCallback(async () => {
    setSubmitting(true);
    setError(null);
    try {
      const result = await clockOut();
      setRecord(result);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "エラーが発生しました");
    } finally {
      setSubmitting(false);
    }
  }, []);

  const status = getStatus(record);

  const statusLabel: Record<ClockStatus, string> = {
    not_clocked_in: "未出勤",
    clocked_in: "出勤済み",
    clocked_out: "退勤済み",
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto mt-8">
      <div className="bg-white shadow rounded-lg p-8 text-center">
        <p className="text-4xl font-mono text-gray-900 mb-2">
          {currentTime.toLocaleTimeString("ja-JP")}
        </p>
        <p className="text-sm text-gray-500 mb-6">
          {currentTime.toLocaleDateString("ja-JP", {
            year: "numeric",
            month: "long",
            day: "numeric",
            weekday: "long",
          })}
        </p>

        <div className="mb-6">
          <span
            className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${
              status === "not_clocked_in"
                ? "bg-gray-100 text-gray-700"
                : status === "clocked_in"
                  ? "bg-green-100 text-green-700"
                  : "bg-blue-100 text-blue-700"
            }`}
          >
            {statusLabel[status]}
          </span>
        </div>

        {record && (
          <div className="mb-6 text-sm text-gray-600 space-y-1">
            <p>出勤: {formatTime(record.clockIn)}</p>
            <p>退勤: {formatTime(record.clockOut)}</p>
          </div>
        )}

        {error && (
          <p className="text-red-600 text-sm mb-4">{error}</p>
        )}

        <div className="flex gap-4 justify-center">
          <button
            onClick={handleClockIn}
            disabled={status !== "not_clocked_in" || submitting}
            className="px-6 py-3 bg-green-600 text-white rounded-lg font-medium
                       disabled:opacity-50 disabled:cursor-not-allowed
                       hover:bg-green-700 transition-colors"
          >
            出勤
          </button>
          <button
            onClick={handleClockOut}
            disabled={status !== "clocked_in" || submitting}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium
                       disabled:opacity-50 disabled:cursor-not-allowed
                       hover:bg-blue-700 transition-colors"
          >
            退勤
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Home() {
  return (
    <AuthGuard>
      <Navigation />
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <ClockPage />
      </main>
    </AuthGuard>
  );
}
