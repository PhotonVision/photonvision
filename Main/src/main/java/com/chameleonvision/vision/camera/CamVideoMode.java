package com.chameleonvision.vision.camera;

import edu.wpi.cscore.VideoMode;

@SuppressWarnings("WeakerAccess")
public class CamVideoMode {
	public final int fps;
	public final int width;
	public final int height;
	public final String pixel_format;

	public CamVideoMode(VideoMode videoMode) {
		fps = videoMode.fps;
		width = videoMode.width;
		height = videoMode.height;
		pixel_format = videoMode.pixelFormat.name();
	}

	public VideoMode.PixelFormat getActualPixelFormat() {
		return VideoMode.PixelFormat.valueOf(pixel_format);
	}

	public boolean isEqualToVideoMode(VideoMode videoMode) {
		return videoMode.fps == fps && videoMode.width == width && videoMode.height == height && videoMode.pixelFormat == getActualPixelFormat();
	}

	public boolean equals(VideoMode vm) {
		return vm.fps == fps &&
				vm.width == width &&
				vm.height == height &&
				vm.pixelFormat == getActualPixelFormat();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof CamVideoMode) {
			var cvm = (CamVideoMode) obj;
			return cvm.fps == fps &&
					cvm.width == width &&
					cvm.height == height &&
					cvm.pixel_format.equals(pixel_format);
		} else if (obj instanceof VideoMode) {
			var vm = (VideoMode) obj;
			return equals(vm);
		} else {
			return false;
		}
	}
}
