/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunkException;
import brut.util.ExtDataInput;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Res9patchStreamDecoder implements ResStreamDecoder {
    @Override
    public void decode(InputStream in, OutputStream out) throws AndrolibException {
        try {
            byte[] data = IOUtils.toByteArray(in);

            if (data.length == 0) {
                return;
            }
            Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
            int w = bm.getWidth(), h = bm.getHeight();
            Bitmap bm2 = Bitmap.createBitmap(w+2,h+2, Bitmap.Config.ARGB_8888);

            if (bm.getConfig() != Bitmap.Config.ARGB_8888) {
                //TODO: Ensure this is gray + alpha case?
                /*Raster srcRaster = im.getRaster();
                WritableRaster dstRaster = im2.getRaster();
                int[] gray = null, alpha = null;
                for (int y = 0; y < im.getHeight(); y++) {
                    gray = srcRaster.getSamples(0, y, w, 1, 0, gray);
                    alpha = srcRaster.getSamples(0, y, w, 1, 1, alpha);

                    dstRaster.setSamples(1, y + 1, w, 1, 0, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 1, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 2, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 3, alpha);
                }*/
                Log.e(this.getClass().getName(),"Bitmap config not supported! Config: " + bm.getConfig().name());
            } else {
                new Canvas(bm2).drawBitmap(bm,1,1,null);
            }

            NinePatch np = getNinePatch(data);
            drawHLine(bm2, h + 1, np.padLeft + 1, w - np.padRight);
            drawVLine(bm2, w + 1, np.padTop + 1, h - np.padBottom);

            int[] xDivs = np.xDivs;
            if (xDivs.length == 0) {
                drawHLine(bm2, 0, 1, w);
            } else {
                for (int i = 0; i < xDivs.length; i += 2) {
                    drawHLine(bm2, 0, xDivs[i] + 1, xDivs[i + 1]);
                }
            }

            int[] yDivs = np.yDivs;
            if (yDivs.length == 0) {
                drawVLine(bm2, 0, 1, h);
            } else {
                for (int i = 0; i < yDivs.length; i += 2) {
                    drawVLine(bm2, 0, yDivs[i] + 1, yDivs[i + 1]);
                }
            }

            // Some images additionally use Optical Bounds
            // https://developer.android.com/about/versions/android-4.3.html#OpticalBounds
            try {
                OpticalInset oi = getOpticalInset(data);

                for (int i = 0; i < oi.layoutBoundsLeft; i++) {
                    int x = 1 + i;
                    bm2.setPixel(x, h + 1,OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsRight; i++) {
                    int x = w - i;
                    bm2.setPixel(x, h + 1, OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsTop; i++) {
                    int y = 1 + i;
                    bm2.setPixel(w + 1, y, OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsBottom; i++) {
                    int y = h - i;
                    bm2.setPixel(w + 1, y, OI_COLOR);
                }
            } catch (CantFind9PatchChunkException t) {
                // This chunk might not exist
            }
            bm2.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException | NullPointerException ex) {
            // In my case this was triggered because a .png file was
            // containing a html document instead of an image.
            // This could be more verbose and try to MIME ?
            throw new AndrolibException(ex);
        }
    }

    private NinePatch getNinePatch(byte[] data) throws AndrolibException,
        IOException {
        ExtDataInput di = new ExtDataInput(new ByteArrayInputStream(data));
        find9patchChunk(di, NP_CHUNK_TYPE);
        return NinePatch.decode(di);
    }

    private OpticalInset getOpticalInset(byte[] data) throws AndrolibException,
        IOException {
        ExtDataInput di = new ExtDataInput(new ByteArrayInputStream(data));
        find9patchChunk(di, OI_CHUNK_TYPE);
        return OpticalInset.decode(di);
    }

    private void find9patchChunk(DataInput di, int magic) throws AndrolibException,
        IOException {
        di.skipBytes(8);
        while (true) {
            int size;
            try {
                size = di.readInt();
            } catch (IOException ex) {
                throw new CantFind9PatchChunkException("Cant find nine patch chunk", ex);
            }
            if (di.readInt() == magic) {
                return;
            }
            di.skipBytes(size + 4);
        }
    }

    private void drawHLine(Bitmap im, int y, int x1, int x2) {
        for (int x = x1; x <= x2; x++) {
            im.setPixel(x, y, NP_COLOR);
        }
    }

    private void drawVLine(Bitmap im, int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            im.setPixel(x, y, NP_COLOR);

        }
    }

    private static final int NP_CHUNK_TYPE = 0x6e705463; // npTc
    private static final int OI_CHUNK_TYPE = 0x6e704c62; // npLb
    private static final int NP_COLOR = 0xff000000;
    private static final int OI_COLOR = 0xffff0000;

    private static class NinePatch {
        public final int padLeft, padRight, padTop, padBottom;
        public final int[] xDivs, yDivs;

        public NinePatch(int padLeft, int padRight, int padTop, int padBottom,
                         int[] xDivs, int[] yDivs) {
            this.padLeft = padLeft;
            this.padRight = padRight;
            this.padTop = padTop;
            this.padBottom = padBottom;
            this.xDivs = xDivs;
            this.yDivs = yDivs;
        }

        public static NinePatch decode(ExtDataInput di) throws IOException {
            di.skipBytes(1); // wasDeserialized
            byte numXDivs = di.readByte();
            byte numYDivs = di.readByte();
            di.skipBytes(1); // numColors
            di.skipBytes(8); // xDivs/yDivs offset
            int padLeft = di.readInt();
            int padRight = di.readInt();
            int padTop = di.readInt();
            int padBottom = di.readInt();
            di.skipBytes(4); // colorsOffset
            int[] xDivs = di.readIntArray(numXDivs);
            int[] yDivs = di.readIntArray(numYDivs);

            return new NinePatch(padLeft, padRight, padTop, padBottom, xDivs, yDivs);
        }
    }

    private static class OpticalInset {
        public final int layoutBoundsLeft, layoutBoundsTop, layoutBoundsRight, layoutBoundsBottom;

        public OpticalInset(int layoutBoundsLeft, int layoutBoundsTop,
                            int layoutBoundsRight, int layoutBoundsBottom) {
            this.layoutBoundsLeft = layoutBoundsLeft;
            this.layoutBoundsTop = layoutBoundsTop;
            this.layoutBoundsRight = layoutBoundsRight;
            this.layoutBoundsBottom = layoutBoundsBottom;
        }

        public static OpticalInset decode(ExtDataInput di) throws IOException {
            int layoutBoundsLeft = Integer.reverseBytes(di.readInt());
            int layoutBoundsTop = Integer.reverseBytes(di.readInt());
            int layoutBoundsRight = Integer.reverseBytes(di.readInt());
            int layoutBoundsBottom = Integer.reverseBytes(di.readInt());
            return new OpticalInset(layoutBoundsLeft, layoutBoundsTop,
                layoutBoundsRight, layoutBoundsBottom);
        }
    }
}
