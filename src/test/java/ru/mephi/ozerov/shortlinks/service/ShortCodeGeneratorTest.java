package ru.mephi.ozerov.shortlinks.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    private static final Pattern ALPHABET_PATTERN = Pattern.compile("[A-Za-z0-9]+");

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generate_returnsLength6ByDefault() {
        String code = generator.generate();
        assertEquals(6, code.length());
    }

    @Test
    void generate_withLength_returnsGivenLength() {
        assertEquals(4, generator.generate(4).length());
        assertEquals(10, generator.generate(10).length());
        assertEquals(1, generator.generate(1).length());
    }

    @RepeatedTest(20)
    void generate_containsOnlyAlphabetCharacters() {
        String code = generator.generate();
        assertTrue(
                ALPHABET_PATTERN.matcher(code).matches(),
                "Code should contain only A-Z, a-z, 0-9: " + code);
    }

    @RepeatedTest(10)
    void generate_producesDifferentCodes() {
        String a = generator.generate();
        String b = generator.generate();
        assertNotEquals(a, b);
    }
}
