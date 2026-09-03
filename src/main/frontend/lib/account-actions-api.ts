import type { Dictionary } from "@/i18n/get-dictionary";
import { problemErrorCode } from "@/lib/problem-detail";

type AccountAction = "forgot-password" | "reset-password" | "verify-email";

export async function submitAccountAction(action: AccountAction, body: unknown) {
  const response = await fetch(`/api/auth/${action}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw await response.json().catch(() => new Error("Account action failed"));
  }
}

export function accountActionError(error: unknown, dictionary: Dictionary) {
  const copy = dictionary.accountActions;
  switch (problemErrorCode(error)) {
    case "action_email_unavailable":
      return copy.mailUnavailable;
    case "action_email_required":
      return copy.mailRequired;
    case "action_email_cooldown":
      return copy.cooldown;
    case "action_token_invalid":
    case "action_token_expired":
    case "action_token_used":
      return copy.invalidLink;
    default:
      return copy.operationError;
  }
}
