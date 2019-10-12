import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const set = key => (state,val) =>{
  state[key] = val
};

export default new Vuex.Store({
  state: {
    settings:{},
    pipeline:{
      exposure:0,
      brightness:0,
      orientation:0,
      hue:[0,15],
      saturation:[0,15],
      value:[0,25],
      erode:false,
      dilate:false,
      area:[0,12],
      ratio:[0,12],
      extent:[0,12],
      targetGrouping:0,
      targetIntersection:0,
      sortMode:0,
      isBinary:0
    },
    cameraSettings:{},
    resolutionList:[],
    port:1181,
    currentCameraIndex:0,
    currentPipelineIndex:0,
    cameraList:[],
    pipelinelist:[],
    point:{}
  },
  mutations: {
    settings: set('settings'),
    pipeline: set('pipeline'),
    cameraSettings: set('cameraSettings'),
    resolutionList: set('resolutionList'),
    port: set('port'),
    currentCameraIndex: set('currentCameraIndex'),
    currentPipelineIndex: set('currentPipelineIndex'),
    cameraList: set('cameraList'),
    pipelinelist: set('cameraList'),
    point:set('point')
  },
  actions: {
    settings: state => state.settings,
    pipeline: state => state.pipeline,
    cameraSettings: state =>state.cameraSettings,
    resolutionList: state =>state.resolutionList,
    port: state =>state.port,
    currentCameraIndex: state =>state.currentCameraIndex,
    currentPipelineIndex: state =>state.currentPipelineIndex,
    cameraList: state =>state.cameraList,
    pipelinelist: state =>state.pipelinelist,
    point: state =>state.point,
  }
})
