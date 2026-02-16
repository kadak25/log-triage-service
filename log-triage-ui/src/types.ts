export type ErrorSignature = {
  exceptionType: string;
  message: string;
  count: number;
};

export type AnalyzeResponse = {
  severity: "LOW" | "MED" | "HIGH";
  detectedIssues: string[];
  possibleRootCause: string;
  nextSteps: string[];
  topErrorSignatures: ErrorSignature[];
  detectedIds?: string[];
  ticketTitle?: string;
  ticketBody?: string;
  suggestedGrepQueries?: string[];
  aiUsed?: boolean;
  aiProvider?: string;
  aiError?: string | null;
  aiLatencyMs?: number | null;
};
