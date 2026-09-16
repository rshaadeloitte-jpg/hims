import { Field, Input, type InputProps } from "@chakra-ui/react";
import { forwardRef, isValidElement, type ReactNode } from "react";

function findInputId(node: ReactNode): string | undefined {
  if (!node || typeof node !== "object") return undefined;
  if (isValidElement(node)) {
    const props = node.props as Record<string, unknown> | undefined;
    if (typeof props?.id === "string") return props.id;
    if (props?.children) return findInputId(props.children as ReactNode);
  }
  if (Array.isArray(node)) {
    for (const child of node) {
      const id = findInputId(child);
      if (id) return id;
    }
  }
  return undefined;
}

/**
 * Label + input + error wrapper for react-hook-form fields.
 * Replaces hand-rolled label/error markup in feature pages.
 */
export function FormField({
  label,
  error,
  helper,
  required = false,
  children,
  id,
  htmlFor,
}: {
  label: string;
  error?: string;
  helper?: string;
  required?: boolean;
  children: ReactNode;
  id?: string;
  htmlFor?: string;
}) {
  const fieldId = id ?? htmlFor ?? findInputId(children);
  return (
    <Field.Root invalid={!!error} required={required} id={fieldId}>
      <Field.Label fontSize="sm" fontWeight="medium" htmlFor={fieldId}>
        {label}
      </Field.Label>
      {children}
      {error ? (
        <Field.ErrorText>{error}</Field.ErrorText>
      ) : helper ? (
        <Field.HelperText>{helper}</Field.HelperText>
      ) : null}
    </Field.Root>
  );
}

export const TextInput = forwardRef<HTMLInputElement, InputProps>(
  function TextInput(props, ref) {
    return (
      <Input
        ref={ref}
        bg="white"
        borderColor="gray.300"
        _focus={{ borderColor: "brand.600" }}
        {...props}
      />
    );
  }
);
