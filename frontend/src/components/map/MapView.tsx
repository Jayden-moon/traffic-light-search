'use client';

import { useEffect } from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMapEvents,
} from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

import { TrafficLight } from '@/types/trafficLight';

// Fix Leaflet default marker icon issue in Next.js / webpack
delete (L.Icon.Default.prototype as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:
    'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

interface MapViewProps {
  results: TrafficLight[];
  center: [number, number];
  onMapClick: (lat: number, lng: number) => void;
}

function MapClickHandler({
  onMapClick,
}: {
  onMapClick: (lat: number, lng: number) => void;
}) {
  useMapEvents({
    click(e) {
      onMapClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

export default function MapView({ results, center, onMapClick }: MapViewProps) {
  useEffect(() => {
    // Force re-render of map when window is resized
    window.dispatchEvent(new Event('resize'));
  }, []);

  return (
    <MapContainer
      center={center}
      zoom={7}
      scrollWheelZoom={true}
      className="h-full w-full rounded-lg"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapClickHandler onMapClick={onMapClick} />
      <MarkerClusterGroup chunkedLoading>
        {results.map((item, idx) => {
          const lat = item.latitude || item.location?.lat;
          const lon = item.longitude || item.location?.lon;
          if (!lat || !lon) return null;
          return (
            <Marker key={idx} position={[lat, lon]}>
              <Popup>
                <div className="text-sm space-y-1">
                  <p className="font-semibold">
                    {item.lotNumberAddress || item.roadNameAddress || '주소 없음'}
                  </p>
                  <p>도로종류: {item.roadType || '—'}</p>
                  <p>신호등구분: {item.trafficLightCategory || '—'}</p>
                  <p>관리기관: {item.managementAgency || '—'}</p>
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MarkerClusterGroup>
    </MapContainer>
  );
}
