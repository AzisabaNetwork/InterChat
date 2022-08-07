package net.azisaba.interchat.test;

import net.azisaba.interchat.api.util.TimeUtil;
import org.junit.jupiter.api.Test;

public class TimeUtilTest {
    @Test
    public void checkRelativeTimeAbs() {
        String time = TimeUtil.toRelativeTimeAbs(0, 1000);
        assert time.equals("1s") : time;
        time = TimeUtil.toRelativeTimeAbs(202208072355L, 1L);
        assert time.equals("2340d 8h 54m 32s 354ms") : time;
    }
}
