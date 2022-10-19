package fi.hsl.transitdata.vehicleposition.application.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeqCacheTest {
    @Test
    public void testSeqCache() {
        SeqCache seqCache = new SeqCache();

        assertTrue(seqCache.isSmallestSeq("1", 7));
        assertFalse(seqCache.isSmallestSeq("1", 9));
        assertTrue(seqCache.isSmallestSeq("1", 7));
        assertTrue(seqCache.isSmallestSeq("1", 1));
    }
}
