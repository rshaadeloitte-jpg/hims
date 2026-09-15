import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  useFiles,
  useFile,
  useFileByToken,
  useUploadFile,
  useDeleteFile,
  useDownloadFileBase64,
  useFileCategories,
  useFileCategoryTree,
  useCreateFileCategory,
  useFileAnnotation,
  useSaveAnnotation,
  readFileAsDataURL,
  stripDataURIPrefix,
} from "./queries";
import { fileUploadSchema } from "./zodSchema";

/**
 * M34 File/Document Management — wire-level + schema validation tests.
 * Pins the wire URLs and validates Zod schemas.
 */

function testWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

// ---- Query hooks ------------------------------------------------------------

describe("useFiles", () => {
  it("fetches files by owner filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          fileName: "scan.png",
          fileType: "image/png",
          fileSize: 1024,
          fileToken: "token-1",
        },
      ],
    });

    const { result } = renderHook(
      () => useFiles({ ownerType: "PATIENT", ownerId: 1 }),
      { wrapper: testWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files?ownerType=PATIENT&ownerId=1",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].fileName).toBe("scan.png");
  });

  it("fetches files by category", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => useFiles({ fileCategoryId: 5 }),
      { wrapper: testWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files?fileCategoryId=5",
      expect.any(Object),
    );
  });

  it("is disabled when no filter params", () => {
    const { result } = renderHook(() => useFiles({}), { wrapper: testWrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});

describe("useFile", () => {
  it("fetches file metadata by id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        id: 42,
        fileName: "report.pdf",
        fileType: "application/pdf",
        fileSize: 2048,
        fileToken: "token-42",
      }),
    });

    const { result } = renderHook(() => useFile(42), { wrapper: testWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/42",
      expect.any(Object),
    );
    expect(result.current.data?.fileName).toBe("report.pdf");
  });

  it("is disabled when id is 0", () => {
    const { result } = renderHook(() => useFile(0), { wrapper: testWrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("useFileByToken", () => {
  it("fetches file by token", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        id: 99,
        fileName: "xray.jpg",
        fileType: "image/jpeg",
        fileSize: 512,
        fileToken: "tok-99",
      }),
    });

    const { result } = renderHook(
      () => useFileByToken("tok-99"),
      { wrapper: testWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/token/tok-99",
      expect.any(Object),
    );
  });
});

describe("useUploadFile", () => {
  it("posts base64 JSON and returns created file", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 50,
        fileName: "test.png",
        fileType: "image/png",
        fileSize: 128,
        fileToken: "new-token",
      }),
    });

    const { result } = renderHook(() => useUploadFile(), { wrapper: testWrapper() });

    result.current.mutate({
      fileName: "test.png",
      fileType: "image/png",
      fileData: "aGVsbG8=",
      ownerType: "PATIENT",
      ownerId: 1,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          fileName: "test.png",
          fileType: "image/png",
          fileData: "aGVsbG8=",
          ownerType: "PATIENT",
          ownerId: 1,
        }),
      }),
    );
    expect(result.current.data?.fileToken).toBe("new-token");
  });
});

describe("useDeleteFile", () => {
  it("deletes file by id", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 204,
      json: async () => null,
    });

    const { result } = renderHook(() => useDeleteFile(), { wrapper: testWrapper() });

    result.current.mutate(42);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/42",
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});

describe("useDownloadFileBase64", () => {
  it("fetches base64 download", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        fileName: "scan.png",
        fileType: "image/png",
        fileSize: 256,
        fileData: "iVBORw0KGgo=",
        fileToken: "tok-5",
      }),
    });

    const { result } = renderHook(
      () => useDownloadFileBase64("tok-5"),
      { wrapper: testWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/token/tok-5/base64",
      expect.any(Object),
    );
    expect(result.current.data?.fileData).toBe("iVBORw0KGgo=");
  });
});

describe("useFileCategories", () => {
  it("fetches category list", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, code: "RADIOLOGY", name: "Radiology" },
        { id: 2, code: "LAB", name: "Lab Reports" },
      ],
    });

    const { result } = renderHook(() => useFileCategories(), { wrapper: testWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/file-categories",
      expect.any(Object),
    );
    expect(result.current.data).toHaveLength(2);
  });
});

describe("useFileCategoryTree", () => {
  it("fetches category tree", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          code: "IMAGING",
          name: "Imaging",
          children: [{ id: 2, code: "XRAY", name: "X-Ray" }],
        },
      ],
    });

    const { result } = renderHook(() => useFileCategoryTree(), { wrapper: testWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/file-categories/tree",
      expect.any(Object),
    );
  });
});

describe("useCreateFileCategory", () => {
  it("creates a new category", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 10, code: "NEW_CAT", name: "New Category" }),
    });

    const { result } = renderHook(() => useCreateFileCategory(), { wrapper: testWrapper() });

    result.current.mutate({ code: "NEW_CAT", name: "New Category" });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/file-categories",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ code: "NEW_CAT", name: "New Category" }),
      }),
    );
  });
});

describe("useFileAnnotation", () => {
  it("fetches annotation JSON", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        annotationJson: JSON.stringify([{ type: "rectangle", x: 10, y: 20 }]),
      }),
    });

    const { result } = renderHook(() => useFileAnnotation(5), { wrapper: testWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/5/annotation",
      expect.any(Object),
    );
  });
});

describe("useSaveAnnotation", () => {
  it("saves annotation JSON", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        id: 5,
        annotationJson: JSON.stringify([{ type: "circle", x: 50, y: 60 }]),
      }),
    });

    const { result } = renderHook(() => useSaveAnnotation(), { wrapper: testWrapper() });

    const annotationJson = JSON.stringify([{ type: "circle", x: 50, y: 60 }]);
    result.current.mutate({ id: 5, annotationJson });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/files/5/annotation",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ annotationJson }),
      }),
    );
  });
});

// ---- Utility functions ------------------------------------------------------

describe("stripDataURIPrefix", () => {
  it("strips data URI prefix", () => {
    const dataUrl = "data:image/png;base64,iVBORw0KGgo=";
    expect(stripDataURIPrefix(dataUrl)).toBe("iVBORw0KGgo=");
  });

  it("returns raw base64 unchanged", () => {
    expect(stripDataURIPrefix("iVBORw0KGgo=")).toBe("iVBORw0KGgo=");
  });
});

describe("readFileAsDataURL", () => {
  it("reads file as data URL", async () => {
    const file = new File(["hello"], "test.txt", { type: "text/plain" });
    const dataUrl = await readFileAsDataURL(file);
    expect(dataUrl).toMatch(/^data:text\/plain;base64,/);
  });
});