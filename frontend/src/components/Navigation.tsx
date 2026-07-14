"use client";

import { useAuth } from "@/lib/auth-context";
import Link from "next/link";

export function Navigation() {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <nav className="bg-white shadow">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center space-x-8">
            <Link href="/" className="text-xl font-bold text-gray-900">
              勤怠管理
            </Link>
            <Link href="/" className="text-gray-600 hover:text-gray-900">
              打刻
            </Link>
            <Link href="/history" className="text-gray-600 hover:text-gray-900">
              履歴
            </Link>
            {user.role === "ADMIN" && (
              <>
                <Link
                  href="/admin/employees"
                  className="text-gray-600 hover:text-gray-900"
                >
                  社員管理
                </Link>
                <Link
                  href="/admin/attendance"
                  className="text-gray-600 hover:text-gray-900"
                >
                  勤怠管理
                </Link>
              </>
            )}
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-600">{user.name}</span>
            <button
              onClick={logout}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              ログアウト
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
