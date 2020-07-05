<template>
  <div>
      <p>{{"ReadTheDocs/index.html"}}</p>
  </div>
</template>

<script>
    import axios from 'axios';
    
    export default {
        name: 'DocsTab',
        components: {},
        data() {
            return {
            fileName: "index.html",
            }
        },
    methods: {
        loadFile() {
          axios({
            method: "get",
            url: "ReadTheDocs/" + this.fileName
          })
        }
      }
    }
</script>


<style scoped>
    .videoClass {
        text-align: center;
    }

    .videoClass img {
        padding-top: 10px;
        height: auto !important;
        vertical-align: middle;
    }

    .colsClass {
        padding: 0 !important;
    }
</style>