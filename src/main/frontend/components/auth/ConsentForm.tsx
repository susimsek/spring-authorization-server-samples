"use client";

import { faCheck, faShieldHalved } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { Alert, Badge, Button, Card, Form, Stack } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";

type ConsentFormProps = {
  dictionary: Dictionary;
};

export function ConsentForm({ dictionary }: ConsentFormProps) {
  return (
    <Card className="border-0 shadow-sm">
      <Card.Body className="p-4 p-md-5">
        <Stack gap={1} className="mb-4">
          <span className="text-primary text-uppercase fw-semibold small">
            {dictionary.consent.eyebrow}
          </span>
          <h1 className="h3 fw-bold mb-1">{dictionary.consent.title}</h1>
          <p className="text-body-secondary mb-0">{dictionary.consent.subtitle}</p>
        </Stack>

        <Suspense fallback={null}>
          <ConsentRequest dictionary={dictionary} />
        </Suspense>

        <div className="border-top mt-4 pt-3 text-center"></div>
      </Card.Body>
    </Card>
  );
}

function ConsentRequest({ dictionary }: ConsentFormProps) {
  const searchParams = useSearchParams();
  const clientId = searchParams.get("client_id") ?? "";
  const state = searchParams.get("state") ?? "";
  const scopes = (searchParams.get("scope") ?? "")
    .split(/\s+/)
    .map((scope) => scope.trim())
    .filter(Boolean);

  if (!clientId || !state || scopes.length === 0) {
    return <Alert variant="danger">{dictionary.consent.invalidRequest}</Alert>;
  }

  return (
    <>
      <div className="bg-body-tertiary rounded-3 p-3 mb-4">
        <div className="d-flex align-items-center gap-3">
          <div className="consent-client-icon d-flex align-items-center justify-content-center rounded-circle bg-primary-subtle text-primary">
            <FontAwesomeIcon icon={faShieldHalved} />
          </div>
          <div className="min-w-0">
            <div className="small text-body-secondary">{dictionary.consent.clientLabel}</div>
            <div className="fw-semibold text-break">{clientId}</div>
          </div>
        </div>
      </div>

      <Form method="post" action="/oauth2/authorize">
        <input type="hidden" name="client_id" value={clientId} />
        <input type="hidden" name="state" value={state} />

        <div className="mb-4">
          <div className="d-flex align-items-center justify-content-between mb-2">
            <Form.Label className="fw-semibold mb-0">{dictionary.consent.permissions}</Form.Label>
            <Badge bg="secondary" pill>
              {scopes.length}
            </Badge>
          </div>
          <p className="small text-body-secondary mb-3">{dictionary.consent.permissionsHelp}</p>

          <Stack gap={2}>
            {scopes.map((scope) => (
              <div key={scope} className="border rounded-3 p-3">
                <Form.Check
                  id={`scope-${scope}`}
                  name="scope"
                  value={scope}
                  defaultChecked
                  label={scope}
                />
                <div className="small text-body-secondary ms-4 mt-1">
                  {scopeDescription(dictionary, scope)}
                </div>
              </div>
            ))}
          </Stack>
        </div>

        <Button type="submit" size="lg" className="w-100">
          <span className="me-2">{dictionary.consent.submit}</span>
          <FontAwesomeIcon icon={faCheck} />
        </Button>
      </Form>
    </>
  );
}

function scopeDescription(dictionary: Dictionary, scope: string) {
  const descriptions = dictionary.consent.scopeDescriptions as Record<string, string>;
  return descriptions[scope] ?? dictionary.consent.scopeDefault;
}
