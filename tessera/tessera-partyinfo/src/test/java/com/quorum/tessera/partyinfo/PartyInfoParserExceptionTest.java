
package com.quorum.tessera.partyinfo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;


public class PartyInfoParserExceptionTest {
    
    @Test
    public void createWithMessage() {
        
        PartyInfoParserException result = new PartyInfoParserException("OUCH");
        
        assertThat(result).hasMessage("OUCH");
        
    }
    
}
