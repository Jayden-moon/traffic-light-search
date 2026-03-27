import axios from 'axios';
import {
  SearchResponse,
  AggregationResponse,
  IndexStatus,
} from '@/types/trafficLight';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
});

export interface SearchParams {
  query?: string;
  sidoName?: string;
  sigunguName?: string;
  roadType?: string;
  trafficLightCategory?: string;
  page?: number;
  size?: number;
}

export async function searchTrafficLights(
  params: SearchParams
): Promise<SearchResponse> {
  const { query, ...rest } = params;
  const apiParams: Record<string, unknown> = { ...rest };
  if (query) apiParams.q = query;
  const { data } = await client.get<SearchResponse>('/search', { params: apiParams });
  return data;
}

export async function geoSearch(
  lat: number,
  lon: number,
  distance: string,
  page: number = 0,
  size: number = 100
): Promise<SearchResponse> {
  const { data } = await client.get<SearchResponse>('/search/geo', {
    params: { lat, lon, distance, page, size },
  });
  return data;
}

export async function getFilterOptions(
  field: string,
  parentFilter?: string
): Promise<string[]> {
  const params: Record<string, string> = {};
  if (parentFilter) {
    params.sidoName = parentFilter;
  }
  const { data } = await client.get<string[]>(
    `/search/filters/${field}`,
    { params }
  );
  return data;
}

export async function getAggregations(
  type: string
): Promise<AggregationResponse> {
  const { data } = await client.get<AggregationResponse>(
    `/aggregations/${type}`
  );
  return data;
}

export async function getAggregationSummary(): Promise<
  Record<string, AggregationResponse>
> {
  const { data } = await client.get<Record<string, AggregationResponse>>(
    '/aggregations/summary'
  );
  return data;
}

export async function createIndex(): Promise<void> {
  await client.post('/index/create');
}

export async function loadData(): Promise<IndexStatus> {
  const { data } = await client.post<IndexStatus>('/index/load');
  return data;
}

export async function syncFromApi(): Promise<IndexStatus> {
  const { data } = await client.post<IndexStatus>('/sync');
  return data;
}

export async function getIndexStatus(): Promise<IndexStatus> {
  const { data } = await client.get<IndexStatus>('/index/status');
  return data;
}
