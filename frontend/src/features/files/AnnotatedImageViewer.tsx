import { useState, useRef, useCallback, useEffect } from "react";
import { Button } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import { useFileAnnotation, useSaveAnnotation, useDownloadFileBase64 } from "./queries";
import { annotationSchema } from "./zodSchema";

/**
 * M34 AnnotatedImageViewer — Image annotation view (ImageAnnotationMaster parity).
 *
 * Features:
 * - Load image from file token (base64)
 * - Draw rectangles, circles, arrows, freehand
 * - Add text labels
 * - Color picker for annotations
 * - Undo/redo support
 * - Save annotation JSON to backend
 * - Export annotated image
 *
 * Stripe-style: clean canvas UI, real-time feedback, accessible controls.
 */

type AnnotationTool = "rectangle" | "circle" | "arrow" | "freehand" | "text" | "select";

interface Annotation {
  id: string;
  type: AnnotationTool;
  x: number;
  y: number;
  width?: number;
  height?: number;
  radius?: number;
  endX?: number;
  endY?: number;
  points?: Array<{ x: number; y: number }>;
  text?: string;
  color: string;
  strokeWidth: number;
}

const COLORS = ["#ef4444", "#f59e0b", "#10b981", "#3b82f6", "#8b5cf6", "#ec4899", "#000000", "#ffffff"];
const STROKE_WIDTHS = [2, 3, 5, 8];

