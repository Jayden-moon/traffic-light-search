'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { geoSearch } from '@/lib/api';
import { TrafficLight } from '@/types/trafficLight';

// Dynamically import MapView with SSR disabled (Leaflet requires window)
const MapView = dynamic(() => import('@/components/map/MapView'), {
  ssr: false,
  loading: () => (
    <div className="h-full flex items-center justify-center bg-gray-100 rounded-lg">
      <p className="text-gray-500">지도 로딩 중...</p>
    </div>
  ),
});

export default function MapPage() {
  const [lat, setLat] = useState('36.5');
  const [lon, setLon] = useState('127.5');
  const [distance, setDistance] = useState('5');
  const [results, setResults] = useState<TrafficLight[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [total, setTotal] = useState(0);
  const [center, setCenter] = useState<[number, number]>([36.5, 127.5]);

  async function handleSearch() {
    const latNum = parseFloat(lat);
    const lonNum = parseFloat(lon);
    const distNum = parseFloat(distance);

    if (isNaN(latNum) || isNaN(lonNum) || isNaN(distNum)) {
      setError('유효한 숫자를 입력하세요.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await geoSearch(latNum, lonNum, `${distNum}km`, 0, 500);
      setResults(data.results);
      setTotal(data.total);
      setCenter([latNum, lonNum]);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'API 서버에 연결할 수 없습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  function handleMapClick(clickLat: number, clickLng: number) {
    setLat(clickLat.toFixed(6));
    setLon(clickLng.toFixed(6));
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 h-[calc(100vh-5rem)] flex flex-col">
      {/* Search Controls */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="flex flex-wrap items-end gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              위도
            </label>
            <input
              type="text"
              value={lat}
              onChange={(e) => setLat(e.target.value)}
              className="border border-gray-300 rounded-md px-3 py-2 w-36 text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              경도
            </label>
            <input
              type="text"
              value={lon}
              onChange={(e) => setLon(e.target.value)}
              className="border border-gray-300 rounded-md px-3 py-2 w-36 text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              반경 (km)
            </label>
            <input
              type="text"
              value={distance}
              onChange={(e) => setDistance(e.target.value)}
              className="border border-gray-300 rounded-md px-3 py-2 w-24 text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            onClick={handleSearch}
            disabled={loading}
            className="px-6 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors disabled:bg-gray-400"
          >
            {loading ? '검색 중...' : '검색'}
          </button>
          {total > 0 && (
            <span className="text-sm text-gray-600">
              결과: {total.toLocaleString()}건
            </span>
          )}
        </div>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
        <p className="mt-2 text-xs text-gray-500">
          지도를 클릭하면 해당 좌표가 자동으로 입력됩니다.
        </p>
      </div>

      {/* Map */}
      <div className="flex-1 rounded-lg overflow-hidden shadow">
        <MapView results={results} center={center} onMapClick={handleMapClick} />
      </div>
    </div>
  );
}
