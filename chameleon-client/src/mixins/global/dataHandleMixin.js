export const dataHandleMixin = {
    methods:{
        handleInput(key, value) {
            let msg = this.$msgPack.encode({[key]: value});
            this.$socket.send(msg);
        },
        handleData(val) {
            this.handleInput(val, this.value[val]);
            this.$emit('update')
        },

    }
};
