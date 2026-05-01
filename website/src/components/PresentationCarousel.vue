<script setup lang="ts">
import { ref, computed } from "vue";
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
  currentSlide.value = (currentSlide.value - 1 + props.slides.length) % props.slides.length;
};

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % props.slides.length;
};

const goToSlide = (index: number) => {
  currentSlide.value = index;
};
</script>

<template>
  <section class="relative py-16 px-8 md:px-16 lg:px-28 bg-zinc-900">
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

      <!-- Navigation buttons -->
      <div v-if="props.slides.length > 1" class="flex items-center justify-center gap-4 mt-8">
        <button
          @click="previousSlide"
          class="p-3 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors text-white"
          aria-label="Previous slide"
        >
          <i class="fa-solid fa-chevron-left text-xl"></i>
        </button>

        <!-- Slide indicators -->
        <div class="flex gap-2">
          <button
            v-for="(_, index) in props.slides"
            :key="index"
            @click="goToSlide(index)"
            :class="[
              'w-3 h-3 rounded-full transition-colors',
              index === currentSlide
                ? 'bg-brand-yellow'
                : 'bg-zinc-600 hover:bg-zinc-500',
            ]"
            :aria-label="`Go to slide ${index + 1}`"
          ></button>
        </div>

        <button
          @click="nextSlide"
          class="p-3 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors text-white"
          aria-label="Next slide"
        >
          <i class="fa-solid fa-chevron-right text-xl"></i>
        </button>
      </div>
    </div>
  </section>
</template>
