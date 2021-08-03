package com.ncoxs.myblog.handler.encryption;

// TODO: 返回 Map 等数据时怎么识别需要加密？可以使用 Aspect 解析注解，然后在 Request 中添加需要加密的标识
/**
 * 标识接口，标识一个类作为控制器方法返回值时需要被加密。
 */
public interface Encryptible {
}
