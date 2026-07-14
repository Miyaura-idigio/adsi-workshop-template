"use client";

import { useCallback, useEffect, useState } from "react";
import type { EmployeeResponse } from "@/types/api";
import { fetchEmployees } from "@/lib/employee-api";

export function useEmployees() {
  const [employees, setEmployees] = useState<EmployeeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchEmployees();
      setEmployees(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "社員一覧の取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { employees, loading, error, reload: load };
}
