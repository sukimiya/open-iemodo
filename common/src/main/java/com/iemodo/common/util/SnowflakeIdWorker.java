package com.iemodo.common.util;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Twitter 雪花算法实现，用于生成分布式唯一ID。
 * 
 * <p>ID 结构（64位）：
 * <pre>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 * 符号位 - 41位时间戳（毫秒） - 5位数据中心ID - 5位工作节点ID - 12位序列号
 * </pre>
 *
 * <p>特性：
 * <ul>
 *   <li>趋势递增，粗略有序</li>
 *   <li>单机每秒可生成约 409.6 万个 ID</li>
 *   <li>支持 32 个数据中心，每个中心 32 个节点</li>
 *   <li>可使用 69 年（从 2024-01-01 开始计算）</li>
 * </ul>
 */
@Component
public class SnowflakeIdWorker {

    /** 起始时间戳（2024-01-01） */
    private static final long TWEPOCH = 1704067200000L;

    /** 工作节点ID位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心ID位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大工作节点ID：31 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 最大数据中心ID：31 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 工作节点ID左移位数：12 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心ID左移位数：17 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数：22 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /** 序列号掩码：4095 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 工作节点ID */
    private final long workerId;

    /** 数据中心ID */
    private final long datacenterId;

    /** 序列号 */
    private final AtomicLong sequence = new AtomicLong(0L);

    /** 上次生成ID的时间戳 */
    private volatile long lastTimestamp = -1L;

    /**
     * 默认构造函数，自动获取 workerId 和 datacenterId
     */
    public SnowflakeIdWorker() {
        this.datacenterId = getDatacenterId();
        this.workerId = getWorkerId(datacenterId);
    }

    /**
     * 指定工作节点和数据中心
     */
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID can't be greater than " + MAX_DATACENTER_ID + " or less than 0");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取下一个ID（线程安全）
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 时钟回拨检查
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for "
                    + (lastTimestamp - timestamp) + " milliseconds");
        }

        // 同一毫秒内，序列号自增
        if (lastTimestamp == timestamp) {
            long seq = sequence.incrementAndGet() & SEQUENCE_MASK;
            if (seq == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
                sequence.set(0);
            }
        } else {
            // 不同毫秒，重置序列号
            sequence.set(0);
        }

        lastTimestamp = timestamp;

        // 组装ID
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence.get();
    }

    /**
     * 获取字符串格式的ID
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 等待直到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 根据MAC地址获取数据中心ID
     */
    private long getDatacenterId() {
        long id = 0L;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    id = ((0x000000FF & (long) mac[mac.length - 2])
                            | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id & MAX_DATACENTER_ID;
                    break;
                }
            }
        } catch (Exception e) {
            id = new SecureRandom().nextInt((int) MAX_DATACENTER_ID + 1);
        }
        return id;
    }

    /**
     * 根据IP地址获取工作节点ID
     */
    private long getWorkerId(long datacenterId) {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (!ip.isLoopbackAddress() && ip.getHostAddress().indexOf(':') == -1) {
                        sb.append(ip.getHostAddress());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        long id;
        if (sb.length() == 0) {
            id = new SecureRandom().nextInt((int) MAX_WORKER_ID + 1);
        } else {
            id = (sb.toString().hashCode() & 0xffff) % (MAX_WORKER_ID + 1);
        }
        
        // 避免冲突：如果与 datacenterId 相同，则偏移
        if (id == datacenterId) {
            id = (id + 1) & MAX_WORKER_ID;
        }
        
        return id;
    }

    /**
     * 解析ID获取时间戳
     */
    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT) + TWEPOCH;
    }

    /**
     * 解析ID获取生成时间
     */
    public static LocalDateTime extractDateTime(long id) {
        long timestamp = extractTimestamp(id);
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );
    }

    /**
     * 生成带时间前缀的订单号（示例）
     */
    public String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date + nextId();
    }
}
