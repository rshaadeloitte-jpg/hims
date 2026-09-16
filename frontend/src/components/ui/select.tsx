import { NativeSelect } from "@chakra-ui/react";
import { forwardRef, type SelectHTMLAttributes } from "react";

/**
 * Brand-styled native select. Renders a real `<select>` so it stays
 * compatible with react-hook-form `{...register("field")}` (ref + change
 * wiring preserved). For custom dropdown UI use the Ark-based
 * `Select` compound instead — but that needs controlled wiring.
 */
export const HimsSelect = forwardRef<
  HTMLSelectElement,
  SelectHTMLAttributes<HTMLSelectElement> & { placeholder?: string }
>(function HimsSelect({ placeholder, children, ...rest }, ref) {
  return (
    <NativeSelect.Root>
      <NativeSelect.Field
        ref={ref}
        placeholder={placeholder}
        bg="white"
        border="1px solid"
        borderColor="gray.300"
        _focus={{ borderColor: "brand.600" }}
        {...rest}
      >
        {children}
      </NativeSelect.Field>
      <NativeSelect.Indicator />
    </NativeSelect.Root>
  );
});
