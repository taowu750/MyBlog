package com.ncoxs.myblog.util.model;

import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.util.general.MapUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import com.ncoxs.myblog.util.general.UnitUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.*;


/**
 * application/x-www-form-urlencoded 格式数据的解析器。
 */
public class FormParser {

    private static final List<String> ONE_NULL_VALUES = Collections.singletonList(null);

    /**
     * 请求的最大大小
     */
    private static long maxRequestSize;
    /**
     * 上传文件的最大大小
     */
    private static long maxFileSize;
    /**
     * 上传文件占用的最大内存
     */
    private static long fileSizeThreshold;
    /**
     * 上传文件临时存储目录
     */
    private static String tmpFileLocation;
    static {
        try {
            Properties properties = new Properties();
            properties.load(ResourceUtil.load("application.properties"));

            String env = (String) properties.get("spring.profiles.active");
            Yaml yaml = new Yaml();
            Map<String, Map<String, Map<String, Map<String, Object>>>> props;
            if (env.equals("dev")) {
                //noinspection unchecked
                props = yaml.loadAs(ResourceUtil.load("application-dev-main.yml"), HashMap.class);
            } else {
                //noinspection unchecked
                props = yaml.loadAs(ResourceUtil.load("application-prod-main.yml"), HashMap.class);
            }

            maxRequestSize = UnitUtil.size2byte((String) props.get("spring").get("servlet").get("multipart").get("max-request-size"));
            maxFileSize = UnitUtil.size2byte((String) props.get("spring").get("servlet").get("multipart").get("max-file-size"));
            fileSizeThreshold = UnitUtil.size2byte((String) props.get("spring").get("servlet").get("multipart").get("file-size-threshold"));
            tmpFileLocation = (String) props.get("spring").get("servlet").get("multipart").get("location");
            if (tmpFileLocation.startsWith("classpath:")) {
                tmpFileLocation = tmpFileLocation.substring(10);
            }
        } catch (IOException e) {
            throw new ImpossibleError(e);
        }
    }

    private Map<String, List<String>> params;
    private Map<String, Part> parts;

    /**
     * 解析 application/x-www-form-urlencoded 数据
     */
    public FormParser(String formData, String encode) {
        Objects.requireNonNull(formData);
        Objects.requireNonNull(encode);

        String[] kvs = formData.split("&");
        params = MapUtil.ofCap(kvs.length);

        try {
            for (String kvStr : kvs) {
                String[] kv = kvStr.split("=");
                params.computeIfAbsent(URLDecoder.decode(kv[0], encode), k -> new ArrayList<>(1))
                        .add(URLDecoder.decode(kv[1], encode));
            }
        } catch (UnsupportedEncodingException e) {
            throw new ImpossibleError(e);
        }
    }

    /**
     * 解析 multipart/form-data 数据
     */
    public FormParser(HttpServletRequest request, String encode) throws FileUploadException, UnsupportedEncodingException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold((int) fileSizeThreshold);
        factory.setRepository(Paths.get(ResourceUtil.classpath(), tmpFileLocation).toFile());

        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        fileUpload.setSizeMax(maxRequestSize);
        fileUpload.setFileSizeMax(maxFileSize);

        List<FileItem> fileItems = fileUpload.parseRequest(request);
        params = new HashMap<>();
        parts = new HashMap<>();
        for (FileItem fileItem : fileItems) {
            parts.put(fileItem.getFieldName(), new Part() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return fileItem.getInputStream();
                }

                @Override
                public String getContentType() {
                    return fileItem.getContentType();
                }

                @Override
                public String getName() {
                    return fileItem.getFieldName();
                }

                @Override
                public String getSubmittedFileName() {
                    return fileItem.getName();
                }

                @Override
                public long getSize() {
                    return fileItem.getSize();
                }

                @Override
                public void write(String fileName) throws IOException {
                    try {
                        fileItem.write(new File(fileName));
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }

                @Override
                public void delete() throws IOException {
                    fileItem.delete();
                }

                @Override
                public String getHeader(String name) {
                    return null;
                }

                @Override
                public Collection<String> getHeaders(String name) {
                    return Collections.emptyList();
                }

                @Override
                public Collection<String> getHeaderNames() {
                    return Collections.emptyList();
                }
            });

            if (fileItem.isFormField()) {
                params.put(fileItem.getFieldName(), Collections.singletonList(new String(fileItem.get(), encode)));
            }
        }
    }

    public String getParameter(String name) {
        return params.getOrDefault(name, ONE_NULL_VALUES).get(0);
    }

    public Enumeration<String> getParameterNames() {
        Iterator<String> nameIter = params.keySet().iterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return nameIter.hasNext();
            }

            @Override
            public String nextElement() {
                return nameIter.next();
            }
        };
    }

    public String[] getParameterValues(String name) {
        if (params.containsKey(name)) {
            return params.get(name).toArray(new String[0]);
        } else {
            return null;
        }
    }

    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = MapUtil.ofCap(params.size());
        for (Map.Entry<String, List<String>> kv : params.entrySet()) {
            result.put(kv.getKey(), kv.getValue().toArray(new String[0]));
        }

        return Collections.unmodifiableMap(result);
    }

    public Collection<Part> getParts() {
        return parts != null ? parts.values() : Collections.emptyList();
    }

    public Part getPart(String name) {
        return parts != null ? parts.get(name) : null;
    }
}
