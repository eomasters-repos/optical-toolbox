package eu.esa.opt.dataio.s3.olci;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.VirtualDir;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.esa.opt.dataio.s3.olci.OlciProductFactory.SYSPROP_OLCI_TIE_POINT_CODING_FORWARD;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OlciProductFactoryTest {

    @Test
    public void testUnitExtrationFromLogScaledUnit() {
        String logUnit = "lg(re g.m-3)";
        Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
        final Matcher m = pattern.matcher(logUnit);
        assertTrue(m.matches());

        assertEquals(logUnit, m.group(0));
        assertEquals("g.m-3", m.group(1));
    }

    @Test
    public void testGetResolutionInKm() {
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_1_EFR"), 1e-8);
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_2_LFR"), 1e-8);
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_2_WFR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_1_ERR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_2_LRR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_2_WRR"), 1e-8);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetResolutionInKm_invalid() {
        try {
            OlciProductFactory.getResolutionInKm("heffalump");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetMaxLineTimeDelta() {
        assertEquals(66057, OlciProductFactory.getMaxLineTimeDelta("OL_1_EFR"));
        assertEquals(66057, OlciProductFactory.getMaxLineTimeDelta("OL_2_LFR"));
        assertEquals(66057, OlciProductFactory.getMaxLineTimeDelta("OL_2_WFR"));
        assertEquals(264054, OlciProductFactory.getMaxLineTimeDelta("OL_1_ERR"));
        assertEquals(264054, OlciProductFactory.getMaxLineTimeDelta("OL_2_LRR"));
        assertEquals(264054, OlciProductFactory.getMaxLineTimeDelta("OL_2_WRR"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetMaxLineTimeDelta_invalid() {
        try {
            OlciProductFactory.getMaxLineTimeDelta("wamafugani");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_tiePointCoding_forwardKey() {
        final String forwardKey = System.getProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
        try {
            System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, "YEAH!");

            final String[] codingKeys = OlciProductFactory.getForwardAndInverseKeys_tiePointCoding();
            assertEquals("YEAH!", codingKeys[0]);
            assertEquals("INV_TIE_POINT", codingKeys[1]);

        } finally {
            if (forwardKey != null) {
                System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, forwardKey);
            } else {
                System.clearProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
            }
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_tiePointCoding_default() {
        final String forwardKey = System.getProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
        try {
            System.clearProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);

            final String[] codingKeys = OlciProductFactory.getForwardAndInverseKeys_tiePointCoding();
            assertEquals("FWD_TIE_POINT_BILINEAR", codingKeys[0]);
            assertEquals("INV_TIE_POINT", codingKeys[1]);

        } finally {
            if (forwardKey != null) {
                System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, forwardKey);
            }
        }
    }

    @Test
    @STTM("SNAP-3755")
    public void testGetFileFromVirtualDir() throws IOException {
        final VirtualDir virtualDir = mock(VirtualDir.class);
        when(virtualDir.listAllFiles()).thenReturn(new String[]{"one", "be_two", "two", "three"});
        when(virtualDir.getFile("be_two")).thenReturn(new File("be_two"));

        final File notExisting = OlciProductFactory.getFileFromVirtualDir("not_existing", virtualDir);
        assertNull(notExisting);

        final File beTwo = OlciProductFactory.getFileFromVirtualDir("be_two", virtualDir);
        assertNotNull(beTwo);
        assertEquals("be_two", beTwo.getName());

        final File eTwo = OlciProductFactory.getFileFromVirtualDir("e_two", virtualDir);
        assertNull(eTwo);
    }

    @Test
    @STTM("SNAP-3755")
    public void testGetFileFromVirtualDir_includesPath() throws IOException {
        final VirtualDir virtualDir = mock(VirtualDir.class);
        when(virtualDir.listAllFiles()).thenReturn(new String[]{"the_zip_file_name/one", "the_zip_file_name/be_two", "the_zip_file_name/two", "the_zip_file_name/three"});
        when(virtualDir.getFile("the_zip_file_name/be_two")).thenReturn(new File("be_two"));

        final File notExisting = OlciProductFactory.getFileFromVirtualDir("not_existing", virtualDir);
        assertNull(notExisting);

        final File beTwo = OlciProductFactory.getFileFromVirtualDir("be_two", virtualDir);
        assertNotNull(beTwo);
        assertEquals("be_two", beTwo.getName());

        final File eTwo = OlciProductFactory.getFileFromVirtualDir("e_two", virtualDir);
        assertNull(eTwo);
    }

    @Test
    @STTM("SNAP-3755")
    public void testIsUncertaintyBand() {
        assertTrue(OlciProductFactory.isUncertaintyBand("Oa03_radiance_unc"));
        assertTrue(OlciProductFactory.isUncertaintyBand("Oa11_radiance_unc"));

        assertFalse(OlciProductFactory.isUncertaintyBand("Oa11_radiance"));
        assertFalse(OlciProductFactory.isUncertaintyBand("atmospheric_temperature_profile"));
    }

    @Test
    @STTM("SNAP-3755")
    public void testIsLogScaledUnit() {
        assertTrue(OlciProductFactory.isLogScaledUnit("lg(firlefanz)"));
        assertTrue(OlciProductFactory.isLogScaledUnit("lg(re mW.m-2.sr-1.nm-1)"));

        assertFalse(OlciProductFactory.isLogScaledUnit("mW.m-2.sr-1.nm-1"));
        assertFalse(OlciProductFactory.isLogScaledUnit("K"));
    }

    @Test
    @STTM("SNAP-3728")
    public void testIsLogScaledUnit_handleNullOrEmpty() {
        assertFalse(OlciProductFactory.isLogScaledUnit(""));
        assertFalse(OlciProductFactory.isLogScaledUnit(null));
    }

    @Test
    @STTM("SNAP-3728")
    public void testIsLogScaledGeophysicalData() {
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("anw_443"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("aphy_443"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("bbp_443"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("bbp_slope"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("ADG443_NN"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("CHL_OC4ME_unc"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("KD490_M07"));
        assertTrue(OlciProductFactory.isLogScaledGeophysicalData("TSM_NN_unc"));

        assertFalse(OlciProductFactory.isLogScaledGeophysicalData("Oa03_radiance_unc"));
        assertFalse(OlciProductFactory.isLogScaledGeophysicalData("Oa11_radiance"));
    }

    @Test
    @STTM("SNAP-3728")
    public void testStripLogFromUnit() {
        assertEquals("kgm-3", OlciProductFactory.stripLogFromUnit("lg(kgm-3)"));
        assertEquals("m-1", OlciProductFactory.stripLogFromUnit("lg(re m-1)"));
        assertEquals("g.m-3", OlciProductFactory.stripLogFromUnit("lg(re g.m-3)\n"));
        assertEquals("", OlciProductFactory.stripLogFromUnit("lg"));
        assertEquals("", OlciProductFactory.stripLogFromUnit(""));
        assertEquals("", OlciProductFactory.stripLogFromUnit(null));

        assertEquals("degrees_north", OlciProductFactory.stripLogFromUnit("degrees_north"));
    }

    @Test
    @STTM("SNAP-3728")
    public void testStripLogFromDescription() {
        assertEquals("(Neural Net) Total suspended matter concentration", OlciProductFactory.stripLogFromDescription("log10 scaled (Neural Net) Total suspended matter concentration"));
        assertEquals("Reflectance for OLCI acquisition band Oa10", OlciProductFactory.stripLogFromDescription("Reflectance for OLCI acquisition band Oa10\n"));
        assertEquals("", OlciProductFactory.stripLogFromDescription(""));
        assertEquals("", OlciProductFactory.stripLogFromDescription(null));
    }
}
