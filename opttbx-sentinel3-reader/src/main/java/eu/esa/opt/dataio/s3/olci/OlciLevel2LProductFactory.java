package eu.esa.opt.dataio.s3.olci;

import eu.esa.opt.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public class OlciLevel2LProductFactory extends OlciProductFactory {

    public OlciLevel2LProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getValidExpression() {
        return "!LQSF.INVALID";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {

        final String vegVariable = getVegetationVariable(targetProduct);
        targetProduct.setAutoGrouping("IWV:" + vegVariable + ":OTCI:RC681:RC865:atmospheric_temperature_profile:" +
                                              "lambda0:FWHM:solar_flux");
    }

    @Override
    protected void setMasks(Product targetProduct) {
        super.setMasks(targetProduct);
        String generalMaskName = "LQSF_GENERAL_RECOM";
        targetProduct.addMask(generalMaskName, "LQSF.CLOUD or LQSF.SNOW_ICE",
                              "Excluding pixels that are deemed unreliable. Flag recommended by QWG.",
                              getColorProvider().getMaskColor(generalMaskName), 0.5);
        String otciMaskName = "LQSF_OTCI_RECOM";
        targetProduct.addMask(otciMaskName, "LQSF.CLOUD or " +
                                      "LQSF.SNOW_ICE or LQSF.OTCI_FAIL or LQSF.OTCI_CLASS_CLSN",
                              "Excluding pixels that are deemed unreliable for OTCI. Flag recommended by QWG.",
                              getColorProvider().getMaskColor(otciMaskName), 0.5);

        final String vegVariable = getVegetationVariable(targetProduct);
        String gifaparMaskName = "LQSF_" + vegVariable + "_RECOM";
        targetProduct.addMask(gifaparMaskName, "LQSF.CLOUD or " +
                                      "LQSF.SNOW_ICE or LQSF." + vegVariable + "_FAIL",
                              "Excluding pixels that are deemed unreliable for " + vegVariable + ". Flag recommended by QWG.",
                              getColorProvider().getMaskColor(gifaparMaskName), 0.5);

        String iwvMaskName = "LQSF_IWV_RECOM";
        targetProduct.addMask(iwvMaskName, "LQSF.CLOUD or " +
                                      "LQSF.SNOW_ICE or LQSF.WV_FAIL",
                              "Excluding pixels that are deemed unreliable for IWV. Flag recommended by QWG.",
                              getColorProvider().getMaskColor(iwvMaskName), 0.5);
    }

    private String getVegetationVariable(Product targetProduct) {
        return targetProduct.getBandGroup().contains("GIFAPAR") ? "GIFAPAR" : "OGVI";
    }
}
