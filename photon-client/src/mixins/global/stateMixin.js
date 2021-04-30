export const stateMixin = {
    methods: {
        currentPipelineType() {
            return this.$store.getters.pipelineType
        },
        currentPipelineSettings() {
            return this.$store.getters.currentPipelineSettings
        },
    }
};