export function AnnotatedImageViewer({
  fileId,
  fileToken,
  onClose,
}: {
  fileId: number;
  fileToken: string;
  onClose: () => void;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const [tool, setTool] = useState<AnnotationTool>("rectangle");
  const [color, setColor] = useState("#ef4444");
  const [strokeWidth, setStrokeWidth] = useState(3);
  const [annotations, setAnnotations] = useState<Annotation[]>([]);
  const [currentAnnotation, setCurrentAnnotation] = useState<Annotation | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [history, setHistory] = useState<Annotation[][]>([[]]);
  const [historyIndex, setHistoryIndex] = useState(0);
  const [textInput, setTextInput] = useState("");
  const [showTextDialog, setShowTextDialog] = useState(false);
  const [textPosition, setTextPosition] = useState({ x: 0, y: 0 });
  const [error, setError] = useState<string | null>(null);

  const annotationQuery = useFileAnnotation(fileId);
  const downloadQuery = useDownloadFileBase64(fileToken);
  const saveAnnotation = useSaveAnnotation();

  // Load existing annotation
  useEffect(() => {
    if (annotationQuery.data?.annotationJson) {
      try {
        const parsed = JSON.parse(annotationQuery.data.annotationJson);
        if (Array.isArray(parsed)) {
          setAnnotations(parsed);
          setHistory([parsed]);
          setHistoryIndex(0);
        }
      } catch {
        // Invalid JSON — start fresh
      }
    }
  }, [annotationQuery.data]);

  // Load image
  useEffect(() => {
    if (downloadQuery.data?.fileData) {
      const img = new Image();
      img.onload = () => {
        imageRef.current = img;
        redrawCanvas();
      };
      img.src = `data:${downloadQuery.data.fileType};base64,${downloadQuery.data.fileData}`;
    }
  }, [downloadQuery.data]);

  const redrawCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    const img = imageRef.current;
    if (!canvas || !img) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    canvas.width = img.naturalWidth;
    canvas.height = img.naturalHeight;
    ctx.drawImage(img, 0, 0);

    // Draw saved annotations
    for (const ann of annotations) {
      drawAnnotation(ctx, ann);
    }

    // Draw current annotation
    if (currentAnnotation) {
      drawAnnotation(ctx, currentAnnotation);
    }
  }, [annotations, currentAnnotation]);

  const drawAnnotation = (ctx: CanvasRenderingContext2D, ann: Annotation) => {
    ctx.strokeStyle = ann.color;
    ctx.lineWidth = ann.strokeWidth;
    ctx.fillStyle = ann.color;

    switch (ann.type) {
      case "rectangle":
        if (ann.width !== undefined && ann.height !== undefined) {
          ctx.strokeRect(ann.x, ann.y, ann.width, ann.height);
        }
        break;
      case "circle":
        if (ann.radius !== undefined) {
          ctx.beginPath();
          ctx.arc(ann.x, ann.y, ann.radius, 0, Math.PI * 2);
          ctx.stroke();
        }
        break;
      case "arrow":
        if (ann.endX !== undefined && ann.endY !== undefined) {
          drawArrow(ctx, ann.x, ann.y, ann.endX, ann.endY);
        }
        break;
      case "freehand":
        if (ann.points && ann.points.length > 1) {
          ctx.beginPath();
          ctx.moveTo(ann.points[0].x, ann.points[0].y);
          for (let i = 1; i < ann.points.length; i++) {
            ctx.lineTo(ann.points[i].x, ann.points[i].y);
          }
          ctx.stroke();
        }
        break;
      case "text":
        if (ann.text) {
          ctx.font = `${ann.strokeWidth * 6 + 12}px sans-serif`;
          ctx.fillText(ann.text, ann.x, ann.y);
        }
        break;
    }
  };

  const drawArrow = (ctx: CanvasRenderingContext2D, x1: number, y1: number, x2: number, y2: number) => {
    const headLength = 15;
    const angle = Math.atan2(y2 - y1, x2 - x1);

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - headLength * Math.cos(angle - Math.PI / 6), y2 - headLength * Math.sin(angle - Math.PI / 6));
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - headLength * Math.cos(angle + Math.PI / 6), y2 - headLength * Math.sin(angle + Math.PI / 6));
    ctx.stroke();
  };

  const getCanvasCoords = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
      x: (e.clientX - rect.left) * scaleX,
      y: (e.clientY - rect.top) * scaleY,
    };
  };

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (tool === "select") return;

    const coords = getCanvasCoords(e);

    if (tool === "text") {
      setTextPosition(coords);
      setShowTextDialog(true);
      return;
    }

    setIsDrawing(true);
    setCurrentAnnotation({
      id: crypto.randomUUID(),
      type: tool,
      x: coords.x,
      y: coords.y,
      color,
      strokeWidth,
      ...(tool === "circle" ? { radius: 0 } : {}),
      ...(tool === "freehand" ? { points: [coords] } : {}),
      ...(tool === "arrow" ? { endX: coords.x, endY: coords.y } : {}),
      ...(tool === "rectangle" ? { width: 0, height: 0 } : {}),
    });
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing || !currentAnnotation) return;

    const coords = getCanvasCoords(e);

    if (currentAnnotation.type === "rectangle") {
      setCurrentAnnotation({
        ...currentAnnotation,
        width: coords.x - currentAnnotation.x,
        height: coords.y - currentAnnotation.y,
      });
    } else if (currentAnnotation.type === "circle") {
      const dx = coords.x - currentAnnotation.x;
      const dy = coords.y - currentAnnotation.y;
      setCurrentAnnotation({
        ...currentAnnotation,
        radius: Math.sqrt(dx * dx + dy * dy),
      });
    } else if (currentAnnotation.type === "arrow") {
      setCurrentAnnotation({
        ...currentAnnotation,
        endX: coords.x,
        endY: coords.y,
      });
    } else if (currentAnnotation.type === "freehand" && currentAnnotation.points) {
      setCurrentAnnotation({
        ...currentAnnotation,
        points: [...currentAnnotation.points, coords],
      });
    }

    redrawCanvas();
  };

  const handleMouseUp = () => {
    if (!isDrawing || !currentAnnotation) return;

    const newAnnotations = [...annotations, currentAnnotation];
    setAnnotations(newAnnotations);

    // Update history
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(newAnnotations);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);

    setCurrentAnnotation(null);
    setIsDrawing(false);
    redrawCanvas();
  };

  const handleTextSubmit = () => {
    if (!textInput.trim()) {
      setShowTextDialog(false);
      return;
    }

    const newAnnotation: Annotation = {
      id: crypto.randomUUID(),
      type: "text",
      x: textPosition.x,
      y: textPosition.y,
      text: textInput,
      color,
      strokeWidth,
    };

    const newAnnotations = [...annotations, newAnnotation];
    setAnnotations(newAnnotations);

    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(newAnnotations);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);

    setTextInput("");
    setShowTextDialog(false);
    redrawCanvas();
  };

  const undo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1;
      setHistoryIndex(newIndex);
      setAnnotations(history[newIndex]);
      redrawCanvas();
    }
  };

  const redo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1;
      setHistoryIndex(newIndex);
      setAnnotations(history[newIndex]);
      redrawCanvas();
    }
  };

  const clearAll = () => {
    setAnnotations([]);
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push([]);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);
    redrawCanvas();
  };

  const handleSave = async () => {
    try {
      const annotationJson = JSON.stringify(annotations);
      // Validate with Zod
      annotationSchema.parse({ annotationJson });
      await saveAnnotation.mutateAsync({ id: fileId, annotationJson });
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save annotation");
    }
  };

  const handleExport = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const link = document.createElement("a");
    link.download = `annotated-${fileId}.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
  };

  return (
    <div className="annotated-viewer" aria-label="annotated-image-viewer">
      {/* Toolbar */}
      <div className="annotated-viewer__toolbar">
        <div className="annotated-viewer__tools">
          {(["rectangle", "circle", "arrow", "freehand", "text", "select"] as AnnotationTool[]).map((t) => (
            <Button
              key={t}
              type="button"
              variant={tool === t ? "primary" : "ghost"}
              size="sm"
              onClick={() => setTool(t)}
              aria-pressed={tool === t}
              aria-label={`${t} tool`}
              title={t.charAt(0).toUpperCase() + t.slice(1)}
            >
              {t === "rectangle" && "▭"}
              {t === "circle" && "○"}
              {t === "arrow" && "→"}
              {t === "freehand" && "✎"}
              {t === "text" && "T"}
              {t === "select" && "↖"}
            </Button>
          ))}
        </div>

        <div className="annotated-viewer__colors">
          {COLORS.map((c) => (
            <Button
              key={c}
              type="button"
              variant="ghost"
              size="sm"
              style={{ backgroundColor: c }}
              onClick={() => setColor(c)}
              aria-label={`Color ${c}`}
              aria-pressed={color === c}
            />
          ))}
        </div>

        <div className="annotated-viewer__stroke-widths">
          {STROKE_WIDTHS.map((w) => (
            <Button
              key={w}
              type="button"
              variant={strokeWidth === w ? "secondary" : "ghost"}
              size="sm"
              onClick={() => setStrokeWidth(w)}
              aria-label={`Stroke width ${w}`}
              aria-pressed={strokeWidth === w}
            >
              <span style={{ width: w * 3, height: w * 3, borderRadius: "50%", backgroundColor: "currentColor", display: "inline-block" }} />
            </Button>
          ))}
        </div>

        <div className="annotated-viewer__actions">
          <Button type="button" variant="secondary" size="sm" onClick={undo} disabled={historyIndex <= 0}>
            Undo
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={redo} disabled={historyIndex >= history.length - 1}>
            Redo
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={clearAll}>
            Clear
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={handleExport}>
            Export
          </Button>
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={handleSave}
            loading={saveAnnotation.isPending}
            loadingText="Saving…"
          >
            Save
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>

      {/* Canvas area */}
      <div className="annotated-viewer__canvas-container">
        {downloadQuery.isLoading && <p>Loading image…</p>}
        {downloadQuery.isError && <p className="text-red-600">Failed to load image</p>}
        {error && <p className="text-red-600" role="alert">{error}</p>}
        {saveAnnotation.isSuccess && <p className="text-green-600">Annotation saved</p>}

        <canvas
          ref={canvasRef}
          className="annotated-viewer__canvas"
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          style={{ cursor: tool === "select" ? "default" : "crosshair" }}
        />
      </div>

      {/* Text input dialog */}
      {showTextDialog && (
        <div className="annotated-viewer__text-dialog">
          <TextInput
            type="text"
            value={textInput}
            onChange={(e) => setTextInput(e.target.value)}
            placeholder="Enter annotation text…"
            autoFocus
            onKeyDown={(e) => {
              if (e.key === "Enter") handleTextSubmit();
              if (e.key === "Escape") setShowTextDialog(false);
            }}
          />
          <Button type="button" variant="primary" size="sm" onClick={handleTextSubmit}>
            Add
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={() => setShowTextDialog(false)}>
            Cancel
          </Button>
        </div>
      )}
    </div>
  );
}