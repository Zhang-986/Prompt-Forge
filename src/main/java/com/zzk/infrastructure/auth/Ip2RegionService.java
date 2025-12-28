package com.zzk.infrastructure.auth;

import com.zzk.domain.service.auth.IpGeoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;

/**
 * IP2Region 实现的 IP 地理位置服务
 * 
 * <p>使用离线数据库，查询速度快，无需网络请求
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
public class Ip2RegionService implements IpGeoService {

    private Searcher searcher;

    /**
     * 初始化 IP2Region 搜索器
     */
    @PostConstruct
    public void init() {
        try {
            // 从 classpath 加载数据库文件
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            InputStream inputStream = resource.getInputStream();
            byte[] dbBytes = FileCopyUtils.copyToByteArray(inputStream);
            
            // 使用完全基于内存的查询（最快）
            searcher = Searcher.newWithBuffer(dbBytes);
            
            log.info("IP2Region 初始化成功，数据库大小: {} KB", dbBytes.length / 1024);
        } catch (Exception e) {
            log.error("IP2Region 初始化失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getLocation(String ip) {
        if (searcher == null) {
            return "未知";
        }
        
        // 跳过内网 IP
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        
        try {
            String region = searcher.search(ip);
            // 返回格式: 国家|区域|省份|城市|ISP
            // 示例: 中国|0|广东省|深圳市|电信
            return region;
        } catch (Exception e) {
            log.warn("IP 地址解析失败: ip={}, error={}", ip, e.getMessage());
            return "未知";
        }
    }

    @Override
    public String getFormattedLocation(String ip) {
        String location = getLocation(ip);
        
        if ("未知".equals(location) || "内网IP".equals(location)) {
            return location;
        }
        
        try {
            // 解析: 国家|区域|省份|城市|ISP
            String[] parts = location.split("\\|");
            if (parts.length < 5) {
                return location;
            }
            
            String country = parts[0];
            String province = parts[2];
            String city = parts[3];
            String isp = parts[4];
            
            StringBuilder formatted = new StringBuilder();
            
            // 中国显示省市，国外显示国家
            if ("中国".equals(country)) {
                if (!"0".equals(province)) {
                    formatted.append(province);
                }
                if (!"0".equals(city) && !city.equals(province)) {
                    formatted.append(city);
                }
            } else if (!"0".equals(country)) {
                formatted.append(country);
            }
            
            // 添加运营商
            if (!"0".equals(isp) && formatted.length() > 0) {
                formatted.append(" (").append(isp).append(")");
            }
            
            return formatted.length() > 0 ? formatted.toString() : "未知";
        } catch (Exception e) {
            log.warn("IP 地址格式化失败: location={}", location);
            return location;
        }
    }

    /**
     * 判断是否是内网 IP
     */
    private boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        
        // localhost
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        
        // 私有地址段
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("0.")) {
            return true;
        }
        
        // 172.16.0.0 - 172.31.255.255
        if (ip.startsWith("172.")) {
            try {
                String[] parts = ip.split("\\.");
                int second = Integer.parseInt(parts[1]);
                if (second >= 16 && second <= 31) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        
        return false;
    }
}
