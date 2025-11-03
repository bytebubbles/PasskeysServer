package com.example.passkeys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64UrlUtils {

    /**
     * 将字符串编码为 Base64URL（无 padding）
     */
    public static String encode(String input) {
        if (input == null) return null;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将字节数组编码为 Base64URL（无 padding）
     */
    public static String encode(byte[] input) {
        if (input == null) return null;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input);
    }

    /**
     * 将 Base64URL 解码为字符串
     */
    public static String decodeToString(String base64url) {
        if (base64url == null) return null;
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64url);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 将 Base64URL 解码为字节数组
     */
    public static byte[] decode(String base64url) {
        if (base64url == null) return null;
        return Base64.getUrlDecoder().decode(base64url);
    }


    /**
     * 将带冒号的 SHA-256 哈希（如 "35:9E:3B:E6:..."）
     * 转换为 Base64URL（无 padding）格式。
     *
     * 示例：
     *   输入: 35:9E:3B:E6:83:AC:EC:78:AF:A6:23:C7:76:13:E1:0C:3F:D2:27:B6:49:2D:4E:5D:E5:B8:B9:01:AD:51:61:94
     *   输出: NZ474IOs7HivpiPHdhPhDD_SJ7ZJLluV5bi5Aa1RYZQ
     */
    public static String apkKeyHashToBase64Url(String hexWithColon) {
        if (hexWithColon == null) return null;

        // 去掉冒号和空格
        String hex = hexWithColon.replace(":", "").replace(" ", "").toUpperCase();

        // 转成字节数组
        byte[] bytes = hexStringToByteArray(hex);

        // 编码为 Base64URL
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string length must be even");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

//    public static void main(String[] args) {
//        String hash = "35:9E:3B:E6:83:AC:EC:78:AF:A6:23:C7:76:13:E1:0C:3F:D2:27:B6:49:2D:4E:5D:E5:B8:B9:01:AD:51:61:94";
//        String base64url = apkKeyHashToBase64Url(hash);
//        System.out.println("android:apk-key-hash:" + base64url);
//    }
}