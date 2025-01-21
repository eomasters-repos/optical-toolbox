package eu.esa.opt.dataio.s3.synergy;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.ceres.multilevel.support.AbstractMultiLevelSource;
import com.bc.ceres.multilevel.support.DefaultMultiLevelImage;
import com.bc.ceres.multilevel.support.DefaultMultiLevelModel;
import com.bc.ceres.multilevel.support.DefaultMultiLevelSource;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

class CameraImageMosaic {

    private static final Interpolation INTERPOLATION = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    //package local for testing
    static RenderedImage create(RenderedImage... sourceImages) {
        double[][] sourceThresholds = new double[sourceImages.length][1];
        sourceThresholds[0][0] = determineSourceThreshold(sourceImages[0]);
        int t = 0;
        for (int i = 1; i < sourceImages.length; i++) {
            t += sourceImages[i - 1].getWidth();
            sourceImages[i] = TranslateDescriptor.create(sourceImages[i], (float) t, 0.0f, INTERPOLATION, null);
            sourceThresholds[i][0] = determineSourceThreshold(sourceImages[i]);
        }
        return MosaicDescriptor.create(sourceImages, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null,
                                       sourceThresholds, null, null);
    }

    private static double determineSourceThreshold(RenderedImage image) {
        final int dataType = image.getSampleModel().getDataType();
        switch (dataType) {
            case (DataBuffer.TYPE_BYTE):
                return Byte.MIN_VALUE;
            case (DataBuffer.TYPE_USHORT):
                return 1.0;
            case (DataBuffer.TYPE_SHORT):
                return Short.MIN_VALUE + 1;
            case (DataBuffer.TYPE_INT):
                return Integer.MIN_VALUE + 1;
            case (DataBuffer.TYPE_FLOAT):
                return Float.MIN_VALUE + 1;
            case (DataBuffer.TYPE_DOUBLE):
                return Double.MIN_VALUE + 1;
        }
        return 0.0;
    }

    public static MultiLevelImage create(final MultiLevelImage... sourceImages) {
        final int mosaicWidth = mosaicWidth(sourceImages);
        final int mosaicHeight = sourceImages[0].getHeight();
        final Rectangle mosaicBounds = new Rectangle(mosaicWidth, mosaicHeight);
        final MultiLevelModel model = new DefaultMultiLevelModel(sourceImages[0].getModel().getLevelCount(),
                                                                 new AffineTransform(),
                                                                 mosaicWidth,
                                                                 mosaicHeight);

        final AbstractMultiLevelSource mosaicMultiLevelSource = new AbstractMultiLevelSource(model) {

            @Override
            protected RenderedImage createImage(int level) {
                final RenderedImage[] levelImages = new RenderedImage[sourceImages.length];
                for (int i = 0; i < levelImages.length; i++) {
                    levelImages[i] = sourceImages[i].getImage(level);
                }
                RenderedImage levelMosaic = create(levelImages);
                final int expectedWidth = expectedLevelImageWidth(mosaicBounds, model.getScale(level));
                if (expectedWidth != levelMosaic.getWidth()) {
                    final float scale = (float) expectedWidth / (float) levelMosaic.getWidth();
                    levelMosaic = ScaleDescriptor.create(levelMosaic, scale, 1.0f, 0.0f, 0.0f, INTERPOLATION, null);
                }

                return levelMosaic;
            }

            private int expectedLevelImageWidth(Rectangle mosaicBounds, double scale) {
                final Rectangle levelBounds = DefaultMultiLevelSource.getLevelImageBounds(mosaicBounds, scale);

                return levelBounds.width;
            }
        };

        return new DefaultMultiLevelImage(mosaicMultiLevelSource);
    }

    private static int mosaicWidth(MultiLevelImage[] sourceImages) {
        int w = 0;
        for (final MultiLevelImage sourceImage : sourceImages) {
            w += sourceImage.getWidth();
        }
        return w;
    }
}
