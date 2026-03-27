'use client';

import { useState, useEffect } from 'react';
import { getAggregationSummary } from '@/lib/api';
import { Bucket } from '@/types/trafficLight';
import StatsCard from '@/components/dashboard/StatsCard';
import RegionChart from '@/components/dashboard/RegionChart';
import RoadTypeChart from '@/components/dashboard/RoadTypeChart';

export default function DashboardPage() {
  const [regionData, setRegionData] = useState<Bucket[]>([]);
  const [roadTypeData, setRoadTypeData] = useState<Bucket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    setLoading(true);
    setError(null);
    try {
      const summary = await getAggregationSummary();

      // Extract region and road type buckets from the summary
      const regions = summary['sidoName']?.buckets || summary['region']?.buckets || [];
      const roadTypes = summary['roadType']?.buckets || summary['roadType']?.buckets || [];

      setRegionData(regions);
      setRoadTypeData(roadTypes);
    } catch (err: unknown) {
      const message =
        err instanceof Error
          ? err.message
          : 'API 서버에 연결할 수 없습니다. 데이터를 먼저 인덱싱해주세요.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  const totalCount = regionData.reduce((sum, b) => sum + b.count, 0);
  const sidoCount = regionData.length;
  const roadTypeCount = roadTypeData.length;

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent" />
            <p className="mt-4 text-gray-600">대시보드 데이터 로딩 중...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <p className="text-red-700">{error}</p>
          <button
            onClick={fetchData}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatsCard title="총 신호등 수" value={totalCount} icon="🚦" />
        <StatsCard title="시도 수" value={sidoCount} icon="🗺" />
        <StatsCard title="도로종류 수" value={roadTypeCount} icon="🛣" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RegionChart data={regionData} />
        <RoadTypeChart data={roadTypeData} />
      </div>
    </div>
  );
}
