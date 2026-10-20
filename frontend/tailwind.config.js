/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  corePlugins: {
    preflight: false, // Preserve Chakra UI & global.css base styles
  },
  theme: {
    extend: {
      colors: {
        deloitte: {
          green: "#86BC25",
          forest: "#008040",
          black: "#111827",
        },
      },
    },
  },
  plugins: [],
};
