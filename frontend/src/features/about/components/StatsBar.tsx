import { stats } from "../data";

export const StatsBar = () => {
  return (
    <div className="grid grid-cols-4 border rounded-xl divide-x">
      {stats.map((s) => (
        <div key={s.id} className="p-6 text-center">
          <p className="text-2xl font-bold text-purple-600">{s.value}</p>
          <p className="text-sm text-gray-500 mt-1">{s.label}</p>
        </div>
      ))}
    </div>
  );
};
