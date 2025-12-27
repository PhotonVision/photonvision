<script setup lang="ts">
defineProps<{
  href: string;
  variant?: "primary" | "secondary" | "outline";
  size?: "sm" | "md" | "lg";
}>();

const variantClasses = {
  primary:
    "bg-primary shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40 hover:scale-105",
  secondary: "bg-brand-blue hover:bg-brand-blue/80 shadow-lg",
  outline: "bg-zinc-800 hover:bg-zinc-700 shadow-lg border border-zinc-600",
};

const sizeClasses = {
  sm: "py-2 px-4 text-sm",
  md: "py-3 px-6",
  lg: "py-4 px-10 text-lg",
};
</script>

<template>
  <a
    :href="href"
    :class="[
      'rounded-lg font-medium text-nowrap transition-all duration-300 inline-block text-center',
      variantClasses[variant ?? 'primary'],
      sizeClasses[size ?? 'md'],
    ]"
  >
    <slot />
  </a>
</template>
