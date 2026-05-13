import { breakpointsVuetifyV3, useBreakpoints } from "@vueuse/core";
export const useCustomBreakpoints = () => {
  return useBreakpoints({ ...breakpointsVuetifyV3, md: 1460, lg: 2000 });
}