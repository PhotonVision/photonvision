<script setup lang="ts">
import Button from "./components/Button.vue";
import GridSection from "./components/GridSection.vue";

const navLinks = [
  {
    href: "https://docs.photonvision.org/en/latest/",
    label: "Documentation",
    icon: "fa-solid fa-book",
  },
  {
    href: "https://discord.gg/wYxTwym",
    label: "Discord",
    icon: "fab fa-discord",
  },
  {
    href: "https://github.com/PhotonVision/photonvision/",
    label: "GitHub",
    icon: "fab fa-github",
  },
  {
    href: "https://www.redbubble.com/people/PhotonVision/shop?asc=u",
    label: "Merch",
    icon: "fa-solid fa-shirt",
  },
];

const features = [
  {
    icon: "fa-solid fa-location-arrow",
    title: "First-Class AprilTag Support",
    description: "FRC Target tracking, out of the box.",
  },
  {
    icon: "fa-solid fa-chess-board",
    title: "Built-In Camera Calibration",
    description:
      "Per-camera intrinsics calibration maximizes accuracy of homography",
  },
  {
    icon: "fa-solid fa-brain",
    title: "Machine Learning",
    description: "Hardware-accelerated inferencing for gamepiece detection",
  },
  {
    icon: "fa-solid fa-eye",
    title: "Driver Mode Integration",
    description: "Use the same camera for driving and robot vision",
  },
  {
    icon: "fa-solid fa-camera",
    title: "Simultaneous Multi-Camera Operation",
    description:
      "PhotonVision can run as many cameras as your hardware can handle",
  },
  {
    icon: "fa-solid fa-layer-group",
    title: "Multi-Tag Pose Estimation",
    description: "Fuse all your available data for peak robot performance",
  },
];

const fossFeatures = [
  {
    icon: "fa-solid fa-check",
    title: "Open Source",
    description:
      "PhotonVision is the largest FOSS FRC Vision project to date, constantly being updated with new features and bug fixes.",
  },
  {
    icon: "fa-solid fa-balance-scale",
    title: "GNU GPL v3",
    description:
      "The GNU GPL v3 license allows you to download, modify and share source code.",
  },
  {
    icon: "fab fa-github",
    title: "We're on GitHub",
    description:
      "We do all of our development openly on GitHub. Transparency is key for every contributor of PhotonVision.",
  },
];

const socialLinks = [
  {
    href: "https://github.com/PhotonVision/photonvision/",
    icon: "fab fa-github",
    label: "GitHub",
  },
  {
    href: "https://discord.gg/wYxTwym",
    icon: "fab fa-discord",
    label: "Discord",
  },
];
</script>

