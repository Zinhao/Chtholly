package com.zinhao.chtholly;

import com.zinhao.chtholly.utils.FilenameFilter;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

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

    // ============ 测试 ============
    @Test
    public void testFilenameFilter() {
        String[] testNames = {
                "正常文件名.txt",
                "非法<字符>:测试.txt",
                "包含/斜杠\\的文件.txt",
                "   首尾空格  .txt   ",
                "CON",           // Windows 保留名
                "COM1.txt",      // Windows 保留名
                "文件名\u0001含控制字符.txt",
                "../../etc/passwd",  // 路径遍历尝试
                "超长文件名" + "x".repeat(200) + ".txt",
                "",              // 空字符串
                "   ",           // 纯空格
                ".",             // 单点
                "文件名🎉表情.txt"  // Emoji
        };

        System.out.println("=== 基础过滤 ===");
        for (String name : testNames) {
            System.out.printf("%-30s -> %s%n",
                    "\"" + name.substring(0, Math.min(name.length(), 25)) + "\"",
                    FilenameFilter.sanitizeBasic(name));
        }

        System.out.println("\n=== 替换模式 ===");
        System.out.println(FilenameFilter.sanitizeWithReplacement("a<b>c:d|e*f?g", '_'));

        System.out.println("\n=== 严格模式 ===");
        System.out.println(FilenameFilter.sanitizeStrict("Hello世界@#$%^&*()文件.txt"));

        System.out.println("\n=== 路径安全 ===");
        System.out.println(FilenameFilter.sanitizePathSafe("../../../etc/passwd"));
    }
}