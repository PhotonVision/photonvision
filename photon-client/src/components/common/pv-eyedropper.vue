<script setup lang="ts">
import type { RGBColor } from "@/types/Components";
import { onDeactivated } from "vue";

// TODO:
// make viewfinder update with img frame changes
// magnify css effect
// mobile support
// grey out everything but the img frame to make it more obvious to look there

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    imgId: string;
  }>(),
  {
    disabled: false
  }
);

const emit = defineEmits<{
  "update:opened": [];
  "update:canceled": [];
  "update:colorSelected": [color: RGBColor];
}>();

const colorPickerOpen = defineModel<boolean>();

const getHoverPixel = (globalX: number, globalY: number): RGBColor | undefined => {
  const targetImg = document.getElementById(props.imgId) as HTMLImageElement | null;
  if (targetImg === null) {
    throw new Error(`Pickable Image ${props.imgId} not Found in Document`);
  }

  if (!document.elementsFromPoint(globalX, globalY).includes(targetImg)) {
    return undefined;
  }

  const utilCanvas = document.createElement("canvas");
  utilCanvas.width = targetImg.clientWidth;
  utilCanvas.height = targetImg.clientHeight;

  const context = utilCanvas.getContext("2d") as CanvasRenderingContext2D | null;
  if (context === null) return undefined;
  context.drawImage(targetImg, 0, 0, targetImg.clientWidth, targetImg.clientHeight);

  // Get the (x, y) position of the click with (0, 0) in the top left corner
  const rect = targetImg.getBoundingClientRect();
  const relativeX = Math.round(((globalX - rect.left) / rect.width) * targetImg.clientWidth);
  const relativeY = Math.round(((globalY - rect.top) / rect.height) * targetImg.clientHeight);

  const imageColorData = context.getImageData(relativeX, relativeY, 1, 1).data;

  return { r: imageColorData[0], g: imageColorData[1], b: imageColorData[2] };
};
const handleColorPickerMove = (event: MouseEvent | TouchEvent) => {
  const colorPickerEl = document.getElementById("color-picker");
  if (colorPickerEl === null) return;
  const colorPixelEl = colorPickerEl.getElementsByClassName("color-picker__pixel").item(0);
  if (colorPixelEl === null) return;

  let x = 0;
  let y = 0;

  if (event instanceof MouseEvent) {
    x = event.clientX;
    y = event.clientY;
  } else if (event instanceof TouchEvent) {
    const touch = event.touches[0];
    x = touch.clientX;
    y = touch.clientY;
  }

  let hoverColor = getHoverPixel(x, y);

  if (hoverColor === undefined) {
    hoverColor = { r: 128, g: 128, b: 128 };
    colorPickerEl.classList.add("disabled");
  } else {
    colorPickerEl.classList.remove("disabled");
  }

  const hoverColorString = `rgb(${hoverColor.r}, ${hoverColor.g}, ${hoverColor.b})`;
  colorPickerEl.style.borderColor = hoverColorString;
  (colorPixelEl as HTMLElement).style.backgroundColor = hoverColorString;

  const w = colorPickerEl.offsetWidth / 2;
  const h = colorPickerEl.offsetHeight / 2;

  colorPickerEl.style.left = x - w + "px";
  colorPickerEl.style.top = y - h + "px";
};
const handleColorPickerDown = (event: MouseEvent | TouchEvent) => {
  let x = 0;
  let y = 0;

  if (event instanceof MouseEvent) {
    x = event.clientX;
    y = event.clientY;
  } else if (event instanceof TouchEvent) {
    const touch = event.touches[0];
    x = touch.clientX;
    y = touch.clientY;
  }

  const hoverColor = getHoverPixel(x, y);

  if (hoverColor === undefined) {
    return;
  }

  // // Convert RGB to HSV
  // // Normalize RGB ranges
  // const r = hoverColor.r / 255;
  // const g = hoverColor.g / 255;
  // const b = hoverColor.b / 255;
  //
  // const minRGB = Math.min(r, Math.min(g, b));
  // const maxRGB = Math.max(r, Math.max(g, b));
  // const d = r === minRGB ? g - b : b === minRGB ? r - g : b - r;
  // const h = r === minRGB ? 3 : b === minRGB ? 1 : 5;
  // let H = 30 * (h - d / (maxRGB - minRGB));
  // let S = (255 * (maxRGB - minRGB)) / maxRGB;
  // let V = 255 * maxRGB;
  // if (isNaN(H)) H = 0;
  // if (isNaN(S)) S = 0;
  // if (isNaN(V)) V = 0;

  emit("update:colorSelected", hoverColor);

  handleColorPickerClose();
};

const handleColorPickerOpen = () => {
  colorPickerOpen.value = true;
  emit("update:opened");

  document.addEventListener("keyup", keypressCancelWrapper);

  const colorPicker = document.createElement("div");
  colorPicker.classList.add("color-picker__viewfinder");
  colorPicker.id = "color-picker";

  const colorPickerPixelView = document.createElement("div");
  colorPickerPixelView.classList.add("color-picker__pixel");
  colorPicker.appendChild(colorPickerPixelView);

  document.body.appendChild(colorPicker);

  document.addEventListener("mousemove", handleColorPickerMove);
  document.addEventListener("touchmove", handleColorPickerMove);
  document.addEventListener("mousedown", handleColorPickerDown);
  document.addEventListener("touchend", handleColorPickerDown);
};
const handleColorPickerClose = () => {
  colorPickerOpen.value = false;

  document.removeEventListener("keyup", keypressCancelWrapper);

  const colorPicker = document.getElementById("color-picker");
  if (colorPicker) {
    document.body.removeChild(colorPicker);
  }

  document.removeEventListener("mousemove", handleColorPickerMove);
  document.removeEventListener("touchmove", handleColorPickerMove);
  document.removeEventListener("mousedown", handleColorPickerDown);
  document.removeEventListener("touchend", handleColorPickerDown);
};

const handleColorPickerCancel = () => {
  emit("update:canceled");

  handleColorPickerClose();
};
const keypressCancelWrapper = (event: KeyboardEvent) => {
  if (event.key !== "Escape") return;

  handleColorPickerCancel();
};

onDeactivated(() => {
  handleColorPickerCancel();
});
</script>

<template>
  <div>
    <v-btn :disabled="disabled" icon="mdi-eyedropper" variant="plain" @click="handleColorPickerOpen" />
  </div>
</template>

<style>
.color-picker__viewfinder {
  position: absolute;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100000;
  cursor: none !important;
  border: 6px solid;
}

.color-picker__viewfinder .color-picker__pixel {
  width: 12px;
  height: 12px;
  border: 1px solid black;
}

.color-picker__viewfinder.disabled {
  opacity: 0.6;
}
</style>
