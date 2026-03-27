export interface TrafficLight {
  sidoName: string;
  sigunguName: string;
  roadType: string;
  roadRouteNumber: string;
  roadRouteName: string;
  roadRouteDirection: string;
  roadNameAddress: string;
  lotNumberAddress: string;
  latitude: number;
  longitude: number;
  location?: { lat: number; lon: number };
  signalInstallType: string;
  roadShape: string;
  isMainRoad: string;
  trafficLightId: string;
  trafficLightCategory: string;
  lightColorType: string;
  signalMethod: string;
  signalSequence: string;
  signalDuration: string;
  lightSourceType: string;
  signalControlMethod: string;
  signalTimeDecisionMethod: string;
  flashingLightEnabled: string;
  flashingStartTime: string;
  flashingEndTime: string;
  hasPedestrianSignal: string;
  hasRemainingTimeDisplay: string;
  hasAudioSignal: string;
  roadSignSerialNumber: string;
  managementAgency: string;
  managementPhone: string;
  dataReferenceDate: string;
  providerCode: string;
  providerName: string;
}

export interface SearchResponse {
  total: number;
  page: number;
  size: number;
  results: TrafficLight[];
}

export interface Bucket {
  key: string;
  count: number;
  subBuckets?: Bucket[];
}

export interface AggregationResponse {
  buckets: Bucket[];
}

export interface IndexStatus {
  totalRecords: number;
  indexed: number;
  failed: number;
  durationMs: number;
  indexExists: boolean;
}