<template>
  <div id="page-wrapper" class="h-screen overflow-y-auto scroll-smooth">
    <header
      id="header"
      class="bg-zinc-900/80 backdrop-blur-md flex items-center justify-between sticky top-0 w-full z-50 border-b border-zinc-700/50 px-4 md:px-8"
    >
      <a href="#" class="flex items-center gap-3 py-2 group">
        <img
          src="/public/images/PhotonVision-Icon-BG.png"
          alt="PhotonVision"
          class="w-10 h-10 group-hover:scale-110 transition-transform duration-300"
        />
        <span class="font-bold text-xl hidden sm:block text-white">
          PhotonVision
        </span>
      </a>
      <div>
        <!-- Mobile menu button -->
        <button
          class="p-3 hover:bg-zinc-700/50 rounded-lg cursor-pointer flex md:!hidden transition-colors anchor/my-anchor"
          popovertarget="mobile-nav"
          aria-label="Open mobile navigation menu"
        >
          <i class="fa-solid fa-bars text-xl"></i>
        </button>

        <div class="relative">
          <div
            popover
            id="mobile-nav"
            class="bg-zinc-900/95 backdrop-blur-lg transition-opacity starting:open:opacity-0 open:opacity-100 opacity-0 duration-300 transition-discrete open:flex open:flex-col md:open:hidden items-center justify-center gap-2 anchored/my-anchor anchored-bottom-span-left overflow-clip p-2 rounded-b-2xl"
          >
            <a
              v-for="link in navLinks"
              :key="link.href"
              :href="link.href"
              class="flex items-center gap-3 px-6 py-4 text-xl text-white hover:text-brand-yellow transition-colors"
            >
              <i :class="link.icon"></i>
              {{ link.label }}
            </a>
            <Button
              href="https://demo.photonvision.org"
              variant="primary"
              size="sm"
            >
              Demo
            </Button>
          </div>
        </div>
      </div>

      <!-- Desktop nav -->
      <nav id="nav" class="hidden md:flex items-center gap-1">
        <a
          v-for="link in navLinks"
          :key="link.href"
          :href="link.href"
          class="flex items-center gap-2 px-4 py-2 rounded-lg hover:bg-zinc-700/50 hover:text-brand-yellow transition-all duration-200"
        >
          <i :class="[link.icon, 'text-sm opacity-70']"></i>
          {{ link.label }}
        </a>
        <Button
          href="https://demo.photonvision.org"
          variant="primary"
          size="sm"
          class="ml-2"
        >
          Demo
        </Button>
      </nav>
    </header>

    <section
      class="flex flex-col min-h-[calc(100vh_-_56px)] md:justify-center items-center p-8 md:p-16 relative overflow-hidden"
    >
      <div
        class="flex justify-center md:justify-start flex-col md:flex-row items-start"
      >
        <span class="self-center md:self-initial mb-8 md:mb-0 md:me-12 shrink">
          <img
            src="/public/images/PhotonVision-Icon-BG.png"
            alt="PhotonVision Logo"
            class="max-w-20 md:max-w-80 drop-shadow-2xl md:p-0 transition-transform duration-500"
          />
        </span>
        <header
          class="flex flex-col gap-6 items-center md:items-start justify-center text-center md:text-start"
        >
          <h2
            class="text-5xl md:text-6xl font-bold drop-shadow-lg font-heading"
          >
            PhotonVision
          </h2>
          <div
            class="text-xl md:text-2xl max-w-128 text-zinc-300 leading-relaxed"
          >
            PhotonVision is the free, fast, and easy-to-use computer vision
            solution for the FIRST® Robotics Competition. Teams can download a
            PhotonVision image for select coprocessors and start tracking
            targets in minutes.
          </div>
          <Button
            href="https://docs.photonvision.org/en/latest/docs/quick-start/quick-install.html"
            variant="primary"
            size="lg"
            class="self-center md:self-start rounded-xl"
          >
            Get Started
          </Button>
        </header>
      </div>
      <a
        href="#demo"
        class="absolute bottom-8 p-4 text-brand-yellow/70 hover:text-brand-yellow hover:translate-y-1 transition-all duration-300 animate-bounce"
      >
        <i class="fa-solid fa-chevron-down text-2xl"></i>
      </a>
    </section>

    <section
      id="demo"
      class="min-h-[calc(100vh_-_56px)] relative justify-center flex items-center py-16 bg-zinc-950"
    >
      <div class="relative mx-4 md:mx-16 lg:mx-32">
        <div
          class="absolute -inset-4 bg-gradient-to-r from-primary via-brand-blue to-brand-yellow rounded-2xl blur-xl opacity-30"
        ></div>
        <img
          src="/public/images/demo.png"
          alt="Demo of PhotonVision UI"
          loading="lazy"
          class="relative rounded-xl shadow-2xl border border-zinc-700/50"
        />
      </div>
      <a
        href="#features"
        class="absolute bottom-8 p-4 text-brand-yellow/70 hover:text-brand-yellow hover:translate-y-1 transition-all duration-300"
      >
        <i class="fa-solid fa-chevron-down text-2xl"></i>
      </a>
    </section>

    <GridSection
      id="features"
      description="PhotonVision is a powerful, open-source vision system for FRC. It's designed to be fast and easy to use, regardless of your team's financial or technical resources."
      :features="features"
      :columns="2"
      show-scroll-indicator
      scroll-target="#video"
    >
      <template #title>
        The Future is in <span class="text-brand-yellow">Sight</span>
      </template>
    </GridSection>

    <section id="video" class="relative">
      <div class="relative">
        <video
          src="/public/videos/in-action.mp4"
          playsinline
          autoplay
          loop
          muted
          class="w-full h-[calc(100vh_-_56px)] object-cover"
        ></video>
        <div
          class="absolute inset-0 bg-gradient-to-t from-zinc-900/60 via-transparent to-zinc-900/30 pointer-events-none"
        ></div>
      </div>
    </section>

    <section
      id="champs"
      class="flex flex-col lg:flex-row items-center justify-center relative py-16 px-8 md:px-16 lg:px-28 gap-12 bg-primary"
    >
      <header
        class="flex flex-col gap-6 justify-center items-center lg:items-start text-center lg:text-left"
      >
        <h2 class="text-4xl font-bold font-heading">Champs 2024 Talk</h2>
        <p class="text-zinc-200 text-lg max-w-md">
          Watch our presentation from the 2024 FIRST Championship and learn how
          to get the most out of PhotonVision.
        </p>
        <div class="flex gap-4">
          <Button
            href="https://docs.google.com/presentation/d/1Gh5InslM5p7aDxjzK8DHoEorpATOl-MQWWixY5GjGgs/edit#slide=id.p"
            variant="secondary"
          >
            Slide Deck
          </Button>
          <Button
            href="https://github.com/PhotonVision/champs_2024"
            variant="outline"
          >
            View Code
          </Button>
        </div>
      </header>
      <div class="flex-1 max-w-3xl w-full">
        <div
          class="relative rounded-xl overflow-hidden shadow-2xl border border-white/10"
        >
          <iframe
            src="https://www.youtube-nocookie.com/embed/iV2v7F_9GwE?si=4wgaT1IrZBpA71dF"
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
        </div>
      </div>
    </section>

    <GridSection
      id="foss"
      description="PhotonVision is an open-source, community based vision system designed for use within the FIRST® Robotics Competition that aims to provide easy and inexpensive vision tracking to teams."
      :features="fossFeatures"
      :columns="3"
      reverse-cards
    >
      <template #title>
        Built by the <span class="text-brand-yellow">Community</span>
      </template>
    </GridSection>

    <footer id="footer" class="py-12 px-8 bg-zinc-800 border-t border-zinc-700">
      <div
        class="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-8"
      >
        <div class="flex items-center gap-4">
          <img
            src="/public/images/PhotonVision-Icon-BG.png"
            class="w-12 h-12 hover:scale-110 transition-transform duration-300"
            alt="PhotonVision logo"
          />
          <span class="font-semibold text-lg">PhotonVision</span>
        </div>
        <div class="flex gap-6">
          <a
            v-for="link in socialLinks"
            :key="link.href"
            :href="link.href"
            class="text-zinc-400 hover:text-brand-yellow transition-colors"
            :aria-label="link.label"
          >
            <i :class="[link.icon, 'text-2xl']"></i>
          </a>
        </div>
        <p class="text-sm text-zinc-400">
          &copy;{{ new Date().getFullYear() }} PhotonVision. All rights
          reserved.
        </p>
      </div>
    </footer>
  </div>
</template>
