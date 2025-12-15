<script setup lang="ts">
import FeatureCard from "./FeatureCard.vue";

withDefaults(
  defineProps<{
    id: string;
    label?: string;
    title?: string;
    description: string;
    features: {
      icon: string;
      title: string;
      description: string;
    }[];
    columns?: 2 | 3;
    reverseCards?: boolean;
    showScrollIndicator?: boolean;
    scrollTarget?: string;
  }>(),
  {
    columns: 2,
    reverseCards: false,
    showScrollIndicator: false,
  },
);
</script>

<template>
  <section
    :id="id"
    class="min-h-[calc(100vh_-_56px)] flex flex-col gap-8 items-center justify-center px-4 md:px-14 lg:px-28 py-20 relative overflow-hidden"
  >
    <header
      class="flex flex-col gap-6 justify-center items-center text-center max-w-4xl relative z-10"
    >
      <span
        v-if="label"
        class="text-brand-blue font-semibold tracking-wider uppercase text-sm"
      >
        {{ label }}
      </span>
      <h2 class="text-4xl md:text-5xl font-bold font-heading">
        <slot name="title">
          {{ title }}
        </slot>
      </h2>
      <p class="text-xl md:text-2xl text-zinc-300 leading-relaxed">
        {{ description }}
      </p>
      <div class="w-24 h-1 bg-brand-blue rounded-full"></div>
    </header>

    <div :class="label ? 'relative z-10' : 'mb-8 lg:mb-0'">
      <div
        class="grid max-w-5xl grid-cols-1 gap-6"
        :class="columns === 3 ? 'lg:grid-cols-3' : 'lg:grid-cols-2'"
      >
        <FeatureCard
          v-for="feature in features"
          :key="feature.title"
          :icon="feature.icon"
          :title="feature.title"
          :description="feature.description"
          :reverse="reverseCards"
        />
      </div>
    </div>

    <a
      v-if="showScrollIndicator && scrollTarget"
      :href="scrollTarget"
      class="absolute bottom-8 p-4 text-brand-yellow/70 hover:text-brand-yellow hover:translate-y-1 transition-all duration-300"
    >
      <i class="fa-solid fa-chevron-down text-2xl"></i>
    </a>
  </section>
</template>
