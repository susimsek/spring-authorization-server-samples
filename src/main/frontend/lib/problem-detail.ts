export type ApiProblem = {
  detail?: string;
  errorCode?: string;
  field?: string;
  violations?: Array<{ field: string; message?: string | null }>;
};

export function problemViolations(data: unknown) {
  if (typeof data !== "object" || data === null) return [];
  const problem = data as ApiProblem;
  if (Array.isArray(problem.violations)) return problem.violations;
  return typeof problem.field === "string"
    ? [
        {
          field: problem.field,
          message: typeof problem.detail === "string" ? problem.detail : undefined,
        },
      ]
    : [];
}

export function problemErrorCode(data: unknown) {
  if (typeof data !== "object" || data === null || !("errorCode" in data)) return undefined;
  const { errorCode } = data as ApiProblem;
  return typeof errorCode === "string" ? errorCode : undefined;
}
