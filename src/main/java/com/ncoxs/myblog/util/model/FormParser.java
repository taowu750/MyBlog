package com.ncoxs.myblog.util.model;

import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.util.general.MapUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * application/x-www-form-urlencoded 格式数据的解析器。
 */
public class FormParser {

    private static final List<String> ONE_NULL_VALUES = Collections.singletonList(null);

    private Map<String, List<String>> params;

    public FormParser(String formData) {
        this(formData, UTF_8.name());
    }

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

    public void putParameter(String name, String value) {
        params.computeIfAbsent(name, k -> new ArrayList<>(1)).add(value);
    }

    public void putParameter(String name, String[] value) {
        params.computeIfAbsent(name, k -> new ArrayList<>(1)).addAll(Arrays.asList(value));
    }
}
