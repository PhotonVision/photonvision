<template>
    <div id="vision">
        <nav class="nav">
            <router-link to="/input"><a>Input</a></router-link>
        </nav>
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: 'Vision'
    }
</script>