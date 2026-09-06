import { useEffect, useRef } from "react";
import {
  useForm as useReactHookForm,
  useWatch,
  type Control,
  type FieldErrors,
  type FieldValues,
  type Path,
  type UseFormClearErrors,
  type UseFormProps,
  type UseFormReturn,
} from "react-hook-form";

function errorPaths(errors: FieldErrors, prefix = ""): string[] {
  return Object.entries(errors).flatMap(([name, error]) => {
    if (!error || typeof error !== "object") return [];
    const path = prefix ? `${prefix}.${name}` : name;
    if ("type" in error) return error.type === "server" ? [path] : [];
    return errorPaths(error as FieldErrors, path);
  });
}

function readPath(value: unknown, path: string): unknown {
  return path.split(/[.[\]]+/).reduce<unknown>((current, segment) => {
    if (!segment || current === null || typeof current !== "object") return undefined;
    return (current as Record<string, unknown>)[segment];
  }, value);
}

function equalValues(previous: unknown, current: unknown): boolean {
  if (Object.is(previous, current)) return true;
  if (
    previous === null ||
    current === null ||
    typeof previous !== "object" ||
    typeof current !== "object"
  ) {
    return false;
  }
  try {
    return JSON.stringify(previous) === JSON.stringify(current);
  } catch {
    return false;
  }
}

function useClearServerErrorsOnChange<
  TFieldValues extends FieldValues,
  TContext = unknown,
  TTransformedValues extends FieldValues | undefined = TFieldValues,
>(
  control: Control<TFieldValues, TContext, TTransformedValues>,
  errors: FieldErrors<TFieldValues>,
  clearErrors: UseFormClearErrors<TFieldValues>,
) {
  const values = useWatch<TFieldValues, TTransformedValues>({ control });
  const previousValues = useRef<unknown>(values);

  useEffect(() => {
    for (const path of errorPaths(errors)) {
      if (!equalValues(readPath(previousValues.current, path), readPath(values, path))) {
        clearErrors(path as Path<TFieldValues>);
      }
    }
    previousValues.current = values;
  }, [clearErrors, errors, values]);
}

export function useForm<
  TFieldValues extends FieldValues = FieldValues,
  TContext = unknown,
  TTransformedValues extends FieldValues | undefined = TFieldValues,
>(
  props?: UseFormProps<TFieldValues, TContext, TTransformedValues>,
): UseFormReturn<TFieldValues, TContext, TTransformedValues> {
  const form = useReactHookForm<TFieldValues, TContext, TTransformedValues>(props);
  useClearServerErrorsOnChange(form.control, form.formState.errors, form.clearErrors);
  return form;
}
