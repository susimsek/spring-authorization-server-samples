"use client";

import { faCheck, faShieldHalved } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useEffect, useState } from "react";
import { Alert, Badge, Button, Card, Form, Spinner, Stack } from "react-bootstrap";

import type { Dictionary } from "@/i18n/get-dictionary";

type ConsentFormProps = {
  dictionary: Dictionary;
};

type ConsentView = {
  clientId: string;
  state: string;
  scopes: string[];
  previouslyApprovedScopes: string[];
  principalName: string;
  userCode: string | null;
  requestUri: string;
};

export function ConsentForm({ dictionary }: ConsentFormProps) {
  const [consent, setConsent] = useState<ConsentView | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetch(`/api/authorization/consent${window.location.search}`, {
      credentials: "same-origin",
      headers: { Accept: "application/json" },
      signal: controller.signal,
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Consent request failed");
        }
        return response.json() as Promise<ConsentView>;
      })
      .then(setConsent)
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        setFailed(true);
      });

    return () => controller.abort();
  }, []);

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

        {failed && <Alert variant="danger">{dictionary.consent.invalidRequest}</Alert>}
        {!failed && !consent && (
          <div className="d-flex justify-content-center py-4">
            <Spinner animation="border" role="status">
              <span className="visually-hidden">Loading</span>
            </Spinner>
          </div>
        )}
        {consent && <ConsentRequest dictionary={dictionary} consent={consent} />}
      </Card.Body>
    </Card>
  );
}

function ConsentRequest({ dictionary, consent }: ConsentFormProps & { consent: ConsentView }) {
  return (
    <>
      <div className="bg-body-tertiary rounded-3 p-3 mb-4">
        <div className="d-flex align-items-center gap-3">
          <div className="consent-client-icon d-flex align-items-center justify-content-center rounded-circle bg-primary-subtle text-primary">
            <FontAwesomeIcon icon={faShieldHalved} />
          </div>
          <div className="min-w-0">
            <div className="small text-body-secondary">{dictionary.consent.clientLabel}</div>
            <div className="fw-semibold text-break">{consent.clientId}</div>
          </div>
        </div>
      </div>

      <Form method="post" action={consent.requestUri}>
        <input type="hidden" name="client_id" value={consent.clientId} />
        <input type="hidden" name="state" value={consent.state} />
        {consent.userCode && <input type="hidden" name="user_code" value={consent.userCode} />}

        {consent.previouslyApprovedScopes.map((scope) => (
          <input key={scope} type="hidden" name="scope" value={scope} />
        ))}

        <div className="mb-4">
          <div className="d-flex align-items-center justify-content-between mb-2">
            <Form.Label className="fw-semibold mb-0">{dictionary.consent.permissions}</Form.Label>
            <Badge bg="secondary" pill>
              {consent.scopes.length}
            </Badge>
          </div>
          <p className="small text-body-secondary mb-3">{dictionary.consent.permissionsHelp}</p>

          <Stack gap={2}>
            {consent.scopes.map((scope) => (
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
