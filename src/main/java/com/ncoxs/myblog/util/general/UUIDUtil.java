package com.ncoxs.myblog.util.general;

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

public class UUIDUtil {

    private static final ThreadLocal<IdGenerator> ID_GENERATOR_THREAD_LOCAL = ThreadLocal.withInitial(
            AlternativeJdkIdGenerator::new);


    public static String generate() {
        return ID_GENERATOR_THREAD_LOCAL.get().
                generateId().toString().replace("-", "");
    }
}
