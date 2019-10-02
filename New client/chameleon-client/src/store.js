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
      Exposure:0,
      Brightness:0,
      Orientation:0,
      Hue:[0,15],
      Saturation:[0,15],
      Value:[0,25],
      Erode:false,
      Dilate:false,
      Area:[0,12],
      Ratio:[0,12],
      Extent:[0,12],
      TargetGrouping:0,
      TargetIntersection:0,
      SortMode:0
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
