<template>
    <div>
        <CVrangeSlider v-model="value.hue" name="Hue" :min="0" :max="180" @input="handleData('hue')"/>
        <CVrangeSlider v-model="value.saturation" name="Saturation" :min="0" :max="255"
                       @input="handleData('saturation')"/>
        <CVrangeSlider v-model="value.value" name="Value" :min="0" :max="255" @input="handleData('value')"/>
        <v-divider color="darkgray " style="margin-top: 5px"/>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setFunction(1)" small>
            <v-icon>colorize</v-icon>
            Eye drop
        </v-btn>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setFunction(2)" small>
            <v-icon>add</v-icon>
            Expand Selection
        </v-btn>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setFunction(3)" small>
            <v-icon>remove</v-icon>
            Shrink Selection
        </v-btn>
        <v-divider color="darkgray "/>
        <CVswitch v-model="value.erode" name="Erode" @input="handleData('erode')"/>
        <CVswitch v-model="value.dilate" name="Dilate" @input="handleData('dilate')"/>
    </div>
</template>

<script>
    import CVrangeSlider from '../../components/cv-range-slider'
    import CVswitch from '../../components/cv-switch'

    export default {
        name: 'Threshold',
        props: ['value'],
        components: {
            CVrangeSlider,
            CVswitch
        },
        data() {
            return {
                currentFunction: undefined,
                colorPicker: undefined,
                currentBinaryState: 0
            }
        },
        computed: {
            pipeline: {
                get() {
                    return this.$store.state.pipeline;
                }
            },
            driverState: {
                get() {
                    return this.$store.state.driverMode;
                },
                set(val) {
                    this.$store.commit("driverMode", val);
                }
            }
        },
        methods: {
            onClick(event) {
                if (this.currentFunction !== undefined) {
                    let hsvArray = this.colorPicker.colorPickerClick(event, this.currentFunction,
                        [[this.value.hue[0], this.value.saturation[0], this.value.value[0]], [this.value.hue[1], this.value.saturation[1], this.value.value[1]]]);
                    this.currentFunction = undefined;
                    this.value.hue = [hsvArray[0][0], hsvArray[1][0]];
                    this.value.saturation = [hsvArray[0][1], hsvArray[1][1]];
                    this.value.value = [hsvArray[0][2], hsvArray[1][2]];
                    this.value.isBinary = this.currentBinaryState;
                    let msg = this.$msgPack.encode({
                        'hue': this.value.hue,
                        'saturation': this.value.saturation,
                        'value': this.value.value,
                        'isBinary': this.value.isBinary
                    });
                    this.$socket.send(msg);
                    this.$emit('update');
                }
            },
            setFunction(index) {
                this.currentBinaryState = this.value.isBinary;
                if (this.currentBinaryState === true) {
                    this.value.isBinary = false;
                    this.handleData('isBinary')
                }
                switch (index) {
                    case 0:
                        this.currentFunction = undefined;
                        break;
                    case 1:
                        this.currentFunction = this.colorPicker.eyeDrop;
                        break;
                    case 2:
                        this.currentFunction = this.colorPicker.expand;
                        break;
                    case 3:
                        this.currentFunction = this.colorPicker.shrink;
                        break;
                }
            },
            handleData(val) {
                this.handleInput(val, this.value[val]);
                this.$emit('update')
            },
        },
        mounted: function () {
            const self = this;
            this.colorPicker = require('../../plugins/ColorPicker').default;
            this.$nextTick(() => {
                self.colorPicker.initColorPicker();
            });
        }
    }

</script>

<style lang="" scoped>

</style>