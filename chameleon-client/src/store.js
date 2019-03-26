import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex);

const set = key => (state,val) =>{
    state[key] = val
}
export const store = new Vuex.Store({

    state:{
        exposure:0,
        brightness:0,
        orientation:0,
        resolution:0 
    },
    mutations:{
        brightness: set('brightness'),
        exposure: set('exposure'),
        orientation:set('orientation'),
        resolution: set('resolution')
    },
    getters:{
        brightness: state => state.brightness,
        exposure: state => state.exposure,
        orientation: state => state.orientation,
        resolution: state => state.resolution
    },
});