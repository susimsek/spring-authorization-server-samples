"use client";

import { faCheck, faShieldHalved, faXmark } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { zodResolver } from "@hookform/resolvers/zod";
import axios from "axios";
import { type FormEvent, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Alert, Badge, Button, Card, Form, Spinner, Stack } from "react-bootstrap";
import { z } from "zod";

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

    axios
      .get<ConsentView>(`/api/authorization/consent${window.location.search}`, {
        signal: controller.signal,
      })
      .then((response) => {
        return response.data;
      })
      .then(setConsent)
      .catch((error: unknown) => {
        if (axios.isCancel(error)) {
          return;
        }
        setFailed(true);
        const errorPath = window.location.pathname.replace(/\/consent$/, "/error");
        window.location.replace(`${errorPath}?type=invalid_request`);
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
  const schema = z.object({
    scopes: z.array(z.string()).min(1, dictionary.consent.scopeRequired),
  });
  const {
    handleSubmit,
    getValues,
    setValue,
    formState: { errors },
  } = useForm<{ scopes: string[] }>({
    resolver: zodResolver(schema),
    defaultValues: { scopes: consent.scopes },
    mode: "onChange",
  });

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    void handleSubmit(() => form.submit())(event);
  };

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

      <Form method="post" action={consent.requestUri} onSubmit={submit} noValidate>
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
                  isInvalid={Boolean(errors.scopes)}
                  name="scope"
                  value={scope}
                  label={scope}
                  defaultChecked
                  onChange={(event) => {
                    const scopes = getValues("scopes");
                    const nextScopes = event.target.checked
                      ? [...scopes, scope]
                      : scopes.filter((value) => value !== scope);
                    setValue("scopes", nextScopes, { shouldValidate: true });
                  }}
                />
                <div className="small text-body-secondary ms-4 mt-1">
                  {scopeDescription(dictionary, scope)}
                </div>
              </div>
            ))}
          </Stack>
          {errors.scopes && (
            <div className="invalid-feedback d-block mt-2">{errors.scopes.message}</div>
          )}
        </div>

        <Button type="submit" size="lg" className="w-100">
          <span className="me-2">{dictionary.consent.submit}</span>
          <FontAwesomeIcon icon={faCheck} />
        </Button>
      </Form>

      <Form method="post" action={consent.requestUri} className="mt-2">
        <input type="hidden" name="client_id" value={consent.clientId} />
        <input type="hidden" name="state" value={consent.state} />
        {consent.userCode && <input type="hidden" name="user_code" value={consent.userCode} />}
        <Button type="submit" size="lg" variant="outline-secondary" className="w-100">
          <span className="me-2">{dictionary.consent.deny}</span>
          <FontAwesomeIcon icon={faXmark} />
        </Button>
      </Form>
    </>
  );
}

function scopeDescription(dictionary: Dictionary, scope: string) {
  const descriptions = dictionary.consent.scopeDescriptions as Record<string, string>;
  return descriptions[scope] ?? dictionary.consent.scopeDefault;
}
