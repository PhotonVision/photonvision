import { onBeforeUnmount, onMounted, ref, type Ref } from "vue";
import { themeDefaults, type ThemeBase } from "@/lib/ThemeManager";

export type ThemeColors = ThemeBase & {
  onBackground?: string;
  onSurface?: string;
  accent?: string;
  error?: string;
  info?: string;
  success?: string;
  warning?: string;
  buttonActive?: string;
  buttonPassive?: string;
  logsBackground?: string;
  sidebar?: string;
};

export type ThemeState = {
  isDark: Ref<boolean>;
  colors: Ref<ThemeColors>;
  refreshTheme: () => void;
};

const themeState: ThemeState = {
  isDark: ref(false),
  colors: ref({ ...themeDefaults.LightTheme }),
  refreshTheme: () => undefined
};

let listenersAttached = false;
let observer: MutationObserver | null = null;
let mediaQuery: MediaQueryList | null = null;
let subscriberCount = 0;

const readCssVar = <T extends string | undefined>(name: string, fallback: T): string | T => {
  if (typeof window === "undefined") return fallback;
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
};

const readIsDark = () => {
  if (typeof window === "undefined") return false;
  const datasetTheme = document.documentElement.dataset.theme;
  if (datasetTheme === "dark") return true;
  if (datasetTheme === "light") return false;
  const storedTheme = localStorage.getItem("theme");
  if (storedTheme === "DarkTheme") return true;
  if (storedTheme === "LightTheme") return false;
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ?? false;
};

const refreshTheme = () => {
  const isDark = readIsDark();
  const fallback: ThemeColors = isDark ? themeDefaults.DarkTheme : themeDefaults.LightTheme;

  themeState.isDark.value = isDark;
  themeState.colors.value = {
    background: readCssVar("--pv-background", fallback.background),
    onBackground: readCssVar("--pv-on-background", fallback.onBackground),
    surface: readCssVar("--pv-surface", fallback.surface),
    onSurface: readCssVar("--pv-on-surface", fallback.onSurface),
    primary: readCssVar("--pv-primary", fallback.primary),
    secondary: readCssVar("--pv-secondary", fallback.secondary),
    accent: readCssVar("--pv-accent", fallback.accent),
    error: readCssVar("--pv-error", fallback.error),
    info: readCssVar("--pv-info", fallback.info),
    success: readCssVar("--pv-success", fallback.success),
    warning: readCssVar("--pv-warning", fallback.warning),
    buttonActive: readCssVar("--pv-button-active", fallback.buttonActive),
    buttonPassive: readCssVar("--pv-button-passive", fallback.buttonPassive),
    logsBackground: readCssVar("--pv-logs-background", fallback.logsBackground),
    sidebar: readCssVar("--pv-sidebar", fallback.sidebar)
  };
};

themeState.refreshTheme = refreshTheme;

const handleThemeChange = () => {
  refreshTheme();
};

const attachListeners = () => {
  if (listenersAttached || typeof window === "undefined") return;
  listenersAttached = true;

  mediaQuery = window.matchMedia?.("(prefers-color-scheme: dark)") ?? null;
  mediaQuery?.addEventListener("change", handleThemeChange);
  window.addEventListener("storage", handleThemeChange);

  observer = new MutationObserver(handleThemeChange);
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["class", "style", "data-theme"]
  });

  refreshTheme();
};

const detachListeners = () => {
  if (!listenersAttached || typeof window === "undefined") return;
  listenersAttached = false;

  mediaQuery?.removeEventListener("change", handleThemeChange);
  window.removeEventListener("storage", handleThemeChange);
  observer?.disconnect();

  mediaQuery = null;
  observer = null;
};

export const useTheme = (): ThemeState => {
  onMounted(() => {
    subscriberCount += 1;
    attachListeners();
  });

  onBeforeUnmount(() => {
    subscriberCount = Math.max(0, subscriberCount - 1);
    if (subscriberCount === 0) detachListeners();
  });

  return themeState;
};
