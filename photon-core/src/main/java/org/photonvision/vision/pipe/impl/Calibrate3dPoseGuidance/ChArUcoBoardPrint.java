/*
 * Copyright (C) Photon Vision.
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

package org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.CharucoBoard;
import org.opencv.objdetect.Dictionary;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class ChArUcoBoardPrint {
    private static final Logger logger = new Logger(ChArUcoBoardPrint.class, LogGroup.General);

    // Charuco Board configuration (duplicates ChArucoDetector)
    private Size board_sz = new Size(Cfg.board_x, Cfg.board_y);
    private final Dictionary dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50);
    private final Size boardImageSize = new Size(Cfg.board_x*Cfg.square_len, Cfg.board_y*Cfg.square_len);
    final Mat boardImage = new Mat();
    private final CharucoBoard board = new CharucoBoard(this.board_sz, Cfg.square_len, Cfg.marker_len, this.dictionary);

    ChArUcoBoardPrint()
    {
        logger.debug("Starting ----------------------------------------");

        /// create board
        this.board.generateImage(this.boardImageSize, this.boardImage);

        // write ChArUco Board to file for print to use for calibration
        
        /* PNG */
        final String boardFilePNG = Cfg.boardFile + ".png";
        try (FileOutputStream outputStreamPNG = new FileOutputStream(new File(boardFilePNG))) {
            logger.info("ChArUcoBoard to be printed is in file " + boardFilePNG);

            byte[] boardByte = new byte[this.boardImage.rows()*this.boardImage.cols()]; // assumes 1 channel Mat [ 1680*2520*CV_8UC1, isCont=true, isSubmat=false, nativeObj=0x294e475cc20, dataAddr=0x294e55f7080 ]

            CRC32 crc32 = new CRC32();

            // SIGNATURE
            final byte[] signaturePNG =
                {
                (byte)0x89, (byte)0x50, (byte)0x4e, (byte)0x47, (byte)0x0d, (byte)0x0a, (byte)0x1a, (byte)0x0a // PNG magic number
                };
            outputStreamPNG.write(signaturePNG);

            // HEADER
            byte[] IHDR =
            {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0d, // length
                (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52, // IHDR
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // data width place holder
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // data height place holder
                (byte)0x08,                                     // bit depth
                (byte)0x00,                                     // color type - grey scale
                (byte)0x00,                                     // compression method
                (byte)0x00,                                     // filter method (default/only one?)
                (byte)0x00,                                     // interlace method
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00  // crc place holder
            };
            // fetch the length data for the IHDR
            int ihdrWidthOffset = 8;
            int ihdrHeightOffset = 12;
            ArrayUtils.intToByteArray(boardImage.cols(), IHDR, ihdrWidthOffset);
            ArrayUtils.intToByteArray(boardImage.rows(), IHDR, ihdrHeightOffset);

            crc32.reset();
            crc32.update(IHDR, 4, IHDR.length-8); // skip the beginning 4 for length and ending 4 for crc
            ArrayUtils.intToByteArray((int)crc32.getValue(), IHDR, IHDR.length-4);
            outputStreamPNG.write(IHDR);

            // PHYSICAL RESOLUTION
            byte[] PHYS = // varies with the requested resolution [pixels per meter]
                {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x09, // length
                (byte)0x70, (byte)0x48, (byte)0x59, (byte)0x73, // pHYs
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // x res [pixels per unit] place holder
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // y res [pixels per unit] place holder
                (byte)0x01,                                     // units [unit is meter]
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00  // crc place holder
                };
            int physXresOffset = 8;
            int physYresOffset = 12;
            ArrayUtils.intToByteArray(Cfg.resXDPM, PHYS, physXresOffset);
            ArrayUtils.intToByteArray(Cfg.resYDPM, PHYS, physYresOffset);

            crc32.reset();
            crc32.update(PHYS, 4, PHYS.length-8); // skip the beginning 4 for length and ending 4 for crc
            ArrayUtils.intToByteArray((int)crc32.getValue(), PHYS, PHYS.length - 4);
            outputStreamPNG.write(PHYS);

            // DATA
            //The complete filtered PNG image is represented by a single zlib datastream that is stored in a number of IDAT chunks.

            // create the filtered, compressed datastream

            boardImage.get(0, 0, boardByte); // board from OpenCV Mat

            // filter type begins each row so step through all the rows adding the filter type to each row
            byte[] boardByteFilter = new byte[boardImage.rows() + boardByte.length];
            int flatIndex = 0;
            int flatIndexFilter = 0;
            for (int row = 0; row < boardImage.rows(); row++)
            {
                boardByteFilter[flatIndexFilter++] = 0x00; // filter type none begins each row          
                for (int col = 0; col < boardImage.cols(); col++)
                {
                    boardByteFilter[flatIndexFilter++] = boardByte[flatIndex++];
                }
            }
            // complete filtered PNG image is represented by a single zlib compression datastream
            byte[] boardCompressed = ArrayUtils.compress(boardByteFilter);

            // chunk the compressed datastream
            // chunking not necessary for the ChArUcoBoard but it's potentially good for other uses
            int chunkSize = 0;
            int chunkSizeMax = 100_000; // arbitrary "small" number
            int dataWritten = 0;

            while (dataWritten < boardCompressed.length) // chunk until done
            {
                chunkSize = Math.min(chunkSizeMax, boardCompressed.length - dataWritten); // max or what's left in the last chunk

                byte[] IDAT = new byte[4 + 4 + chunkSize + 4]; // 4 length + 4 "IDAT" + chunk length + 4 CRC

                ArrayUtils.intToByteArray(chunkSize, IDAT, 0); // stash length of the chunk data in first 4 bytes
                IDAT[4] = (byte)("IDAT".charAt(0));
                IDAT[5] = (byte)("IDAT".charAt(1));
                IDAT[6] = (byte)("IDAT".charAt(2));
                IDAT[7] = (byte)("IDAT".charAt(3));
                for(int i=0; i < chunkSize; i++)
                {
                    IDAT[8 + i] = boardCompressed[dataWritten + i]; // stash data from where we left off to its place in the chunk
                }

                crc32.reset();
                crc32.update(IDAT, 4, IDAT.length - 8); // skip the beginning 4 for length and ending 4 for crc
                ArrayUtils.intToByteArray((int)crc32.getValue(), IDAT, IDAT.length - 4); // crc in last 4 bytes  

                outputStreamPNG.write(IDAT);
                dataWritten += chunkSize;
            }

            // END
            final byte[] IEND =
                {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // length
                (byte)0x49, (byte)0x45, (byte)0x4e, (byte)0x44, // IEND
                (byte)0xae, (byte)0x42, (byte)0x60, (byte)0x82  // crc
                };
            
            outputStreamPNG.write(IEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /// end create board
}
