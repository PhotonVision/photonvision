/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.frame.provider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
* A {@link FrameProvider} that will read and provide an image from a {@link java.nio.file.Path
* path}.
*/
public class FileFrameProvider implements FrameProvider {
    private static int count = 0;

    private Frame m_frame;
    private final Path m_path;

    private final double m_fov;

    private boolean m_reloadImage;

    /**
    * Instantiates a new FileFrameProvider.
    *
    * @param path The path of the image to read from.
    * @param fov The fov of the image.
    */
    public FileFrameProvider(Path path, double fov) {
        if (!Files.exists(path))
            throw new RuntimeException("Invalid path for image: " + path.toAbsolutePath().toString());
        m_path = path;
        m_fov = fov;

        loadImage();
    }

    /**
    * Instantiates a new File frame provider.
    *
    * @param pathAsString The path of the image to read from as a string.
    * @param fov The fov of the image.
    */
    public FileFrameProvider(String pathAsString, double fov) {
        this(Paths.get(pathAsString), fov);
    }

    private void loadImage() {
        Mat image = Imgcodecs.imread(m_path.toString());

        if (image.cols() > 0 && image.rows() > 0) {
            FrameStaticProperties m_properties =
                    new FrameStaticProperties(image.width(), image.height(), m_fov);
            m_frame = new Frame(new CVMat(image), m_properties);
        } else {
            throw new RuntimeException("Image loading failed!");
        }
    }

    /**
    * Set image reloading. If true this will reload the image from the path set in the constructor
    * every time {@link FileFrameProvider#get()} is called.
    *
    * @param reloadImage True to enable image reloading.
    */
    public void setImageReloading(boolean reloadImage) {
        m_reloadImage = reloadImage;
    }

    /**
    * Returns if image reloading is enabled.
    *
    * @return True if image reloading is enabled.
    */
    public boolean isImageReloading() {
        return m_reloadImage;
    }

    @Override
    public Frame get() {
        if (m_reloadImage) {
            if (m_frame != null) m_frame.release();
            m_frame = null;
            loadImage();
        }

        return m_frame;
    }

    @Override
    public String getName() {
        return "FileFrameProvider" + count++ + " - " + m_path.getFileName();
    }
}
