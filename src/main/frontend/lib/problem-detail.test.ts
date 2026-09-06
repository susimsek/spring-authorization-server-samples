import { problemErrorCode, problemViolations } from "./problem-detail";

describe("problem detail helpers", () => {
  it("extracts valid violations and the error code", () => {
    const problem = {
      errorCode: "validation_failed",
      violations: [{ field: "clientId", message: "Client ID is required." }],
    };

    expect(problemViolations(problem)).toEqual([
      { field: "clientId", message: "Client ID is required." },
    ]);
    expect(problemErrorCode(problem)).toBe("validation_failed");
  });

  it("exposes a field-specific business error as a single violation", () => {
    expect(
      problemViolations({ detail: "Username is already registered.", field: "username" }),
    ).toEqual([{ field: "username", message: "Username is already registered." }]);
  });

  it("rejects malformed problem detail properties", () => {
    expect(problemViolations(null)).toEqual([]);
    expect(problemViolations({ violations: "clientId" })).toEqual([]);
    expect(problemErrorCode(null)).toBeUndefined();
    expect(problemErrorCode({ errorCode: 400 })).toBeUndefined();
  });
});
