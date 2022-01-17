package com.github.kischang.kisiot.updater;

/**
 * IOT OTA工具类
 *
 * @author KisChang
 * @date 2021-12-30
 */
public class KisiotOTAUpdaterUtils {

    /**
     * 检测固件版本
     *
     * @param otaUrl
     * @param nowVer
     * @return 不需要更新则返回null，否则返回版本号
     */
    public static String checkOtaVersion(String otaUrl, String nowVer) {
        return null;
    }

    /**
     * 下载更新的连接
     *
     * @param otaUrl  固件地址父路径
     * @param otaName 固件名称
     * @param toVer   目标版本
     * @return 固件的下载存储位置
     */
    public static String downloadOta(String otaUrl, String otaName, String toVer) {
        return null;
    }

    /**
     * 检测并安装
     *
     * @param otaFile 待安装的更新包
     * @return 是否成功ota
     */
    public static boolean checkAndUpdater(String otaFile) {
        return false;
    }

}
