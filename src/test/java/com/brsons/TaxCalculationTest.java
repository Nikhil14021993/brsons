package com.brsons;

import com.brsons.service.TaxCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class TaxCalculationTest {

    @Test
    public void testTaxTypeDetermination() {
        TaxCalculationService taxService = new TaxCalculationService();
        
        // Test Rajasthan (business state) - should be CGST_SGST
        assertEquals("CGST_SGST", taxService.determineTaxType("Rajasthan"));
        assertTrue(taxService.isIntraStateTransaction("Rajasthan"));
        
        // Test other states - should be IGST
        assertEquals("IGST", taxService.determineTaxType("Maharashtra"));
        assertEquals("IGST", taxService.determineTaxType("Gujarat"));
        assertEquals("IGST", taxService.determineTaxType("Karnataka"));
        assertEquals("IGST", taxService.determineTaxType("Delhi"));
        
        // Test null/empty - should be UNKNOWN
        assertEquals("UNKNOWN", taxService.determineTaxType(null));
        assertEquals("UNKNOWN", taxService.determineTaxType(""));
        assertEquals("UNKNOWN", taxService.determineTaxType("   "));
        
        // Test case insensitive
        assertEquals("CGST_SGST", taxService.determineTaxType("rajasthan"));
        assertEquals("CGST_SGST", taxService.determineTaxType("RAJASTHAN"));
        assertEquals("CGST_SGST", taxService.determineTaxType(" Rajasthan "));
    }
    
    @Test
    public void testBusinessState() {
        TaxCalculationService taxService = new TaxCalculationService();
        assertEquals("Rajasthan", taxService.getBusinessState());
    }
}
