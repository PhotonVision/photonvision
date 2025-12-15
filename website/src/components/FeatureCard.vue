<script setup lang="ts">
defineProps<{
  icon: string;
  title: string;
  description: string;
  reverse?: boolean;
}>();
</script>

<template>
  <section
    :class="[
      'relative rounded-xl bg-zinc-800/50 border border-zinc-700/50 flex flex-col',
      reverse ? 'p-8' : 'p-6',
    ]"
  >
    <div
      :class="[
        'flex items-center justify-center rounded-xl bg-linear-to-br from-brand-blue to-primary shadow-lg',
        'size-12 rounded-2xl mb-4',
      ]"
    >
      <i :class="[icon, reverse ? 'text-2xl' : '']"></i>
    </div>
    <h3
      :class="[
        'font-semibold text-brand-yellow',
        reverse ? 'text-xl mb-3' : 'text-xl mb-2',
      ]"
    >
      {{ title }}
    </h3>
    <p class="text-zinc-300">{{ description }}</p>
  </section>
</template>
