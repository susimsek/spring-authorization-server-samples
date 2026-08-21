export type ApiProblem = {
  detail?: string;
  errorCode?: string;
  violations?: Array<{ field: string }>;
};

export function problemViolations(data: unknown) {
  if (typeof data !== "object" || data === null || !("violations" in data)) return [];
  const violations = (data as ApiProblem).violations;
  return Array.isArray(violations) ? violations : [];
}

export function problemErrorCode(data: unknown) {
  if (typeof data !== "object" || data === null || !("errorCode" in data)) return undefined;
  const { errorCode } = data as ApiProblem;
  return typeof errorCode === "string" ? errorCode : undefined;
}
