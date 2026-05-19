import { breakpointsTailwind, breakpointsVuetifyV3, useBreakpoints } from "@vueuse/core";
export const useCustomBreakpoints = () => {
  return useBreakpoints(breakpointsTailwind);
};
