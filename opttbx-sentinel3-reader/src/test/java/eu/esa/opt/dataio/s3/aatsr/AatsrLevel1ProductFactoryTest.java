package eu.esa.opt.dataio.s3.aatsr;

import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Created by Sabine on 20.02.2017.
 */
public class AatsrLevel1ProductFactoryTest {

    private AatsrLevel1ProductFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new AatsrLevel1ProductFactory(null);
    }

    @Test
    public void testGetProductSpecificMetadataElementName() throws Exception {
        assertEquals("atsrProductInformation", factory.getProductSpecificMetadataElementName());
    }

    @Test
    public void testFindMasterProduct() throws Exception {
        final ArrayList<Product> openProductList = new ArrayList<>();
        openProductList.add(new Product("p1", "t", 12, 14));
        openProductList.add(new Product("p2", "t", 12, 15));
        openProductList.add(new Product("p3", "t", 13, 15));
        openProductList.add(new Product("p4", "t", 13, 14));
        openProductList.add(new Product("p5", "t", 12, 14));
        final Product masterProduct = AatsrLevel1ProductFactory.findMasterProduct(openProductList);
        assertEquals("p3", masterProduct.getName());
    }
}