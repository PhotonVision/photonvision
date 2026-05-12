<template>
  <div class="not-found-container">
    <div>
      <div class="pb-9 text-[42px] font-semibold text-white">
        The page you are looking for isn't here
      </div>
      <div class="text-[18px] text-white">
        Please use the sidebar to find what you are looking for
      </div>
    </div>
    <img src="@/assets/images/notfound.webp" alt="missing-page" style="margin-top: 64px" />
  </div>
</template>

<style>
.not-found-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>
