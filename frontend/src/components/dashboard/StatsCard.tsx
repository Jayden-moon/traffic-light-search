'use client';

interface StatsCardProps {
  title: string;
  value: string | number;
  icon?: string;
}

export default function StatsCard({ title, value, icon }: StatsCardProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6 flex items-center space-x-4">
      {icon && (
        <div className="text-3xl flex-shrink-0">{icon}</div>
      )}
      <div>
        <p className="text-sm font-medium text-gray-500">{title}</p>
        <p className="text-2xl font-bold text-gray-900">
          {typeof value === 'number' ? value.toLocaleString() : value}
        </p>
      </div>
    </div>
  );
}
