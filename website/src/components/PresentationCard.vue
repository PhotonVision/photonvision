<script setup lang="ts">
import Button from "./Button.vue";

defineProps<{
  title: string;
  description: string;
  slidedeckLink: string;
  codeLink?: string;
  videoEmbedUrl?: string;
}>();
</script>

<template>
  <section
    id="champs"
    class="flex flex-col lg:flex-row items-center justify-center relative py-16 px-8 md:px-16 lg:px-28 gap-12 bg-primary"
  >
    <header
      class="flex flex-col gap-6 justify-center items-center lg:items-start text-center lg:text-left"
    >
      <h2 class="text-4xl font-bold font-heading">{{ title }}</h2>
      <p class="text-zinc-200 text-lg max-w-md">
        {{ description }}
      </p>
      <div class="flex gap-4">
        <Button :href="slidedeckLink" variant="secondary"> Slide Deck </Button>
        <Button v-if="codeLink" :href="codeLink" variant="outline">
          View Code
        </Button>
      </div>
    </header>
    <div class="flex-1 max-w-3xl w-full">
      <div
        class="relative rounded-xl overflow-hidden shadow-2xl border border-white/10"
      >
        <iframe
          v-if="videoEmbedUrl"
          :src="videoEmbedUrl"
          title="YouTube video player"
          frameborder="0"
          allow="
            accelerometer;
            autoplay;
            clipboard-write;
            encrypted-media;
            gyroscope;
            picture-in-picture;
            web-share;
          "
          referrerpolicy="strict-origin-when-cross-origin"
          allowfullscreen
          class="w-full aspect-video"
        ></iframe>
        <div
          v-else
          class="w-full aspect-video bg-zinc-800/50 flex items-center justify-center"
        >
          <p class="text-2xl font-semibold text-zinc-400">Coming Soon!</p>
        </div>
      </div>
    </div>
  </section>
</template>
