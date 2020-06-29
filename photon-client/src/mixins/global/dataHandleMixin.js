export const dataHandleMixin = {
    methods: {
        handleInput(key, value) {
            let msg = this.$msgPack.encode({[key]: value});
            this.$socket.send(msg);
        },
        handleData(val) {
            this.handleInput(val, this.value[val]);
            this.$emit('update')
        },
        handlePipelineData(val) {
            let msg = this.$msgPack.encode({
                ["changePipelineSetting"]: {
                    [val]: this.value[val],
                    ["cameraIndex"]: this.$store.getters.currentCameraIndex
                }
            });
            this.$socket.send(msg);
            this.$emit('update')
        },
        rollback(val, e) {
            //TODO UPDATE VALUES INTO WEBSOCKET
            this.$store.commit('updatePipeline', {[val]: e})
        }
    }
};
