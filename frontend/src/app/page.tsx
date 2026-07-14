"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navigation } from "@/components/Navigation";
import { useAuth } from "@/lib/auth-context";

export default function Home() {
  return (
    <AuthGuard>
      <Navigation />
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <h1 className="text-2xl font-bold text-gray-900">ダッシュボード</h1>
        <p className="mt-2 text-gray-600">
          出退勤の打刻や勤怠履歴の確認ができます。
        </p>
      </main>
    </AuthGuard>
  );
}
