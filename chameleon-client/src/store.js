import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex);
const set = key => (state,val) =>{
    state[key] = val
};
export const store = new Vuex.Store({

    state:{
        //header
        curr_camera:"",
        curr_pipeline:"",
        cameraList:[],
        pipelineList:[],
        //input
        exposure:54,
        brightness:0,
        orientation:0,
        resolution:0,
        resolutionList:[],
        FOV:0,
        //threshold
        hue:[0,10],
        saturation:[0,10],
        value:[0,10],
        erode: false,
        dilate: false,
        //contours
        area:[0,100],
        ratio:[0,20],
        extent:[0,100],
        sort_mode:'Largest', 
        target_group:'Single', 
        target_intersection:'Up',
        //Settings
        team_number:0,
        connection_type:"DHCP",
        ip:"",
        gateway:"",
        netmask:"",
        hostname:"",
        //live info
        port:1181,
        is_binary:0,
        //points
        raw_point:[],
        point:{}

    },
    mutations:{
        curr_camera (state,value){
            state['curr_camera'] = value;
            state['pipeline'] = "0";
        },
        curr_pipeline: set('curr_pipeline'),
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
        extent: set('extent'),
        team_number: set('team_number'),
        connection_type: set('connection_type'),
        ip: set('ip'),
        netmask: set('netmask'),
        gateway : set('gateway'),
        hostname : set('hostname'),
        is_binary: set('is_binary'),
        cameraList : set('cameraList'),
        pipelineList: set('piplineList'),
        sort_mode: set('sort_mode'),
        target_group:set('target_group'),
        target_intersection:set('target_intersection'),
        FOV:set('FOV'),
        port:set('port'),
        raw_point:set('raw_point'),
        point:set('point')
    },
    getters:{
        curr_camera: state => state.curr_camera,
        curr_pipeline: state => state.curr_pipeline,
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
        extent: state =>state.extent,
        team_number: state => state.teamValue,
        connection_type: state => state.connectionType,
        ip: state => state.ip,
        netmask: state => state.netmask,
        gateway: state => state.gateway,
        hostname: state => state.hostName,
        is_binary: state => state.is_binary,
        cameraList: state => state.cameraList,
        pipelineList: state => state.pipelineList,
        sort_mode: state => state.sort_mode,
        target_group: state => state.target_group,
        target_intersection: state => state.target_intersection,
        FOV: state => state.FOV,
        port: state => state.port,
        raw_point:state => state.raw_point,
        point: state => state.point

    },
});