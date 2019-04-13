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
