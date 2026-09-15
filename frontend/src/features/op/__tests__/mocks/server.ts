import { setupServer } from "msw/node";
import { handlers } from "./handlers";

/**
 * MSW Node test server — boots in beforeAll(), tears down in afterAll().
 * The handlers cover the three downstream services (patient :8090,
 * appointment :8091, queue :8089) plus the in-tree ABHA endpoint that
 * the OP screens call.
 */
export const server = setupServer(...handlers);
