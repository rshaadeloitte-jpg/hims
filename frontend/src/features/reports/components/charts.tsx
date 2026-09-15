import { useMemo } from "react";

/**
 * Lightweight Vercel Analytics-style chart primitives.
 *
 * No external chart dependency — pure SVG so the bundle stays small and the
 * rendering is deterministic under jsdom for tests. The visual language
 * mirrors Vercel Analytics: gradient area fills, soft gridlines, tooltip
 * overlay on hover.
 */

export interface SeriesPoint {
  /** X-axis value (usually a date or category label). */
  label: string;
  /** Y-axis value. */
  value: number;
}

interface AreaChartProps {
  data: SeriesPoint[];
  height?: number;
  color?: string;
  gradientFrom?: string;
  gradientTo?: string;
  formatValue?: (n: number) => string;
  "aria-label"?: string;
}

/**
 * AreaChart — single-series area chart with gradient fill + hover tooltip.
 * Mirrors the Vercel Analytics visitor graph.
 */
export function AreaChart({
  data,
  height = 160,
  color = "#1d4ed8",
  gradientFrom = "rgba(29, 78, 216, 0.25)",
  gradientTo = "rgba(29, 78, 216, 0)",
  formatValue = (n) => n.toLocaleString(),
  "aria-label": ariaLabel,
}: AreaChartProps) {
  const width = 640;
  const padding = { top: 12, right: 12, bottom: 24, left: 12 };
  const innerW = width - padding.left - padding.right;
  const innerH = height - padding.top - padding.bottom;

  const { points, pathD, areaD } = useMemo(() => {
    if (data.length === 0) {
      return { points: [], pathD: "", areaD: "" };
    }
    const values = data.map((d) => d.value);
    const max = Math.max(...values, 1);
    const min = Math.min(...values, 0);
    const range = max - min || 1;
    const stepX = data.length > 1 ? innerW / (data.length - 1) : 0;

    const pts = data.map((d, i) => ({
      x: padding.left + i * stepX,
      y: padding.top + innerH - ((d.value - min) / range) * innerH,
      ...d,
    }));

    const line = pts.map((p, i) => `${i === 0 ? "M" : "L"}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ");
    const area = `${line} L${pts[pts.length - 1].x.toFixed(1)},${(padding.top + innerH).toFixed(1)} L${pts[0].x.toFixed(1)},${(padding.top + innerH).toFixed(1)} Z`;

    return { points: pts, pathD: line, areaD: area };
  }, [data, innerW, innerH, padding.left, padding.top]);

  const gradientId = useMemo(() => `area-grad-${Math.random().toString(36).slice(2, 8)}`, []);

  if (data.length === 0) {
    return (
      <div className="chart-empty" role="img" aria-label={ariaLabel}>
        No data
      </div>
    );
  }

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width="100%"
      height={height}
      role="img"
      aria-label={ariaLabel}
      className="chart-svg"
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={gradientFrom} />
          <stop offset="100%" stopColor={gradientTo} />
        </linearGradient>
      </defs>

      {/* Y-axis gridlines (4 steps) */}
      {[0, 0.25, 0.5, 0.75, 1].map((t) => {
        const y = padding.top + innerH * (1 - t);
        return (
          <line
            key={t}
            x1={padding.left}
            y1={y}
            x2={width - padding.right}
            y2={y}
            stroke="var(--color-border)"
            strokeWidth={1}
          />
        );
      })}

      {/* Area fill */}
      <path d={areaD} fill={`url(#${gradientId})`} />

      {/* Line */}
      <path d={pathD} fill="none" stroke={color} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />

      {/* Hover targets + tooltip */}
      {points.map((p, i) => (
        <g key={i} className="chart-point">
          <circle cx={p.x} cy={p.y} r={4} fill={color} className="chart-point__dot" />
          <rect
            x={p.x - 30}
            y={p.y - 36}
            width={60}
            height={28}
            rx={4}
            fill="var(--color-text)"
            className="chart-point__tooltip"
          />
          <text
            x={p.x}
            y={p.y - 18}
            textAnchor="middle"
            fill="var(--color-text-inverse)"
            fontSize={11}
            className="chart-point__tooltip-text"
          >
            {formatValue(p.value)}
          </text>
          <rect
            x={p.x - 20}
            y={padding.top}
            width={40}
            height={innerH}
            fill="transparent"
            className="chart-point__hit"
          />
        </g>
      ))}

      {/* X-axis labels (show first, middle, last) */}
      {points.length > 0 && (
        <>
          <text x={points[0].x} y={height - 6} fontSize={10} fill="var(--color-text-muted)" textAnchor="start">
            {points[0].label}
          </text>
          {points.length > 2 && (
            <text
              x={points[Math.floor(points.length / 2)].x}
              y={height - 6}
              fontSize={10}
              fill="var(--color-text-muted)"
              textAnchor="middle"
            >
              {points[Math.floor(points.length / 2)].label}
            </text>
          )}
          <text x={points[points.length - 1].x} y={height - 6} fontSize={10} fill="var(--color-text-muted)" textAnchor="end">
            {points[points.length - 1].label}
          </text>
        </>
      )}
    </svg>
  );
}

interface BarChartProps {
  data: SeriesPoint[];
  height?: number;
  color?: string;
  formatValue?: (n: number) => string;
  "aria-label"?: string;
}

/**
 * BarChart — vertical bar chart with rounded tops + hover tooltip.
 */
export function BarChart({
  data,
  height = 160,
  color = "#1d4ed8",
  formatValue = (n) => n.toLocaleString(),
  "aria-label": ariaLabel,
}: BarChartProps) {
  const width = 640;
  const padding = { top: 12, right: 12, bottom: 24, left: 12 };
  const innerW = width - padding.left - padding.right;
  const innerH = height - padding.top - padding.bottom;

  const bars = useMemo(() => {
    if (data.length === 0) return [];
    const max = Math.max(...data.map((d) => d.value), 1);
    const barWidth = data.length > 0 ? innerW / data.length - 4 : 0;
    return data.map((d, i) => {
      const bw = Math.max(barWidth, 4);
      const x = padding.left + i * (innerW / data.length) + 2;
      const barH = (d.value / max) * innerH;
      const y = padding.top + innerH - barH;
      return { x, y, width: bw, height: barH, ...d };
    });
  }, [data, innerW, innerH, padding.left, padding.top]);

  if (data.length === 0) {
    return (
      <div className="chart-empty" role="img" aria-label={ariaLabel}>
        No data
      </div>
    );
  }

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width="100%"
      height={height}
      role="img"
      aria-label={ariaLabel}
      className="chart-svg"
    >
      {/* Y-axis gridlines */}
      {[0, 0.25, 0.5, 0.75, 1].map((t) => {
        const y = padding.top + innerH * (1 - t);
        return (
          <line
            key={t}
            x1={padding.left}
            y1={y}
            x2={width - padding.right}
            y2={y}
            stroke="var(--color-border)"
            strokeWidth={1}
          />
        );
      })}

      {bars.map((b, i) => (
        <g key={i} className="chart-bar">
          <rect
            x={b.x}
            y={b.y}
            width={b.width}
            height={b.height}
            rx={3}
            fill={color}
            opacity={0.85}
            className="chart-bar__rect"
          />
          <rect
            x={b.x}
            y={b.y - 32}
            width={b.width}
            height={28}
            rx={4}
            fill="var(--color-text)"
            className="chart-bar__tooltip"
          />
          <text
            x={b.x + b.width / 2}
            y={b.y - 14}
            textAnchor="middle"
            fill="var(--color-text-inverse)"
            fontSize={11}
            className="chart-bar__tooltip-text"
          >
            {formatValue(b.value)}
          </text>
          <rect
            x={b.x}
            y={padding.top}
            width={b.width}
            height={innerH}
            fill="transparent"
            className="chart-bar__hit"
          />
          <text
            x={b.x + b.width / 2}
            y={height - 6}
            fontSize={10}
            fill="var(--color-text-muted)"
            textAnchor="middle"
          >
            {b.label}
          </text>
        </g>
      ))}
    </svg>
  );
}

interface SparklineProps {
  data: SeriesPoint[];
  color?: string;
  height?: number;
  "aria-label"?: string;
}

/**
 * Sparkline — minimal inline chart for count widgets. No axes, no tooltip.
 */
export function Sparkline({ data, color = "#1d4ed8", height = 32, "aria-label": ariaLabel }: SparklineProps) {
  const width = 120;

  const pathD = useMemo(() => {
    if (data.length < 2) return "";
    const max = Math.max(...data.map((d) => d.value), 1);
    const min = Math.min(...data.map((d) => d.value), 0);
    const range = max - min || 1;
    const stepX = width / (data.length - 1);
    return data
      .map((d, i) => {
        const x = i * stepX;
        const y = height - ((d.value - min) / range) * height;
        return `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(" ");
  }, [data, height]);

  if (data.length < 2) return null;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width={width}
      height={height}
      role="img"
      aria-label={ariaLabel}
      className="chart-sparkline"
    >
      <path d={pathD} fill="none" stroke={color} strokeWidth={1.5} strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
