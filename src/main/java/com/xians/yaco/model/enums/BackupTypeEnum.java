package com.xians.yaco.model.enums;

/**
 * <pre>
 *     备份类型enum
 * </pre>
 *
 * @author : XIANS
 */
public enum BackupTypeEnum {

    /**
     * 资源文件
     */
    RESOURCES("resources"),

    /**
     * 数据库
     */
    DATABASES("databases"),

    /**
     * 文章
     */
    POSTS("posts");

    private String desc;

    BackupTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}