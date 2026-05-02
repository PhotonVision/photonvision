<script setup lang="ts">
import { ref } from "vue";
import PresentationCard from "./PresentationCard.vue";

export interface PresentationCardData {
  title: string;
  description: string;
  slidedeckLink: string;
  codeLink?: string;
  videoEmbedUrl?: string;
}

const props = defineProps<{
  slides: PresentationCardData[];
}>();

const currentSlide = ref(0);

const previousSlide = () => {
  if (currentSlide.value > 0) {
    currentSlide.value--;
  } else {
    currentSlide.value = props.slides.length - 1;
  }
};

const nextSlide = () => {
  if (currentSlide.value < props.slides.length - 1) {
    currentSlide.value++;
  } else {
    currentSlide.value = 0;
  }
};
</script>

<template>
  <section class="relative w-full bg-zinc-900">
    <div class="relative">
      <!-- Carousel slides -->
      <div class="overflow-hidden">
        <PresentationCard
          :key="currentSlide"
          :title="props.slides[currentSlide].title"
          :description="props.slides[currentSlide].description"
          :slidedeckLink="props.slides[currentSlide].slidedeckLink"
          :codeLink="props.slides[currentSlide].codeLink"
          :videoEmbedUrl="props.slides[currentSlide].videoEmbedUrl"
        />
      </div>

      <!-- Side navigation arrows -->
      <button
        v-if="props.slides.length > 1"
        @click="previousSlide"
        :class="[
          'absolute left-8 top-1/2 -translate-y-1/2 p-4 transition-all duration-300 text-4xl z-10',
          'text-brand-yellow/70 hover:text-brand-yellow cursor-pointer',
        ]"
        aria-label="Previous slide"
      >
        <i class="fa-solid fa-chevron-left"></i>
      </button>

      <button
        v-if="props.slides.length > 1"
        @click="nextSlide"
        :class="[
          'absolute right-8 top-1/2 -translate-y-1/2 p-4 transition-all duration-300 text-4xl z-10',
          'text-brand-yellow/70 hover:text-brand-yellow cursor-pointer',
        ]"
        aria-label="Next slide"
      >
        <i class="fa-solid fa-chevron-right"></i>
      </button>
    </div>
  </section>
</template>
