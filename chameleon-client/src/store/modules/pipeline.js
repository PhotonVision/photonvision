export default {
    state: {
        exposure: 0,
        brightness: 0,
        gain: 0,
        rotationMode: 0,
        hue: [0, 15],
        saturation: [0, 15],
        value: [0, 25],
        erode: false,
        dilate: false,
        area: [0, 12],
        ratio: [0, 12],
        extent: [0, 12],
        speckle: 5,
        targetGrouping: 0,
        targetIntersection: 0,
        sortMode: 0,
        multiple: false,
        isBinary: 0,
        calibrationMode: 0,
        videoModeIndex: 0,
        streamDivisor: 0,
        is3D: false,
        targetRegion: 0,
        targetOrientation: 1
    },
    mutations: {
        isBinary: (state, value) => {
            console.log(value)
            state.isBinary = value
        },
        mutatePipeline: (state, {key, value}) => {
            // console.log(`key:${key}, value: ${value}`)
            this.set(state, key, value)
        }

    },
    actions: {},
    getters: {
        pipeline: state => {
            return state
        }
    }
};