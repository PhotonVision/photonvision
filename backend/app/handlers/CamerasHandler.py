import cscore
import cv2


class CamerasHandler:

    @staticmethod
    def get_cameras():
        arr = []

        usb_devices = cscore.UsbCamera.enumerateUsbCameras()

        for index in range(len(usb_devices)):
            cap = cv2.VideoCapture(index)
            if cap.isOpened():
                arr.append(index)
            cap.release()
            index += 1
        return [usb_devices[i] for i in arr]

    @staticmethod
    def start_cameras(usb_devices):
        cameras = []
        for device in usb_devices:
            camera = cscore.UsbCamera(name='', dev=device.dev)
            camera.setPixelFormat(pixelFormat=camera.enumerateVideoModes()[0].pixelFormat) #TODO if dictionary is empy do this else take from dictionary
            cameras.append(camera)
        return cameras
