package coral.uid.snowflake.support;

/**
 * @author Ricky Fung
 * @version 1.0
 * @since 2018-06-24 22:34
 */
public class TwitterSnowflake {
    private long epoch;    //纪元

    public static final long WORKER_ID_BITS = 10L;   //机器标识位数
    public static final long SEQUENCE_BITS = 12L;  //毫秒内自增位

    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);   //机器ID最大值

    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;    //机器ID偏左移12位

    private static final long TIMESTAMP_LEFT_SHIFT_BITS  = SEQUENCE_BITS + WORKER_ID_BITS; //时间毫秒左移22位

    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public TwitterSnowflake(long epoch, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId must in [0, %d]", MAX_WORKER_ID));
        }
        this.workerId = workerId;
        if(epoch<0 || epoch>System.currentTimeMillis()){
            throw new IllegalArgumentException(String.format("epoch must in (0, %d]", System.currentTimeMillis()));
        }
        this.epoch = epoch;
    }

    public synchronized long getUid() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            if (0L == (++sequence & SEQUENCE_MASK)) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }

    public String parseUid(long uid) {

        long tt = (uid >> TIMESTAMP_LEFT_SHIFT_BITS) + epoch;
        long worker = (uid >> WORKER_ID_LEFT_SHIFT_BITS) & ((1 << WORKER_ID_BITS) - 1);
        long seq = uid & SEQUENCE_MASK;

        StringBuilder sb = new StringBuilder(128);
        sb.append('{').append('"').append("timestamp").append('"').append(':').append(tt).append(',')
                .append('"').append("workerId").append('"').append(':').append(worker).append(',')
                .append('"').append("sequence").append('"').append(':').append(seq).append('}');
        return sb.toString();
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
