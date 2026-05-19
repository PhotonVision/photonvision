import { onBeforeUnmount, onMounted, ref, type Ref } from "vue";

export type ThemeColors = {
  background: string;
  onBackground: string;
  surface: string;
  surfaceVariant: string;
  onSurface: string;
  primary: string;
  secondary: string;
  accent: string;
  error: string;
  info: string;
  success: string;
  warning: string;
  buttonActive: string;
  buttonPassive: string;
  logsBackground: string;
  sidebar: string;
};

export type ThemeState = {
  isDark: Ref<boolean>;
  colors: Ref<ThemeColors>;
  refreshTheme: () => void;
};

const defaultDark: ThemeColors = {
  background: "#151515",
  onBackground: "#f0f0f0",
  surface: "#1c232c",
  surfaceVariant: "#485b70",
  onSurface: "#f0f0f0",
  primary: "#39A4D5",
  secondary: "#FFD843",
  accent: "#006492",
  error: "#ff2e2e",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
  buttonActive: "#FFD843",
  buttonPassive: "#39A4D5",
  logsBackground: "#151515",
  sidebar: "#151515"
};

const defaultLight: ThemeColors = {
  background: "#232C37",
  onBackground: "#ffffff",
  surface: "#006492",
  surfaceVariant: "#8f8f8f",
  onSurface: "#f0f0f0",
  primary: "#FFD843",
  secondary: "#39A4D5",
  accent: "#FFD843",
  error: "#b80000",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
  buttonActive: "#FFD843",
  buttonPassive: "#39A4D5",
  logsBackground: "#232C37",
  sidebar: "#006492"
};

const themeState: ThemeState = {
  isDark: ref(false),
  colors: ref({ ...defaultLight }),
  refreshTheme: () => undefined
};

let listenersAttached = false;
let observer: MutationObserver | null = null;
let mediaQuery: MediaQueryList | null = null;
let subscriberCount = 0;

const readCssVar = (name: string, fallback: string) => {
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
  const fallback = isDark ? defaultDark : defaultLight;

  themeState.isDark.value = isDark;
  themeState.colors.value = {
    background: readCssVar("--pv-background", fallback.background),
    onBackground: readCssVar("--pv-on-background", fallback.onBackground),
    surface: readCssVar("--pv-surface", fallback.surface),
    surfaceVariant: readCssVar("--pv-surface-variant", fallback.surfaceVariant),
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
