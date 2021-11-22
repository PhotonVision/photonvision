// eslint-disable-next-line no-unused-vars
import Vue from 'vue'

export default {
    state: {
        done: [],
        undone: [],
        newMutation: true
    },
    mutations: {
        updatePipeline: (state, val) => {
            state.done.push(val)
            if (state.newMutation) {
                state.undone = []
            }
        },
        addUndone: (state, val) => {
            state.undone.push(val);
        },
        removeLastDone: state => {
            state.done.pop()
        },
        removeLastUnDone: state => {
            state.undone.pop()
        },
        updateStatus: (state, bool) => {
            state.newMutation = bool;
        },
    },
    actions: {
        undo: (context, {vm}) => {
            let commit = context.getters.lastDone;
            context.commit('removeLastDone')
            context.commit('updateStatus', false)
            for (let key in commit) {
                if (commit.hasOwnProperty(key)) {
                    context.commit('addUndone', {[key]: context.getters["pipeline"][key]});
                    context.commit('mutatePipeline', {'key': key, 'value': commit[key]});
                    vm.handleInput(key, commit[key]);
                }
            }
            context.commit('updateStatus', true)
        },
        redo: (context, {vm}) => {
            let commit = context.getters.lastUnDone;
            context.commit('removeLastUnDone');
            context.commit('updateStatus', false)
            for (let key in commit) {
                if (commit.hasOwnProperty(key)) {
                    context.commit('mutatePipeline', {'key': key, 'value': commit[key]});
                    vm.handleInput(key, commit[key]);
                }
            }
            context.commit('updateStatus', true)
        }
    },
    getters: {
        lastDone: state => {
            return state.done[state.done.length - 1]
        },
        lastUnDone: state => {
            return state.undone[state.undone.length - 1]
        },
        canUndo: state => {
            return state.done.length
        },
        canRedo: state => {
            return state.undone.length
        }
    }

};
