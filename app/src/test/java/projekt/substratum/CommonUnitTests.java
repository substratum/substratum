package projekt.substratum;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommonUnitTests {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test(expected = NullPointerException.class)
    public void nullStringTest() {
        String str = null;
        assertTrue(str.isEmpty());
    }
}