package com.zinhao.chtholly;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        String s = "wda[1Openai] ca[";
        Pattern p = Pattern.compile("\\[.*?]");
        Matcher m = p.matcher(s);
        boolean result = m.find();
        assertTrue(result);

    }
}