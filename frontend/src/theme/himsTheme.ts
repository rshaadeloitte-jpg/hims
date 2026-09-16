import {
  createSystem,
  defaultConfig,
  defineConfig,
  defineRecipe,
} from "@chakra-ui/react";

/**
 * HIMS Enterprise design system for Chakra UI v3.
 * Carries the Deloitte brand tokens (previously tokens.css) into Chakra
 * so every migrated component gets brand greens, type and buttons
 * from the theme instead of hand-written CSS.
 */

const FONT_SANS =
  `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif`;

const buttonRecipe = defineRecipe({
  className: "hims-button",
  base: {
    fontWeight: "semibold",
    borderRadius: "md",
    cursor: "pointer",
    _disabled: {
      opacity: 0.5,
      cursor: "not-allowed",
    },
  },
  variants: {
    variant: {
      // Deloitte black primary — turns forest green on hover.
      primary: {
        bg: "black",
        color: "white",
        border: "1px solid",
        borderColor: "black",
        _hover: { bg: "forest", borderColor: "forest", color: "white" },
      },
      // Neutral secondary action.
      secondary: {
        bg: "white",
        color: "black",
        border: "1px solid",
        borderColor: "gray.300",
        _hover: { borderColor: "black", bg: "gray.50" },
      },
      ghost: {
        bg: "transparent",
        color: "black",
        _hover: { bg: "gray.100" },
      },
      // Brand-green solid for success / go actions.
      solid: {
        bg: "brand.500",
        color: "white",
        _hover: { bg: "brand.600" },
      },
      outline: {
        bg: "transparent",
        color: "brand.700",
        border: "1px solid",
        borderColor: "brand.500",
        _hover: { bg: "brand.50" },
      },
      subtle: {
        bg: "brand.100",
        color: "brand.800",
        _hover: { bg: "brand.200" },
      },
    },
    size: {
      sm: { h: "8", px: "3", fontSize: "sm" },
      md: { h: "10", px: "4", fontSize: "sm" },
      lg: { h: "11", px: "6", fontSize: "md" },
    },
  },
  defaultVariants: {
    variant: "primary",
    size: "md",
  },
});

const himsConfig = defineConfig({
  globalCss: {
    "html, body": {
      margin: 0,
      padding: 0,
    },
    body: {
      fontFamily: FONT_SANS,
      fontSize: "14px",
      lineHeight: 1.5,
      color: "#111827",
      background: "#F4F6F8",
    },
    ":focus-visible": {
      outline: "2px solid {colors.brand.500}",
      outlineOffset: "2px",
    },
  },
  theme: {
    tokens: {
      colors: {
        // Deloitte primary green ramp, anchored at #86BC25.
        brand: {
          50: { value: "#f5fae9" },
          100: { value: "#e9f4cf" },
          200: { value: "#d4e9a6" },
          300: { value: "#b7da74" },
          400: { value: "#9ccb46" },
          500: { value: "#86bc25" },
          600: { value: "#699a1c" },
          700: { value: "#527a19" },
          800: { value: "#416117" },
          900: { value: "#374f16" },
          950: { value: "#1a280a" },
        },
        // Deloitte forest green (single-step accent).
        forest: { value: "#008040" },
      },
      fonts: {
        body: { value: FONT_SANS },
        heading: { value: FONT_SANS },
      },
    },
    semanticTokens: {
      colors: {
        // App surfaces so pages read `bg="surface"` etc.
        canvas: { value: "#F4F6F8" },
        surface: { value: "#FFFFFF" },
        muted: { value: "#F8FAFC" },
      },
    },
    recipes: {
      button: buttonRecipe,
    },
  },
});

export const himsSystem = createSystem(defaultConfig, himsConfig);

/** Alias named `system` for the Chakra CLI typegen (expects this name). */
export const system = himsSystem;
