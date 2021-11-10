package com.ncoxs.myblog.util.codec;

import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.exception.DecompressException;
import org.springframework.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 压缩/解压相关工具类
 */
public class CompressUtil {

    public static byte[] zipCompress(byte[] data) throws IOException {
        FastByteArrayOutputStream byteOut = new FastByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteOut);
        zipOutputStream.putNextEntry(new ZipEntry("content"));
        zipOutputStream.write(data);
        zipOutputStream.close();

        return byteOut.toByteArray();
    }

    public static byte[] compress(byte[] data, String compressMode) throws IOException {
        switch (compressMode) {
            case HttpHeaderConst.COMPRESS_MODE_ZIP:
                return zipCompress(data);

            default:
                throw new IllegalArgumentException("compressMode error");
        }
    }

    public static byte[] zipDecompress(InputStream in) throws IOException {
        // 加载数据
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry zipEntry = zipIn.getNextEntry();
        if (zipIn.available() == 0 || zipEntry == null) {
            throw new DecompressException("no data");
        }

        // 解压数据
        byte[] data = new byte[2048];
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        int len;
        while ((len = zipIn.read(data)) != -1) {
            out.write(data, 0, len);
        }
        out.close();

        return out.toByteArray();
    }

    public static byte[] decompress(InputStream in, String compressMode) throws IOException {
        switch (compressMode) {
            case HttpHeaderConst.COMPRESS_MODE_ZIP:
                return zipDecompress(in);

            default:
                throw new IllegalArgumentException("compressMode error");
        }
    }
}
