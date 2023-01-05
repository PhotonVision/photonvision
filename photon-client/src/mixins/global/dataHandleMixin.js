export const dataHandleMixin = {
    methods: {
        handleInput(key, value) {
            let msg = this.$msgPack.encode({[key]: value});
            this.$store.state.websocket.ws.send(msg);
        },
        handleInputWithIndex(key, value, cameraIndex = this.$store.getters.currentCameraIndex) {
            let msg = this.$msgPack.encode({
                [key]: value,
                ["cameraIndex"]: cameraIndex,
            });
            this.$store.state.websocket.ws.send(msg);
        },
        handleData(val) {
            this.handleInput(val, this[val]);
            this.$emit('update')
        },
        handlePipelineData(val) {
            let msg = this.$msgPack.encode({
                ["changePipelineSetting"]: {
                    [val]: this[val],
                    ["cameraIndex"]: this.$store.getters.currentCameraIndex
                }
            });
            this.$store.state.websocket.ws.send(msg);
            this.$emit('update')
        },
        handlePipelineUpdate(key, val) {
            let msg = this.$msgPack.encode({
                ["changePipelineSetting"]: {
                    [key]: val,
                    ["cameraIndex"]: this.$store.getters.currentCameraIndex
                }
            });
            this.$store.state.websocket.ws.send(msg);
            this.$emit('update')
        },
        handleTruthyPipelineData(val) {
            let msg = this.$msgPack.encode({
                ["changePipelineSetting"]: {
                    [val]: !!(this[val]),
                    ["cameraIndex"]: this.$store.getters.currentCameraIndex
                }
            });
            this.$store.state.websocket.ws.send(msg);
            this.$emit('update')
        },
        rollback(val, e) {
            //TODO UPDATE VALUES INTO WEBSOCKET
            this.$store.commit('updatePipeline', {[val]: e})
        }
    }
};
