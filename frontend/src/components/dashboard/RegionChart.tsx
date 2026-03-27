'use client';

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Bucket } from '@/types/trafficLight';

interface RegionChartProps {
  data: Bucket[];
}

export default function RegionChart({ data }: RegionChartProps) {
  const sorted = [...data].sort((a, b) => b.count - a.count);

  const chartData = sorted.map((item) => ({
    name: item.key,
    count: item.count,
  }));

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        시도별 신호등 수
      </h3>
      <div className="h-[400px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            layout="vertical"
            data={chartData}
            margin={{ top: 5, right: 30, left: 60, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" tickFormatter={(v) => v.toLocaleString()} />
            <YAxis type="category" dataKey="name" width={80} fontSize={12} />
            <Tooltip
              formatter={(value: number) => [
                value.toLocaleString(),
                '신호등 수',
              ]}
            />
            <Bar dataKey="count" fill="#2563eb" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
