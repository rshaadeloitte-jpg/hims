// Polyfill crypto.randomUUID for non-secure contexts (plain HTTP over IP).
if (typeof crypto !== "undefined" && typeof crypto.randomUUID !== "function") {
  (crypto as { randomUUID?: () => string }).randomUUID = () =>
    "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
}

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { Provider } from "./components/ui/provider";
import "./styles/tokens.css";
import "./styles/global.css";
import "./styles/op.css";
import "./styles/patient.css";
import "./styles/queue.css";
import "./styles/grievance.css";
import "./styles/ejs.css";

// Stale-while-revalidate defaults tuned for master data (rarely changes);
// devtools/retry stay modest so API errors surface quickly in the UI.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 0,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <Provider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </Provider>
    </QueryClientProvider>
  </StrictMode>,
);
