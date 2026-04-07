package com.distributed.inventory.config;

public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static final String MASTER = "master";
    public static final String SLAVE = "slave";

    public static void setDataSourceType(String type) {
        CONTEXT_HOLDER.set(type);
    }

    public static String getDataSourceType() {
        String type = CONTEXT_HOLDER.get();
        return type == null ? MASTER : type;
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
