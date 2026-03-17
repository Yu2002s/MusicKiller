package xyz.jdynb.music.utils.lyric;

import android.os.Build;
import android.util.Log;

import com.drake.engine.utils.EncryptUtil;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

public class KuwoLyricDecryptor {

    private static final byte[] KEY = "yeelion".getBytes();
    private static final int KEY_LEN = KEY.length;

    /**
     * 构建请求参数（XOR加密 + Base64编码）
     * @param musicId 音乐ID
     * @param isGetLyricx 是否获取lrcx格式
     * @return 加密后的Base64参数
     */
    public static String buildParams(long musicId, boolean isGetLyricx) {
        String params = "user=12345,web,web,web&requester=localhost&req=1&rid=MUSIC_" + musicId;
        if (isGetLyricx) {
            params += "&lrcx=1";
        }

        byte[] bufStr = params.getBytes();
        int bufStrLen = bufStr.length;
        byte[] output = new byte[bufStrLen];

        // XOR加密
        for (int i = 0; i < bufStrLen; i++) {
            output[i] = (byte) (bufStr[i] ^ KEY[i % KEY_LEN]);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(output);
        } else {
            return android.util.Base64.encodeToString(output, android.util.Base64.DEFAULT);
        }
    }

    /**
     * 解密歌词数据
     * @param buf 原始响应数据
     * @param isGetLyricx 是否为lrcx格式
     * @return 解密后的歌词文本
     * @throws Exception 解密失败
     */
    public static String decodeLyrics(byte[] buf, boolean isGetLyricx) throws Exception {
        // 检查内容格式
        String header = new String(buf, 0, Math.min(10, buf.length), StandardCharsets.UTF_8);
        if (!header.startsWith("tp=content")) {
            return "";
        }

        // 提取压缩数据（跳过 "\r\n\r\n" 之后的内容）
        int dataStart = indexOf(buf, "\r\n\r\n".getBytes()) + 4;
        byte[] compressedData = new byte[buf.length - dataStart];
        System.arraycopy(buf, dataStart, compressedData, 0, compressedData.length);

        // zlib解压
        byte[] lrcData = inflate(compressedData);

        // 如果不是lrcx格式，直接用GB18030解码
        if (!isGetLyricx) {
            return new String(lrcData, Charset.forName("GB18030"));
        }

        // lrcx格式需要先Base64解码，再XOR解密
        String base64Str = new String(lrcData);
        byte[] bufStr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bufStr = Base64.getDecoder().decode(base64Str);
        } else {
            bufStr = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT);
        }
        int bufStrLen = bufStr.length;
        byte[] output = new byte[bufStrLen];

        // XOR解密
        for (int i = 0; i < bufStrLen; i++) {
            output[i] = (byte) (bufStr[i] ^ KEY[i % KEY_LEN]);
        }

        return new String(output, Charset.forName("GB18030"));
    }

    /**
     * 转换酷我歌词格式为标准LRC格式
     * @param rawLrc 原始歌词
     * @return 转换后的LRC格式歌词
     */
    public static String convertKuwoLrc(String rawLrc) {
        String[] lines = rawLrc.split("\\r\\n|\\r|\\n");
        int kuwoOffset = 1;
        int kuwoOffset2 = 1;

        // 解析kuwo标签
        Pattern kuwoTagPattern = Pattern.compile("\\[kuwo:(\\d+)\\]");
        Matcher kuwoTagMatch = kuwoTagPattern.matcher(rawLrc);
        if (kuwoTagMatch.find()) {
            int kuwoValue = Integer.parseInt(kuwoTagMatch.group(1), 8);
            kuwoOffset = kuwoValue / 10;
            kuwoOffset2 = kuwoValue % 10;
            if (kuwoOffset == 0 || kuwoOffset2 == 0) {
                kuwoOffset = 1;
                kuwoOffset2 = 1;
            }
        }

        Pattern lineTimePattern = Pattern.compile("^\\[(\\d{2}:\\d{2}\\.\\d{3})\\](.*)$");
        Pattern wordPattern = Pattern.compile("<(-?\\d+),(-?\\d+)>([^<]*)");
        Pattern translationPattern = Pattern.compile("[\\u4e00-\\u9fa5]");

        List<String> processedLrc = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher lineTimeMatch = lineTimePattern.matcher(line);

            if (!lineTimeMatch.matches()) {
                processedLrc.add(line);
                continue;
            }

            String content = lineTimeMatch.group(2);
            if (content.replace("<0,0>", "").trim().isEmpty()) {
                continue;
            }

            String lineTimeStr = lineTimeMatch.group(1);
            String[] timeParts = lineTimeStr.split("[:.]+");
            int lineStartTimeMs = Integer.parseInt(timeParts[0]) * 60000
                                + Integer.parseInt(timeParts[1]) * 1000
                                + Integer.parseInt(timeParts[2]);

            boolean isTranslationLine = content.startsWith("<0,0>")
                                       && translationPattern.matcher(content).find();

            if (!isTranslationLine) {
                StringBuilder newContent = new StringBuilder();
                Matcher wordMatcher = wordPattern.matcher(content);
                boolean firstWord = true;

                List<int[]> wordMatches = new ArrayList<>();
                while (wordMatcher.find()) {
                    int offset = Integer.parseInt(wordMatcher.group(1));
                    int offset2 = Integer.parseInt(wordMatcher.group(2));
                    String text = wordMatcher.group(3);

                    wordMatches.add(new int[]{offset, offset2});

                    int wordStartTimeMs = Math.abs((offset + offset2) / (kuwoOffset * 2));
                    int absoluteTimeMs = lineStartTimeMs + wordStartTimeMs;

                    if (firstWord) {
                        newContent.append(text);
                        firstWord = false;
                    } else {
                        newContent.append(formatTime(absoluteTimeMs)).append(text);
                    }
                }

                // 计算结束时间
                String calculatedEndTimestamp = "";
                if (!wordMatches.isEmpty()) {
                    int[] lastMatch = wordMatches.get(wordMatches.size() - 1);
                    int offset = lastMatch[0];
                    int offset2 = lastMatch[1];
                    int wordStartTimeMs = Math.abs((offset + offset2) / (kuwoOffset * 2));
                    int wordDurationMs = Math.abs((offset - offset2) / (kuwoOffset2 * 2));
                    int wordEndTimeMs = wordStartTimeMs + wordDurationMs;
                    int absoluteEndTimeMs = lineStartTimeMs + wordEndTimeMs;
                    calculatedEndTimestamp = formatTime(absoluteEndTimeMs);
                }

                // 检查是否有翻译行
                String translationText = "";
                String translationEndTimestamp = "";
                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1];
                    Matcher nextLineTimeMatch = lineTimePattern.matcher(nextLine);
                    if (nextLineTimeMatch.matches()) {
                        String nextContent = nextLineTimeMatch.group(2);
                        if (nextContent.startsWith("<0,0>")
                            && translationPattern.matcher(nextContent).find()) {
                            translationText = nextContent.replace("<0,0>", "").trim();

                            // 寻找翻译行的结束时间
                            for (int j = i + 2; j < lines.length; j++) {
                                Matcher futureLineMatch = lineTimePattern.matcher(lines[j]);
                                if (futureLineMatch.matches()) {
                                    translationEndTimestamp = "[" + futureLineMatch.group(1) + "]";
                                    break;
                                }
                            }
                            if (translationEndTimestamp.isEmpty()) {
                                translationEndTimestamp = calculatedEndTimestamp;
                            }
                            i++;
                        }
                    }
                }

                processedLrc.add("[" + lineTimeStr + "]" + newContent + calculatedEndTimestamp);
                if (!translationText.isEmpty()) {
                    processedLrc.add("[" + lineTimeStr + "]" + translationText + translationEndTimestamp);
                }
            }
        }

        return String.join("\n", processedLrc);
    }

    /**
     * 格式化时间戳
     */
    private static String formatTime(int ms) {
        if (ms < 0) ms = 0;
        int minutes = ms / 60000;
        int seconds = (ms % 60000) / 1000;
        int milliseconds = ms % 1000;
        return String.format("[%02d:%02d.%03d]", minutes, seconds, milliseconds);
    }

    /**
     * zlib解压
     */
    private static byte[] inflate(byte[] data) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        inflater.end();
        return outputStream.toByteArray();
    }

    /**
     * 查找字节数组中的子数组位置
     */
    private static int indexOf(byte[] array, byte[] target) {
        for (int i = 0; i <= array.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    /**
     * 示例用法
     */
    public static void main(String[] args) {
        try {
            long musicId = 514436619;
            boolean isGetLyricx = false;

            // 1. 构建请求参数
            String params = buildParams(musicId, isGetLyricx);
            String url = "http://newlyric.kuwo.cn/newlyric.lrc?" + params;
            System.out.println("请求URL: " + url);

            // 2. 发起HTTP请求获取响应数据（需要自己实现HTTP请求部分）
            // byte[] responseData = httpGet(url);
            byte[] responseData = new byte[1024];//HttpUtil.createGet(url).execute().bodyBytes();

            // 3. 解密歌词
            String rawLyric = decodeLyrics(responseData, isGetLyricx);
            System.out.println("原始歌词:\n" + rawLyric);

            // 4. 转换格式
            String convertedLyric = convertKuwoLrc(rawLyric);
            System.out.println("\n转换后的LRC歌词:\n" + convertedLyric);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
