import { breakpointsTailwind, useBreakpoints } from "@vueuse/core";

export const useCustomBreakpoints = () => {
  return useBreakpoints(breakpointsTailwind);
};
