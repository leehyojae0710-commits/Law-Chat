import { useEffect, useState } from "react";
import { getDashboardStats, type DashboardStats } from "../../../api/admin";

export const useDashboardStats = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .finally(() => setIsLoading(false));
  }, []);

  return { stats, isLoading };
};
