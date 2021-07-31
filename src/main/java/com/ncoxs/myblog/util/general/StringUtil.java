package com.ncoxs.myblog.util.general;

public class StringUtil {

    private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            sb.append(HEX_CHAR[(value & 0xf0) >>> 4]);
            sb.append(HEX_CHAR[value & 0x0f]);
        }
        return sb.toString();
    }

    public static byte[] fromHexString(String hexString) {
        byte[] bytes;
        bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2),
                    16);
        }
        return bytes;
    }
}
