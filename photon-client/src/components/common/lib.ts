import { computed, type MaybeRefOrGetter, toValue } from "vue";
import type { Ref } from "vue";

/**
 * Maps a 12-column grid number to a Tailwind width class.
 * Shared across pv-input, pv-number-input, pv-radio, pv-file-input.
 */
export const colWidthClasses: Record<number, string> = {
  1: "w-1/12",
  2: "w-1/6",
  3: "w-1/4",
  4: "w-1/3",
  5: "w-5/12",
  6: "w-1/2",
  7: "w-7/12",
  8: "w-2/3",
  9: "w-3/4",
  10: "w-5/6",
  11: "w-11/12",
  12: "w-full"
};

export const colWidthClass = (cols: number) => colWidthClasses[cols] ?? "flex-1";

/**
 * Compute a percentage-based flex-basis for a 12-col label/content split.
 * Shared across pv-slider, pv-range-slider, pv-select, pv-switch.
 */
export function useColFlexBasis(contentCols: Ref<number> | (() => number)) {
  const getCols = typeof contentCols === "function" ? contentCols : () => contentCols.value;
  const labelWidth = computed(() => `${((12 - getCols()) / 12) * 100}%`);
  const contentWidth = computed(() => `${(getCols() / 12) * 100}%`);
  return { labelWidth, contentWidth };
}

// ─── Shared Tailwind class constants ─────────────────────────────────────────

/** Popover / dropdown / select-content floating surface */
export const popoverSurfaceClass =
  "z-[2500] overflow-hidden rounded-xl border border-white/12 bg-pv-surface text-pv-on-surface shadow-2xl shadow-black/45 ring-1 ring-white/8";

/** Inline number-input box used inside sliders */
export const sliderNumberInputClass =
  "h-10 w-20 shrink-0 rounded-xl border border-white/12 bg-black/15 pl-3 pr-1 text-left text-sm text-pv-on-surface outline-none transition focus:border-pv-primary disabled:cursor-not-allowed disabled:opacity-45";

/** Slider thumb (reka-ui) */
export const sliderThumbClass =
  "block size-5 rounded-full border-2 border-pv-primary bg-white shadow-md outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:pointer-events-none disabled:opacity-50 pv-slider-thumb";

/**
 * Build the wrapper class list for a text-field / file-input field area.
 */
export function fieldWrapperClasses(opts: {
  density?: string;
  disabled: boolean;
  variant: "underlined" | "outline";
  hasError: boolean;
}): string[] {
  const densityMap: Record<string, string> = {
    comfortable: "min-h-10 text-sm",
    default: "min-h-11 text-base",
    compact: "min-h-9 text-sm"
  };

  const base = [
    "flex w-full items-center gap-2",
    densityMap[opts.density ?? "compact"] ?? "min-h-9 text-sm",
    "transition",
    opts.disabled ? "cursor-not-allowed opacity-50" : "",
    opts.variant === "underlined"
      ? "border-b border-white/20 bg-transparent"
      : "rounded-xl border border-white/12 bg-black/15 px-3"
  ];

  if (opts.variant === "underlined") {
    base.push("px-0");
  }

  base.push(opts.hasError ? "border-pv-error/70" : "focus-within:border-pv-primary");

  return base;
}

// ─── Theme color helpers ─────────────────────────────────────────────────────

/**
 * Returns true when the value is already a usable CSS color
 * (hex, rgb(), rgba(), var(), hsl(), oklch(), etc.) rather than a theme token.
 */
export function isRawCssColor(color: string): boolean {
  return /^(#|rgb|rgba|hsl|hsla|oklch|var\()/.test(color);
}

const toThemeVar = (token: string) => {
  const normalized = token.includes("-") ? token : token.replace(/[A-Z]/g, (match) => `-${match.toLowerCase()}`);
  return `var(--color-pv-${normalized})`;
};

const themeTokens = new Set([
  "background",
  "onBackground",
  "surface",
  "surfaceVariant",
  "onSurface",
  "primary",
  "secondary",
  "accent",
  "error",
  "info",
  "success",
  "warning",
  "buttonActive",
  "buttonPassive",
  "logsBackground",
  "sidebar"
]);

const legacyColorMap: Record<string, string> = {
  "light-grey": "#c5c5c5",
  "light-gray": "#c5c5c5"
};

/**
 * Resolve a theme-token string (e.g. "primary", "error") or a raw CSS color
 * into concrete CSS color values at various opacities.
 *
 * Usage:
 *   const { solid, translucent, border } = useThemeColor(() => props.color);
 *
 * Shared across pv-alert, pv-chip, pv-progress and any future component that
 * accepts a `color` prop that can be either a theme token or a raw value.
 */
export function useThemeColor(
  color: MaybeRefOrGetter<string>,
  opts?: { translucentAlpha?: number; borderAlpha?: number }
) {
  const tAlpha = opts?.translucentAlpha ?? 0.65;
  const bAlpha = opts?.borderAlpha ?? 0.45;
  const tPercent = `${Math.round(tAlpha * 100)}%`;
  const bPercent = `${Math.round(bAlpha * 100)}%`;

  const baseColor = computed(() => {
    const c = toValue(color);
    if (isRawCssColor(c)) return c;
    if (legacyColorMap[c]) return legacyColorMap[c];
    if (themeTokens.has(c)) return toThemeVar(c);
    return c;
  });

  const solid = computed(() => {
    return baseColor.value;
  });

  const translucent = computed(() => {
    const c = toValue(color);
    return isRawCssColor(c) ? c : `color-mix(in srgb, ${baseColor.value} ${tPercent}, transparent)`;
  });

  const border = computed(() => {
    const c = toValue(color);
    return isRawCssColor(c) ? c : `color-mix(in srgb, ${baseColor.value} ${bPercent}, transparent)`;
  });

  const isRaw = computed(() => isRawCssColor(toValue(color)));

  /** Common "light tone" tokens whose filled variant needs dark text */
  const isLightTone = computed(() =>
    ["buttonActive", "primary", "secondary", "warning", "success"].includes(toValue(color))
  );

  return { solid, translucent, border, isRaw, isLightTone };
}
