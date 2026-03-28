import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {
  searchTrafficLights,
  geoSearch,
  getFilterOptions,
  getAggregations,
  getAggregationSummary,
  createIndex,
  loadData,
  syncFromApi,
  getIndexStatus,
} from '@/lib/api';

// axios 인스턴스가 아닌 기본 axios를 mock하므로 직접 mock adapter 사용
// api.ts 내부에서 axios.create로 client를 생성하므로, 모듈 자체를 mock합니다.
jest.mock('axios', () => {
  const mockAxiosInstance = {
    get: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
  };
  return {
    __esModule: true,
    default: {
      create: jest.fn(() => mockAxiosInstance),
      ...mockAxiosInstance,
    },
  };
});

// client 인스턴스를 가져옵니다
const mockedAxios = axios.create() as jest.Mocked<ReturnType<typeof axios.create>>;

describe('API 유틸리티', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('searchTrafficLights', () => {
    it('검색어와 파라미터를 올바르게 전달한다', async () => {
      const mockResponse = {
        data: { total: 1, page: 0, size: 20, results: [{ sidoName: '서울특별시' }] },
      };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await searchTrafficLights({
        query: '강남',
        sidoName: '서울특별시',
        page: 0,
        size: 20,
      });

      expect(mockedAxios.get).toHaveBeenCalledWith('/search', {
        params: expect.objectContaining({
          q: '강남',
          sidoName: '서울특별시',
          page: 0,
          size: 20,
        }),
      });
      expect(result.total).toBe(1);
      expect(result.results[0].sidoName).toBe('서울특별시');
    });

    it('검색어가 없으면 q 파라미터를 포함하지 않는다', async () => {
      const mockResponse = { data: { total: 0, page: 0, size: 20, results: [] } };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      await searchTrafficLights({ page: 0, size: 20 });

      const callArgs = (mockedAxios.get as jest.Mock).mock.calls[0];
      expect(callArgs[1].params.q).toBeUndefined();
    });

    it('빈 검색어는 q 파라미터를 포함하지 않는다', async () => {
      const mockResponse = { data: { total: 0, page: 0, size: 20, results: [] } };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      await searchTrafficLights({ query: '', page: 0, size: 20 });

      const callArgs = (mockedAxios.get as jest.Mock).mock.calls[0];
      expect(callArgs[1].params.q).toBeUndefined();
    });
  });

  describe('geoSearch', () => {
    it('위치 기반 검색 파라미터를 올바르게 전달한다', async () => {
      const mockResponse = {
        data: { total: 5, page: 0, size: 100, results: [] },
      };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await geoSearch(37.5665, 126.978, '1km', 0, 100);

      expect(mockedAxios.get).toHaveBeenCalledWith('/search/geo', {
        params: { lat: 37.5665, lon: 126.978, distance: '1km', page: 0, size: 100 },
      });
      expect(result.total).toBe(5);
    });

    it('기본값이 올바르게 적용된다', async () => {
      const mockResponse = { data: { total: 0, page: 0, size: 100, results: [] } };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      await geoSearch(36.0, 127.0, '5km');

      expect(mockedAxios.get).toHaveBeenCalledWith('/search/geo', {
        params: { lat: 36.0, lon: 127.0, distance: '5km', page: 0, size: 100 },
      });
    });
  });

  describe('getFilterOptions', () => {
    it('필터 옵션을 요청한다', async () => {
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce({
        data: ['서울특별시', '부산광역시'],
      });

      const result = await getFilterOptions('sidoName');

      expect(mockedAxios.get).toHaveBeenCalledWith('/search/filters/sidoName', {
        params: {},
      });
      expect(result).toEqual(['서울특별시', '부산광역시']);
    });

    it('부모 필터와 함께 요청한다', async () => {
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce({
        data: ['강남구', '서초구'],
      });

      const result = await getFilterOptions('sigunguName', '서울특별시');

      expect(mockedAxios.get).toHaveBeenCalledWith('/search/filters/sigunguName', {
        params: { sidoName: '서울특별시' },
      });
      expect(result).toEqual(['강남구', '서초구']);
    });
  });

  describe('getAggregations', () => {
    it('집계 타입별로 요청한다', async () => {
      const mockData = { buckets: [{ key: '서울특별시', count: 5000 }] };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce({ data: mockData });

      const result = await getAggregations('by-region');

      expect(mockedAxios.get).toHaveBeenCalledWith('/aggregations/by-region');
      expect(result.buckets[0].key).toBe('서울특별시');
    });
  });

  describe('getAggregationSummary', () => {
    it('요약 집계를 요청한다', async () => {
      const mockData = {
        sidoName: { buckets: [{ key: '서울특별시', count: 5000 }] },
        roadType: { buckets: [{ key: '일반국도', count: 10000 }] },
      };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce({ data: mockData });

      const result = await getAggregationSummary();

      expect(mockedAxios.get).toHaveBeenCalledWith('/aggregations/summary');
      expect(result.sidoName.buckets[0].key).toBe('서울특별시');
      expect(result.roadType.buckets[0].key).toBe('일반국도');
    });
  });

  describe('인덱스 관리 API', () => {
    it('createIndex는 POST /index/create를 호출한다', async () => {
      (mockedAxios.post as jest.Mock).mockResolvedValueOnce({ data: undefined });

      await createIndex();

      expect(mockedAxios.post).toHaveBeenCalledWith('/index/create');
    });

    it('loadData는 POST /index/load를 호출한다', async () => {
      const mockStatus = {
        totalRecords: 10000, indexed: 9950, failed: 50, durationMs: 5000, indexExists: true,
      };
      (mockedAxios.post as jest.Mock).mockResolvedValueOnce({ data: mockStatus });

      const result = await loadData();

      expect(mockedAxios.post).toHaveBeenCalledWith('/index/load');
      expect(result.totalRecords).toBe(10000);
      expect(result.failed).toBe(50);
    });

    it('syncFromApi는 POST /sync를 호출한다', async () => {
      const mockStatus = {
        totalRecords: 50000, indexed: 49500, failed: 500, durationMs: 30000, indexExists: true,
      };
      (mockedAxios.post as jest.Mock).mockResolvedValueOnce({ data: mockStatus });

      const result = await syncFromApi();

      expect(mockedAxios.post).toHaveBeenCalledWith('/sync');
      expect(result.indexed).toBe(49500);
    });

    it('getIndexStatus는 GET /index/status를 호출한다', async () => {
      const mockStatus = {
        totalRecords: 5000, indexed: 5000, failed: 0, durationMs: 0, indexExists: true,
      };
      (mockedAxios.get as jest.Mock).mockResolvedValueOnce({ data: mockStatus });

      const result = await getIndexStatus();

      expect(mockedAxios.get).toHaveBeenCalledWith('/index/status');
      expect(result.indexExists).toBe(true);
    });
  });
});
