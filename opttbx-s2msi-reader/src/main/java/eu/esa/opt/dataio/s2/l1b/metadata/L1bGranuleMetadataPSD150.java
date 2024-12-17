package eu.esa.opt.dataio.s2.l1b.metadata;

import com.bc.ceres.core.Assert;
import eu.esa.opt.dataio.s2.*;
import eu.esa.opt.dataio.s2.l1b.L1bPSD150Constants;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.metadata.GenericXmlMetadata;
import org.esa.snap.core.metadata.XmlMetadataParser;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;
import org.locationtech.jts.geom.Coordinate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.esa.opt.dataio.s2.l1b.CoordinateUtils.as3DCoordinates;

/**
 * Created by obarrile on 07/10/2016.
 */
public class L1bGranuleMetadataPSD150 extends GenericXmlMetadata implements IL1bGranuleMetadata {

    MetadataElement simplifiedMetadataElement;
    String format = "";

    private static class L1bGranuleMetadataPSD150Parser extends XmlMetadataParser<L1bGranuleMetadataPSD150> {

        public L1bGranuleMetadataPSD150Parser(Class metadataFileClass) {
            super(metadataFileClass);
            setSchemaLocations(L1bPSD150Constants.getGranuleSchemaLocations());
            setSchemaBasePath(L1bPSD150Constants.getGranuleSchemaBasePath());
        }

        @Override
        protected boolean shouldValidateSchema() {
            return false;
        }
    }

    public static L1bGranuleMetadataPSD150 create(VirtualPath path) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(path);
        L1bGranuleMetadataPSD150 result = null;
        InputStream stream = null;
        try {
            if (path.exists()) {
                stream = path.getInputStream();
                L1bGranuleMetadataPSD150Parser parser = new L1bGranuleMetadataPSD150Parser(L1bGranuleMetadataPSD150.class);
                result = parser.parse(stream);
                result.updateName();
                result.format = "SAFE"; //at granule level SAFE and SAFE_COMPACT are equal
            }
        } finally {
            try {
                if(stream != null) {
                    stream.close();
                }
            } catch (IOException ignore) {
            }
        }
        return result;
    }

    public L1bGranuleMetadataPSD150(String name) {
        super(name);
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getMetadataProfile() {
        return null;
    }

    @Override
    public List<Coordinate> getGranuleCorners() {
        List<Double> polygon = new ArrayList<>();
        try {
            String polygonString = getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_GRANULE_CORNERS, null);
            String[] coordStrings = polygonString.split(" ");
            for (String coordString : coordStrings) {
                polygon.add(Double.parseDouble(coordString));
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Unable to get geometric information from L1B granule metadata");
            return null;
        }
        return as3DCoordinates(polygon);
    }

    @Override
    public String getGranuleID() {
        return getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_TILE_ID,"-1");
    }

    @Override
    public String getDetectorID() {
        return getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_DETECTOR_ID,"-1");
    }

    @Override
    public Map<S2SpatialResolution, S2Metadata.TileGeometry> getGranuleGeometries(S2Config config) {
        Map<S2SpatialResolution, S2Metadata.TileGeometry> resolutions = new HashMap<>();
        int pos = Integer.parseInt(getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_GRANULE_POSITION,"0"));
        String detector = getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_DETECTOR_ID,"-1");

        String[] resolutionsValues = getAttributeValues(L1bPSD150Constants.PATH_GRANULE_METADATA_SIZE_RESOLUTION);
        if(resolutionsValues == null) {
            return resolutions;
        }
        for (String res : resolutionsValues) {
            S2SpatialResolution resolution = S2SpatialResolution.valueOfResolution(Integer.parseInt(res));
            S2Metadata.TileGeometry tgeox = new S2Metadata.TileGeometry();

            tgeox.setNumCols(Integer.parseInt(getAttributeSiblingValue(L1bPSD150Constants.PATH_GRANULE_METADATA_SIZE_RESOLUTION, res,
                    L1bPSD150Constants.PATH_GRANULE_METADATA_SIZE_NCOLS, "0")));

            TileLayout tileLayout = config.getTileLayout(resolution);

            if (tileLayout != null) {
                tgeox.setNumRows(tileLayout.height);
            } else {
                SystemUtils.LOG.fine("No TileLayout at resolution R" + resolution + "m");
            }

            tgeox.setNumRowsDetector(Integer.parseInt(getAttributeSiblingValue(L1bPSD150Constants.PATH_GRANULE_METADATA_SIZE_RESOLUTION, res,
                    L1bPSD150Constants.PATH_GRANULE_METADATA_SIZE_NROWS, "0")));

            tgeox.setPosition(pos);

            tgeox.setResolution(resolution.resolution);

            tgeox.setxDim(resolution.resolution);
            tgeox.setyDim(-resolution.resolution);
            tgeox.setDetector(detector);

            resolutions.put(resolution, tgeox);
        }

        return resolutions;
    }

    @Override
    public S2Metadata.ProductCharacteristics getTileProductOrganization() {
        S2Metadata.ProductCharacteristics characteristics = new S2Metadata.ProductCharacteristics();
        characteristics.setSpacecraft("Sentinel-2");
        characteristics.setProcessingLevel("Level-1B");
        characteristics.setMetaDataLevel("Standard");

        List<S2BandInformation> aInfo = new ArrayList<>();

        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B1, S2SpatialResolution.R60M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B2, S2SpatialResolution.R10M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B3, S2SpatialResolution.R10M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B4, S2SpatialResolution.R10M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B5, S2SpatialResolution.R20M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B6, S2SpatialResolution.R20M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B7, S2SpatialResolution.R20M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B8, S2SpatialResolution.R10M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B8A, S2SpatialResolution.R20M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B9, S2SpatialResolution.R60M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B10, S2SpatialResolution.R60M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B11, S2SpatialResolution.R20M, getFormat()));
        aInfo.add(L1bMetadataProc.makeSpectralInformation(S2BandConstants.B12, S2SpatialResolution.R20M, getFormat()));

        int size = aInfo.size();
        characteristics.setBandInformations(aInfo.toArray(new S2BandInformation[size]));

        return characteristics;
    }

    @Override
    public MetadataElement getMetadataElement() {
        return rootElement;
    }

    @Override
    public MetadataElement getSimplifiedMetadataElement() {
        //TODO
        return rootElement;
    }

    @Override
    public String getFormat() {
        return format;
    }

    private void updateName() {
        String tileId = getAttributeValue(L1bPSD150Constants.PATH_GRANULE_METADATA_TILE_ID, null);
        if(tileId == null || tileId.length()<62) {
            setName("Level-1B_Granule_ID");
        }
        setName("Level-1B_Granule_" + tileId.substring(51, 61));
    }
}
