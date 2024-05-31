package eu.esa.opt.dataio.s3.meris;

import eu.esa.opt.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductFactory extends MerisProductFactory {

    private final static String validExpression = "!quality_flags.invalid";

    public MerisLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode.getName().matches("M[0-1][0-9].*")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                final String targetName = targetBand.getName();
                targetBand.setSpectralBandIndex(getBandindex(targetName));
                targetBand.setSpectralWavelength(getWavelength(targetName));
                targetBand.setSpectralBandwidth(getBandwidth(targetName));
            }
        }
        targetNode.setValidPixelExpression(getValidExpression());
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("M*_radiance:M*_radiance_err:atmospheric_temperature_profile:lambda0:FWHM:solar_flux");
    }

    @Override
    protected String getValidExpression() {
        return validExpression;
    }

}
