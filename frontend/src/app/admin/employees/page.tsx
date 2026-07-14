"use client";

import { useState } from "react";
import Link from "next/link";
import { AuthGuard } from "@/components/AuthGuard";
import { Navigation } from "@/components/Navigation";
import { useEmployees } from "@/hooks/useEmployees";
import { deactivateEmployee } from "@/lib/employee-api";
import type { EmployeeResponse } from "@/types/api";

export default function EmployeeListPage() {
  return (
    <AuthGuard requiredRole="ADMIN">
      <Navigation />
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <EmployeeList />
      </main>
    </AuthGuard>
  );
}

function EmployeeList() {
  const { employees, loading, error, reload } = useEmployees();
  const [deleting, setDeleting] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  async function handleDelete(employee: EmployeeResponse) {
    setDeleting(employee.id);
    try {
      await deactivateEmployee(employee.id);
      await reload();
    } catch (e) {
      alert(e instanceof Error ? e.message : "削除に失敗しました");
    } finally {
      setDeleting(null);
      setConfirmId(null);
    }
  }

  if (loading) {
    return <p className="text-gray-500">読み込み中...</p>;
  }

  if (error) {
    return <p className="text-red-500">{error}</p>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">社員管理</h1>
        <Link
          href="/admin/employees/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          新規登録
        </Link>
      </div>

      <div className="bg-white shadow overflow-hidden rounded-lg">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                社員コード
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                氏名
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                メール
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                ロール
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                状態
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                操作
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {employees.map((emp) => (
              <tr key={emp.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {emp.employeeCode}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {emp.name}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {emp.email}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                      emp.role === "ADMIN"
                        ? "bg-purple-100 text-purple-800"
                        : "bg-green-100 text-green-800"
                    }`}
                  >
                    {emp.role === "ADMIN" ? "管理者" : "一般"}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className="px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    有効
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm space-x-2">
                  <Link
                    href={`/admin/employees/${emp.id}/edit`}
                    className="text-blue-600 hover:text-blue-800"
                  >
                    編集
                  </Link>
                  {confirmId === emp.id ? (
                    <>
                      <button
                        onClick={() => handleDelete(emp)}
                        disabled={deleting === emp.id}
                        className="text-red-600 hover:text-red-800 font-medium"
                      >
                        {deleting === emp.id ? "削除中..." : "確認"}
                      </button>
                      <button
                        onClick={() => setConfirmId(null)}
                        className="text-gray-500 hover:text-gray-700"
                      >
                        取消
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => setConfirmId(emp.id)}
                      className="text-red-600 hover:text-red-800"
                    >
                      削除
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {employees.length === 0 && (
          <p className="text-center py-8 text-gray-500">
            社員が登録されていません
          </p>
        )}
      </div>
    </div>
  );
}
