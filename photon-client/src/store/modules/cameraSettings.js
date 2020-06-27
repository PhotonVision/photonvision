export default {
    state: {
        calibration: [],
        fov: 0,
        resolution: 0,
        streamDivisor: 0,
        tilt: 0
    },
    getters: {
        cameraSettings: state => {
            return state
        }
    }
}