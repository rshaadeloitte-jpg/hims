import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AreaChart, BarChart, Sparkline, type SeriesPoint } from "./charts";

const SAMPLE_DATA: SeriesPoint[] = [
  { label: "Mon", value: 10 },
  { label: "Tue", value: 20 },
  { label: "Wed", value: 15 },
  { label: "Thu", value: 30 },
  { label: "Fri", value: 25 },
];

describe("AreaChart", () => {
  it("renders an SVG with role img", () => {
    render(<AreaChart data={SAMPLE_DATA} aria-label="Test area chart" />);
    const svg = screen.getByRole("img", { name: "Test area chart" });
    expect(svg.tagName.toLowerCase()).toBe("svg");
  });

  it("renders empty state when no data", () => {
    const { container } = render(<AreaChart data={[]} />);
    expect(container.textContent).toContain("No data");
  });

  it("renders correct number of data points", () => {
    const { container } = render(<AreaChart data={SAMPLE_DATA} />);
    const dots = container.querySelectorAll(".chart-point__dot");
    expect(dots.length).toBe(5);
  });
});

describe("BarChart", () => {
  it("renders an SVG with role img", () => {
    render(<BarChart data={SAMPLE_DATA} aria-label="Test bar chart" />);
    const svg = screen.getByRole("img", { name: "Test bar chart" });
    expect(svg.tagName.toLowerCase()).toBe("svg");
  });

  it("renders empty state when no data", () => {
    const { container } = render(<BarChart data={[]} />);
    expect(container.textContent).toContain("No data");
  });

  it("renders correct number of bars", () => {
    const { container } = render(<BarChart data={SAMPLE_DATA} />);
    const rects = container.querySelectorAll(".chart-bar__rect");
    expect(rects.length).toBe(5);
  });
});

describe("Sparkline", () => {
  it("renders an SVG with role img", () => {
    render(<Sparkline data={SAMPLE_DATA} aria-label="Test sparkline" />);
    const svg = screen.getByRole("img", { name: "Test sparkline" });
    expect(svg.tagName.toLowerCase()).toBe("svg");
  });

  it("returns null when fewer than 2 data points", () => {
    const { container } = render(<Sparkline data={[{ label: "Mon", value: 10 }]} />);
    expect(container.innerHTML).toBe("");
  });
});
