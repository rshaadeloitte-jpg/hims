import {
  createSystem,
  defaultConfig,
  defineConfig,
  defineRecipe,
} from "@chakra-ui/react";

const FONT_SANS =
  `'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`;

const FONT_HEADING =
  `'Plus Jakarta Sans', 'Inter', sans-serif`;

/**
 * Contrast-safe color assignments per WCAG 4.5:1 for normal text.
 * Brand green (#86BC25) is accent-only: rails, dots, logos, decorative highlights.
 * Never use brand.500 for body text, labels, or focus rings.
 */
const buttonRecipe = defineRecipe({
  className: "hims-button",
  base: {
    fontWeight: "600",
    borderRadius: "md",
    cursor: "pointer",
    transition: "all 0.22s ease",
    _disabled: {
      opacity: 0.6,
      cursor: "not-allowed",
      transform: "none",
      shadow: "none",
    },
    _hover: {
      transform: "translateY(-1px)",
      shadow: "md",
    },
    _focusVisible: {
      outline: "2px solid",
      outlineColor: "border.focus",
      outlineOffset: "2px",
    },
  },
  variants: {
    variant: {
      // Primary action: Deloitte black for main decisions
      primary: {
        bg: "deloitte.black",
        color: "white",
        border: "1px solid",
        borderColor: "deloitte.black",
        _hover: { bg: "deloitte.warm", borderColor: "deloitte.warm" },
      },
      // Confirmation/completion: readable green
      success: {
        bg: "success.DEFAULT",
        color: "white",
        border: "1px solid",
        borderColor: "success.DEFAULT",
        _hover: { bg: "forest", borderColor: "forest" },
      },
      // Neutral secondary action
      secondary: {
        bg: "white",
        color: "text.primary",
        border: "1px solid",
        borderColor: "border.strong",
        _hover: { borderColor: "text.primary", bg: "surface.muted" },
      },
      ghost: {
        bg: "transparent",
        color: "text.primary",
        _hover: { bg: "surface.muted" },
      },
      // Brand-green for non-critical actions (darker for readability)
      solid: {
        bg: "brand.700",
        color: "white",
        _hover: { bg: "brand.800" },
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
      // TMS teal accent variant
      accent: {
        bg: "teal.DEFAULT",
        color: "white",
        _hover: { bg: "teal.dark" },
      },
      // Danger/destructive
      danger: {
        bg: "danger.DEFAULT",
        color: "white",
        border: "1px solid",
        borderColor: "danger.DEFAULT",
        _hover: { bg: "#B91C1C" },
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
      fontSize: "13.5px",
      lineHeight: 1.5,
      color: "text.primary",
      background: "surface.canvas",
    },
    ":focus-visible": {
      outline: "2px solid {colors.border.focus}",
      outlineOffset: "2px",
    },
  },
  theme: {
    tokens: {
      colors: {
        // ── Deloitte Brand Palette (accent-only for #86BC25) ─────────────
        brand: {
          50: { value: "#f5fae9" },
          100: { value: "#e9f4cf" },
          200: { value: "#d4e9a6" },
          300: { value: "#b7da74" },
          400: { value: "#9ccb46" },
          500: { value: "#86bc25" },  // ACCENT ONLY: rails, dots, logos
          600: { value: "#699a1c" },
          700: { value: "#527a19" },  // Readable green text minimum
          800: { value: "#416117" },
          900: { value: "#374f16" },
          950: { value: "#1a280a" },
        },
        // ── Readable Greens (for text, actions, status) ──────────────────
        forest: { value: "#008040" },      // Success text minimum
        success: {
          DEFAULT: { value: "#15803D" },   // WCAG 4.5:1 on white
          soft: { value: "#DCFCE7" },
        },
        warning: {
          DEFAULT: { value: "#B45309" },   // WCAG 4.5:1 on white
          soft: { value: "#FEF3C7" },
        },
        danger: {
          DEFAULT: { value: "#DC2626" },
          soft: { value: "#FEE2E2" },
        },
        // ── TMS Deloitte Teal Accent ─────────────────────────────────────
        teal: {
          DEFAULT: { value: "#007680" },
          dark: { value: "#005C63" },
        },
        // ── TMS Deloitte Gold Accent ─────────────────────────────────────
        gold: { value: "#FFB81C" },
        // ── Deloitte Blacks ──────────────────────────────────────────────
        deloitte: {
          black: { value: "#1F1F1F" },
          warm: { value: "#2C2C2C" },
        },
        // ── Semantic Text Colors (contrast-safe) ─────────────────────────
        text: {
          primary: { value: "#111827" },   // Main decisions, actions
          secondary: { value: "#4B5563" }, // Supporting info
          muted: { value: "#6B7280" },     // Helper text, placeholders
          inverse: { value: "#FFFFFF" },
        },
        // ── Semantic Surface Colors ──────────────────────────────────────
        surface: {
          canvas: { value: "#F4F5F7" },    // App background
          panel: { value: "#FFFFFF" },     // Cards, modals
          muted: { value: "#F8F9FA" },     // Subtle sections
          header: { value: "#FFFFFF" },    // Top header
          sidebar: { value: "#1F1F1F" },   // Navigation
        },
        // ── Semantic Border Colors ───────────────────────────────────────
        border: {
          DEFAULT: { value: "#E5E7EB" },   // Default structure
          focus: { value: "#527A19" },     // Focus state (dark green)
          strong: { value: "#D1D5DB" },    // Selected/active controls
        },
      },
      fonts: {
        body: { value: FONT_SANS },
        heading: { value: FONT_HEADING },
      },
      shadows: {
        xs: { value: "0 1px 2px rgba(31, 31, 31, 0.06)" },
        sm: { value: "0 1px 4px rgba(31, 31, 31, 0.08), 0 2px 8px rgba(31, 31, 31, 0.04)" },
        md: { value: "0 4px 16px rgba(31, 31, 31, 0.1), 0 2px 6px rgba(31, 31, 31, 0.06)" },
        lg: { value: "0 8px 32px rgba(31, 31, 31, 0.14), 0 4px 12px rgba(31, 31, 31, 0.08)" },
        xl: { value: "0 20px 56px rgba(31, 31, 31, 0.2)" },
        primary: { value: "0 4px 16px rgba(134, 188, 37, 0.28)" },
      },
    },
    semanticTokens: {
      colors: {
        canvas: { value: "{colors.surface.canvas}" },
        muted: { value: "{colors.surface.muted}" },
        headerBg: { value: "{colors.surface.header}" },
        sidebarBg: { value: "{colors.surface.sidebar}" },
      },
    },
    recipes: {
      button: buttonRecipe,
    },
  },
});

export const himsSystem = createSystem(defaultConfig, himsConfig);
export const system = himsSystem;