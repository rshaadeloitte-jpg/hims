import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

// Polyfill localStorage for jsdom environment
if (typeof localStorage === "undefined") {
  const storage = new Map<string, string>();
  (globalThis as any).localStorage = {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
    clear: () => storage.clear(),
    key: (index: number) => Array.from(storage.keys())[index] ?? null,
    get length() { return storage.size; },
  };
}

// Polyfill HTMLDialogElement for jsdom environment
if (typeof HTMLDialogElement !== "undefined") {
  HTMLDialogElement.prototype.showModal = HTMLDialogElement.prototype.showModal || function (this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.show = HTMLDialogElement.prototype.show || function (this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = HTMLDialogElement.prototype.close || function (this: HTMLDialogElement) {
    this.open = false;
  };
}

// Global Chakra UI v3 wrapper for @testing-library/react render
vi.mock("@testing-library/react", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@testing-library/react")>();
  const React = await import("react");
  const { Provider: ChakraUiProvider } = await import("../components/ui/provider");

  return {
    ...actual,
    render: (ui: React.ReactElement, options?: any) => {
      const UserWrapper = options?.wrapper;
      const AllTheProviders = ({ children }: { children: React.ReactNode }) => {
        const inner = UserWrapper ? React.createElement(UserWrapper, null, children) : children;
        return React.createElement(ChakraUiProvider, null, inner);
      };
      return actual.render(ui, { ...options, wrapper: AllTheProviders });
    },
  };
});

// Unmount React trees between tests so queries never leak across cases.
afterEach(() => {
  cleanup();
});
