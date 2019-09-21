<template>
    <div>
        <v-row >
            <v-col cols="6" class="colsClass">
                <v-tabs background-color="#212121" dark height="50" fixed-tabs slider-color="#4baf62">
                    <v-tab to="">General</v-tab>
                    <v-tab to="">Cameras</v-tab>
                </v-tabs>
            </v-col>
      </v-row>
    </div>
</template>

<script>
    export default {
        name: 'SettingsTab',
        data() {
            return {
                
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>