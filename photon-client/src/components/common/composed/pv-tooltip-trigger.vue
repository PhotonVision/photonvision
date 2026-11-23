<script setup lang="ts">
import { Primitive } from "reka-ui";
import { injectTooltipRootContext } from "reka-ui";
import { onMounted, ref, useId, useTemplateRef } from "vue";

withDefaults(
  defineProps<{
    asChild?: boolean;
    as?: string;
  }>(),
  {
    as: "button"
  }
);

const rootContext = injectTooltipRootContext();
const triggerElement = useTemplateRef<HTMLDivElement | null>("triggerElement");
const contentId = useId();
const hasPointerMoveOpened = ref(false);

onMounted(() => {
  rootContext.contentId ||= contentId;
  rootContext.onTriggerChange(triggerElement.value as HTMLElement | undefined);
});

function handlePointerMove(event: PointerEvent) {
  // Tooltips should only open on hover, never on keyboard focus.
  if (rootContext.disabled.value || event.pointerType === "touch") return;
  if (!hasPointerMoveOpened.value) {
    rootContext.onTriggerEnter();
    hasPointerMoveOpened.value = true;
  }
}

function handlePointerLeave() {
  if (rootContext.disabled.value) return;
  rootContext.onTriggerLeave();
  hasPointerMoveOpened.value = false;
}
</script>

<template>
  <primitive
    ref="triggerElement"
    :as="as"
    :as-child="asChild"
    :aria-describedby="rootContext.open ? rootContext.contentId : undefined"
    :data-state="rootContext.stateAttribute"
    @pointermove="handlePointerMove"
    @pointerleave="handlePointerLeave"
  >
    <slot />
  </primitive>
</template>
