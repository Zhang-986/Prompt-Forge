package com.zzk.domain.service.auth;

/**
 * IP 地理位置服务接口
 * 
 * <p>根据 IP 地址解析地理位置信息
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface IpGeoService {

    /**
     * 根据 IP 地址获取地理位置
     * 
     * @param ip IP 地址
     * @return 地理位置字符串，如 "中国|0|广东省|深圳市|电信"，解析失败返回 "未知"
     */
    String getLocation(String ip);

    /**
     * 根据 IP 地址获取格式化的地理位置
     * 
     * @param ip IP 地址
     * @return 格式化的地理位置，如 "广东省深圳市"
     */
    String getFormattedLocation(String ip);
}
