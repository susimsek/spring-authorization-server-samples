import { problemErrorCode, problemViolations } from "./problem-detail";

describe("problem detail helpers", () => {
  it("extracts valid violations and the error code", () => {
    const problem = {
      errorCode: "validation_failed",
      violations: [{ field: "clientId" }],
    };

    expect(problemViolations(problem)).toEqual([{ field: "clientId" }]);
    expect(problemErrorCode(problem)).toBe("validation_failed");
  });

  it("rejects malformed problem detail properties", () => {
    expect(problemViolations(null)).toEqual([]);
    expect(problemViolations({ violations: "clientId" })).toEqual([]);
    expect(problemErrorCode(null)).toBeUndefined();
    expect(problemErrorCode({ errorCode: 400 })).toBeUndefined();
  });
});
