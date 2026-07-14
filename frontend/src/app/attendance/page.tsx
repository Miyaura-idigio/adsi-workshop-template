"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navigation } from "@/components/Navigation";
import { getMonthlyRecords, getMonthlySummary } from "@/lib/attendance-api";
import type {
  AttendanceRecordResponse,
  MonthlySummaryResponse,
} from "@/types/api";
import Link from "next/link";
import { useEffect, useState } from "react";

function formatMinutes(minutes: number | null): string {
  if (minutes === null) return "--:--";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}時間${m > 0 ? `${m}分` : ""}`;
}

function formatTime(datetime: string | null): string {
  if (!datetime) return "--:--";
  return new Date(datetime).toLocaleTimeString("ja-JP", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getCurrentYearMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function generateYearMonthOptions(): string[] {
  const options: string[] = [];
  const now = new Date();
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    options.push(
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
    );
  }
  return options;
}

function AttendanceHistoryPage() {
  const [yearMonth, setYearMonth] = useState(getCurrentYearMonth());
  const [records, setRecords] = useState<AttendanceRecordResponse[]>([]);
  const [summary, setSummary] = useState<MonthlySummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([getMonthlyRecords(yearMonth), getMonthlySummary(yearMonth)])
      .then(([recs, sum]) => {
        setRecords(recs);
        setSummary(sum);
      })
      .catch(() => {
        setRecords([]);
        setSummary(null);
      })
      .finally(() => setLoading(false));
  }, [yearMonth]);

  return (
    <div className="max-w-4xl mx-auto mt-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">勤怠履歴</h1>
        <select
          value={yearMonth}
          onChange={(e) => setYearMonth(e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm"
        >
          {generateYearMonthOptions().map((ym) => (
            <option key={ym} value={ym}>
              {ym.replace("-", "年")}月
            </option>
          ))}
        </select>
      </div>

      {summary && (
        <div className="bg-white shadow rounded-lg p-4 mb-6 grid grid-cols-3 gap-4 text-center">
          <div>
            <p className="text-sm text-gray-500">出勤日数</p>
            <p className="text-xl font-bold">{summary.totalWorkingDays}日</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">合計勤務時間</p>
            <p className="text-xl font-bold">
              {formatMinutes(summary.totalActualMinutes)}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-500">合計残業時間</p>
            <p className="text-xl font-bold">
              {formatMinutes(summary.totalOvertimeMinutes)}
            </p>
          </div>
        </div>
      )}

      {loading ? (
        <p className="text-gray-500 text-center">読み込み中...</p>
      ) : records.length === 0 ? (
        <p className="text-gray-500 text-center">この月の勤怠記録はありません</p>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  日付
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  出勤時刻
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  退勤時刻
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  勤務時間
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  残業時間
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  操作
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {records.map((record) => (
                <tr key={record.id}>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {record.date}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {formatTime(record.clockIn)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {formatTime(record.clockOut)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {formatMinutes(record.workingMinutes)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {formatMinutes(record.overtimeMinutes)}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <Link
                      href={`/attendance/edit/${record.id}`}
                      className="text-blue-600 hover:text-blue-800"
                    >
                      修正
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function AttendancePage() {
  return (
    <AuthGuard>
      <Navigation />
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <AttendanceHistoryPage />
      </main>
    </AuthGuard>
  );
}
