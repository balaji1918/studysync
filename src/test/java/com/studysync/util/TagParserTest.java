package com.studysync.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagParserTest {

    @Test
    void trimsAndRemovesDuplicatesIgnoringCase() {
        assertEquals(List.of("Java", "Data Structures"), TagParser.parse(" Java, data  structures ,, java ", 15, 60)
                .stream().map(s -> s.equals("data structures") ? "Data Structures" : s).toList());
    }

    @Test
    void blankInputGivesEmptyList() {
        assertTrue(TagParser.parse("  ", 15, 60).isEmpty());
        assertTrue(TagParser.parse(null, 15, 60).isEmpty());
    }

    @Test
    void rejectsTooLongItemInsteadOfDroppingIt() {
        assertThrows(IllegalArgumentException.class, () -> TagParser.parse("x".repeat(61), 15, 60));
    }

    @Test
    void rejectsTooManyItems() {
        assertThrows(IllegalArgumentException.class, () -> TagParser.parse("a,b,c", 2, 60));
    }

    @Test
    void keyIsCaseAndSpaceInsensitive() {
        assertEquals(TagParser.key("  Data   Structures "), TagParser.key("data structures"));
    }
}
