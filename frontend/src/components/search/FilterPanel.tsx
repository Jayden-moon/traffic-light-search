'use client';

import { useState, useEffect, useCallback } from 'react';
import { getFilterOptions } from '@/lib/api';

export interface Filters {
  sidoName?: string;
  sigunguName?: string;
  roadType?: string;
  trafficLightCategory?: string;
}

interface FilterPanelProps {
  filters: Filters;
  onFilterChange: (filters: Filters) => void;
}

export default function FilterPanel({
  filters,
  onFilterChange,
}: FilterPanelProps) {
  const [sidoOptions, setSidoOptions] = useState<string[]>([]);
  const [sigunguOptions, setSigunguOptions] = useState<string[]>([]);
  const [roadTypeOptions, setRoadTypeOptions] = useState<string[]>([]);
  const [signalTypeOptions, setSignalTypeOptions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchInitialOptions();
  }, []);

  const fetchSigungu = useCallback(
    async (sido: string) => {
      if (!sido) {
        setSigunguOptions([]);
        return;
      }
      try {
        const res = await getFilterOptions('sigunguName', sido);
        setSigunguOptions(res);
      } catch {
        setSigunguOptions([]);
      }
    },
    []
  );

  useEffect(() => {
    fetchSigungu(filters.sidoName || '');
  }, [filters.sidoName, fetchSigungu]);

  async function fetchInitialOptions() {
    setLoading(true);
    try {
      const [sido, roadType, signalType] = await Promise.all([
        getFilterOptions('sidoName'),
        getFilterOptions('roadType'),
        getFilterOptions('trafficLightCategory'),
      ]);
      setSidoOptions(sido);
      setRoadTypeOptions(roadType);
      setSignalTypeOptions(signalType);
    } catch {
      // API might not be available
    } finally {
      setLoading(false);
    }
  }

  function handleChange(field: keyof Filters, value: string) {
    const newFilters = { ...filters, [field]: value || undefined };

    if (field === 'sidoName') {
      newFilters.sigunguName = undefined;
    }

    onFilterChange(newFilters);
  }

  function handleReset() {
    onFilterChange({});
  }

  return (
    <div className="bg-white rounded-lg shadow p-4 space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-900">필터</h3>
        <button
          onClick={handleReset}
          className="text-sm text-blue-600 hover:text-blue-800"
        >
          초기화
        </button>
      </div>

      {loading && (
        <p className="text-sm text-gray-500">필터 옵션 로딩 중...</p>
      )}

      {/* 시도명 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          시도명
        </label>
        <select
          value={filters.sidoName || ''}
          onChange={(e) => handleChange('sidoName', e.target.value)}
          className="block w-full border border-gray-300 rounded-md px-3 py-2 text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">전체</option>
          {sidoOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      </div>

      {/* 시군구명 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          시군구명
        </label>
        <select
          value={filters.sigunguName || ''}
          onChange={(e) => handleChange('sigunguName', e.target.value)}
          disabled={!filters.sidoName}
          className="block w-full border border-gray-300 rounded-md px-3 py-2 text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-400"
        >
          <option value="">전체</option>
          {sigunguOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      </div>

      {/* 도로종류 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          도로종류
        </label>
        <select
          value={filters.roadType || ''}
          onChange={(e) => handleChange('roadType', e.target.value)}
          className="block w-full border border-gray-300 rounded-md px-3 py-2 text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">전체</option>
          {roadTypeOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      </div>

      {/* 신호등구분 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          신호등구분
        </label>
        <select
          value={filters.trafficLightCategory || ''}
          onChange={(e) => handleChange('trafficLightCategory', e.target.value)}
          className="block w-full border border-gray-300 rounded-md px-3 py-2 text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">전체</option>
          {signalTypeOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
