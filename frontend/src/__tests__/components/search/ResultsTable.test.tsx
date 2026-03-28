import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ResultsTable from '@/components/search/ResultsTable';
import { SearchResponse } from '@/types/trafficLight';

describe('ResultsTable', () => {
  const mockOnPageChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('로딩 중일 때 스피너를 표시한다', () => {
    render(
      <ResultsTable results={null} onPageChange={mockOnPageChange} loading={true} />
    );

    expect(screen.getByText('검색 중...')).toBeInTheDocument();
  });

  it('결과가 null이면 안내 메시지를 표시한다', () => {
    render(
      <ResultsTable results={null} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('검색어를 입력하거나 필터를 선택하세요.')).toBeInTheDocument();
  });

  it('결과가 0건이면 없음 메시지를 표시한다', () => {
    const emptyResults: SearchResponse = {
      total: 0,
      page: 0,
      size: 20,
      results: [],
    };

    render(
      <ResultsTable results={emptyResults} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument();
  });

  it('결과가 있으면 테이블을 렌더링한다', () => {
    const results: SearchResponse = {
      total: 1,
      page: 0,
      size: 20,
      results: [
        {
          sidoName: '서울특별시',
          sigunguName: '강남구',
          roadType: '일반국도',
          roadRouteNumber: '1',
          roadRouteName: '경부선',
          roadRouteDirection: '상행',
          roadNameAddress: '테헤란로 123',
          lotNumberAddress: '역삼동 123-4',
          latitude: 37.5665,
          longitude: 126.978,
          signalInstallType: '기둥식',
          roadShape: '교차로',
          isMainRoad: 'Y',
          trafficLightId: 'TL-001',
          trafficLightCategory: '차량신호등',
          lightColorType: '3색',
          signalMethod: '점등',
          signalSequence: '',
          signalDuration: '',
          lightSourceType: 'LED',
          signalControlMethod: '',
          signalTimeDecisionMethod: '',
          flashingLightEnabled: '',
          flashingStartTime: '',
          flashingEndTime: '',
          hasPedestrianSignal: '',
          hasRemainingTimeDisplay: '',
          hasAudioSignal: '',
          roadSignSerialNumber: '',
          managementAgency: '서울시청',
          managementPhone: '',
          dataReferenceDate: '',
          providerCode: '',
          providerName: '',
        },
      ],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('서울특별시')).toBeInTheDocument();
    expect(screen.getByText('강남구')).toBeInTheDocument();
    expect(screen.getByText('일반국도')).toBeInTheDocument();
    expect(screen.getByText('차량신호등')).toBeInTheDocument();
    expect(screen.getByText('서울시청')).toBeInTheDocument();
  });

  it('총 건수를 표시한다', () => {
    const results: SearchResponse = {
      total: 12345,
      page: 0,
      size: 20,
      results: [{
        sidoName: '서울특별시', sigunguName: '강남구', roadType: '', roadRouteNumber: '',
        roadRouteName: '', roadRouteDirection: '', roadNameAddress: '', lotNumberAddress: '',
        latitude: 0, longitude: 0, signalInstallType: '', roadShape: '', isMainRoad: '',
        trafficLightId: '', trafficLightCategory: '', lightColorType: '', signalMethod: '',
        signalSequence: '', signalDuration: '', lightSourceType: '', signalControlMethod: '',
        signalTimeDecisionMethod: '', flashingLightEnabled: '', flashingStartTime: '',
        flashingEndTime: '', hasPedestrianSignal: '', hasRemainingTimeDisplay: '',
        hasAudioSignal: '', roadSignSerialNumber: '', managementAgency: '',
        managementPhone: '', dataReferenceDate: '', providerCode: '', providerName: '',
      }],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('12,345')).toBeInTheDocument();
  });

  it('이전 버튼은 첫 페이지에서 비활성화된다', () => {
    const results: SearchResponse = {
      total: 100, page: 0, size: 20,
      results: [{
        sidoName: '서울특별시', sigunguName: '', roadType: '', roadRouteNumber: '',
        roadRouteName: '', roadRouteDirection: '', roadNameAddress: '', lotNumberAddress: '',
        latitude: 0, longitude: 0, signalInstallType: '', roadShape: '', isMainRoad: '',
        trafficLightId: '', trafficLightCategory: '', lightColorType: '', signalMethod: '',
        signalSequence: '', signalDuration: '', lightSourceType: '', signalControlMethod: '',
        signalTimeDecisionMethod: '', flashingLightEnabled: '', flashingStartTime: '',
        flashingEndTime: '', hasPedestrianSignal: '', hasRemainingTimeDisplay: '',
        hasAudioSignal: '', roadSignSerialNumber: '', managementAgency: '',
        managementPhone: '', dataReferenceDate: '', providerCode: '', providerName: '',
      }],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('이전')).toBeDisabled();
    expect(screen.getByText('다음')).not.toBeDisabled();
  });

  it('다음 버튼 클릭 시 onPageChange가 호출된다', () => {
    const results: SearchResponse = {
      total: 100, page: 0, size: 20,
      results: [{
        sidoName: '서울특별시', sigunguName: '', roadType: '', roadRouteNumber: '',
        roadRouteName: '', roadRouteDirection: '', roadNameAddress: '', lotNumberAddress: '',
        latitude: 0, longitude: 0, signalInstallType: '', roadShape: '', isMainRoad: '',
        trafficLightId: '', trafficLightCategory: '', lightColorType: '', signalMethod: '',
        signalSequence: '', signalDuration: '', lightSourceType: '', signalControlMethod: '',
        signalTimeDecisionMethod: '', flashingLightEnabled: '', flashingStartTime: '',
        flashingEndTime: '', hasPedestrianSignal: '', hasRemainingTimeDisplay: '',
        hasAudioSignal: '', roadSignSerialNumber: '', managementAgency: '',
        managementPhone: '', dataReferenceDate: '', providerCode: '', providerName: '',
      }],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    fireEvent.click(screen.getByText('다음'));

    expect(mockOnPageChange).toHaveBeenCalledWith(1);
  });

  it('마지막 페이지에서 다음 버튼이 비활성화된다', () => {
    const results: SearchResponse = {
      total: 20, page: 0, size: 20,
      results: [{
        sidoName: '서울특별시', sigunguName: '', roadType: '', roadRouteNumber: '',
        roadRouteName: '', roadRouteDirection: '', roadNameAddress: '', lotNumberAddress: '',
        latitude: 0, longitude: 0, signalInstallType: '', roadShape: '', isMainRoad: '',
        trafficLightId: '', trafficLightCategory: '', lightColorType: '', signalMethod: '',
        signalSequence: '', signalDuration: '', lightSourceType: '', signalControlMethod: '',
        signalTimeDecisionMethod: '', flashingLightEnabled: '', flashingStartTime: '',
        flashingEndTime: '', hasPedestrianSignal: '', hasRemainingTimeDisplay: '',
        hasAudioSignal: '', roadSignSerialNumber: '', managementAgency: '',
        managementPhone: '', dataReferenceDate: '', providerCode: '', providerName: '',
      }],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('다음')).toBeDisabled();
  });

  it('페이지 정보를 올바르게 표시한다', () => {
    const results: SearchResponse = {
      total: 100, page: 2, size: 20,
      results: [{
        sidoName: '서울특별시', sigunguName: '', roadType: '', roadRouteNumber: '',
        roadRouteName: '', roadRouteDirection: '', roadNameAddress: '', lotNumberAddress: '',
        latitude: 0, longitude: 0, signalInstallType: '', roadShape: '', isMainRoad: '',
        trafficLightId: '', trafficLightCategory: '', lightColorType: '', signalMethod: '',
        signalSequence: '', signalDuration: '', lightSourceType: '', signalControlMethod: '',
        signalTimeDecisionMethod: '', flashingLightEnabled: '', flashingStartTime: '',
        flashingEndTime: '', hasPedestrianSignal: '', hasRemainingTimeDisplay: '',
        hasAudioSignal: '', roadSignSerialNumber: '', managementAgency: '',
        managementPhone: '', dataReferenceDate: '', providerCode: '', providerName: '',
      }],
    };

    render(
      <ResultsTable results={results} onPageChange={mockOnPageChange} loading={false} />
    );

    expect(screen.getByText('페이지 3 / 5')).toBeInTheDocument();
  });
});
