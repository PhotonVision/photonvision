import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex);
const set = key => (state,val) =>{
    state[key] = val
};
export const store = new Vuex.Store({

    state:{
        //header
        camera:0,
        pipeline:0,
        //input
        exposure:54,
        brightness:0,
        orientation:0,
        resolution:0,
        //threshold
        hue:[0,10],
        saturation:[0,10],
        value:[0,10],
        erode: false,
        dilate: false,
        //contours
        area:[0,100],
        ratio:[0,0],
        extent:[0,100]

    },
    mutations:{
        camera (state,value){
            state['camera'] = value;
            state['pipeline'] = "0";
        },
        pipeline: set('pipeline'),
        brightness: set('brightness'),
        exposure: set('exposure'),
        orientation:set('orientation'),
        resolution: set('resolution'),
        hue: set('hue'),
        saturation: set('saturation'),
        value: set('value'),
        erode: set('erode'),
        dilate: set('dilate'),
        area: set('area'),
        ratio: set('ratio'),
        extent: set('extent')
    },
    getters:{
        camera: state => state.camera,
        pipeline: state => state.pipeline,
        brightness: state => state.brightness,
        exposure: state => state.exposure,
        orientation: state => state.orientation,
        resolution: state => state.resolution,
        hue: state => state.hue,
        saturation: state => state.saturation,
        value: state => state.value,
        erode: state => state.dilate,
        dilate: state => state.dilate,
        area: state =>state.area,
        ratio: state =>state.ratio,
        extent: state =>state.extent
    },
});