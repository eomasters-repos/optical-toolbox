package eu.esa.opt.dataio.s3.synergy;

import eu.esa.opt.dataio.s3.util.S3NetcdfReader;
import eu.esa.opt.dataio.s3.util.S3Util;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.nc2.Variable;

/**
 * @author Tonio Fincke
 */
class SynOlcRadReader extends S3NetcdfReader {

    @Override
    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"N_LINE_OLC", "N_DET_CAM"}, {"N_SCAN_SLST_NAD_1km_L1C", "N_PIX_SLST_NAD_1km_L1C"},
                {"N_SCAN_SLST_NAD_05km_L1C", "N_PIX_SLST_NAD_05km_L1C"}, {"OLC_MISREG_ALT_DIM", "N_DET_CAM"}};
    }

    @Override
    protected String[] getSeparatingDimensions() {
        return new String[]{"N_CAM"};
    }

    @Override
    public String[] getSuffixesForSeparatingDimensions() {
        return new String[]{"CAM"};
    }

    @Override
    protected void addVariableAsBand(Product product, Variable variable, String variableName, boolean synthetic) {
        if (variableName.contains("MISREGIST_SLST") &&
                (variableName.contains("row_corresp") || variableName.contains("col_corresp"))) {
            final Band band = product.addBand(variableName, ProductData.TYPE_FLOAT32);
            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            band.setScalingFactor(S3Util.getScalingFactor(variable));
            band.setScalingOffset(S3Util.getAddOffset(variable));
            band.setSynthetic(synthetic);
            S3Util.addSampleCodings(product, band, variable, false);
            S3Util.addFillValue(band, variable);
        } else {
            super.addVariableAsBand(product, variable, variableName, synthetic);
        }
    }
}
