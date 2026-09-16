import { afterAll, afterEach, beforeAll, beforeEach } from "vitest";
import { server } from "./mocks/server";
import { resetOpSession } from "../store/opSession";

// Boot MSW once per test file; reset handlers between cases so a test
// adding a custom handler doesn't leak into the next.
beforeAll(() => server.listen({ onUnhandledRequest: "bypass" }));
afterAll(() => server.close());
beforeEach(() => {
  server.resetHandlers();
  // Reset Zustand store between cases — without this, walk-in draft state
  // from a previous test would bleed into the next.
  resetOpSession();
});
afterEach(() => {
  server.resetHandlers();
});
