export interface AttendancePolicyResponse {
  id: string;
  name: string;
  version: number;
  minimumThresholdPercentage: number;
  statusContributions: Record<string, number>;
  missingRecordStrategy: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active: boolean;
}

export interface CreateAttendancePolicyRequest {
  name: string;
  minimumThresholdPercentage: number;
  statusContributions: Record<string, number>;
  missingRecordStrategy: string;
}

export interface OverallAttendancePolicyResponse {
  id: string;
  name: string;
  version: number;
  aggregationStrategy: string;
  minimumThresholdPercentage: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active: boolean;
}

export interface CreateOverallAttendancePolicyRequest {
  name: string;
  aggregationStrategy: string;
  minimumThresholdPercentage: number;
}
