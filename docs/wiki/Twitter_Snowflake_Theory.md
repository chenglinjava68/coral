# Twitter Snowflake

## 算法原理
[Twitter Snowflake](https://github.com/twitter/snowflake) 生成的 unique ID 的组成 (由高位到低位):

1. 41 bits: Timestamp (毫秒级)
2. 10 bits: 节点 ID (datacenter ID 5 bits + worker ID 5 bits)
3. 12 bits: sequence number

一共 63 bits (最高位是 0).

***************************************************************
| 0(最高位预留) | 时间戳(41位) | 机器ID(10位) | 自增序列(12位) |
***************************************************************

unique ID 生成过程:

* 10 bits 的机器号, 在 ID 分配 Worker 启动的时候，从一个 Zookeeper 集群获取 (保证所有的 Worker 不会有重复的机器号)；
* 41 bits 的 Timestamp: 每次要生成一个新 ID 的时候，都会获取一下当前的 Timestamp, 然后分两种情况生成 sequence number；
* 如果当前的 Timestamp 和前一个已生成 ID 的 Timestamp 相同 (在同一毫秒中)，就用前一个 ID 的 sequence number + 1 作为新的 sequence number (12 bits);  
  如果本毫秒内的所有 ID 用完，等到下一毫秒继续 (这个等待过程中, 不能分配出新的 ID)；
* 如果当前的 Timestamp 比前一个 ID 的 Timestamp 大, 随机生成一个初始 sequence number (12bits) 作为本毫秒内的第一个 sequence number；

41-bit的时间可以表示（1L<<41）/(1000L x 3600 x 24 x 365)=69年的时间，10-bit机器可以分别表示1024台机器。如果我们对IDC划分有需求，还可以将10-bit分5-bit给IDC，分5-bit给工作机器。这样就可以表示32个IDC，每个IDC下可以有32台机器，可以根据自身需求定义。12个自增序列号可以表示2^12个ID，理论上snowflake方案的QPS约为409.6w/s，这种分配方式可以保证在任何一个IDC的任何一台机器在任意毫秒内生成的ID都是不同的。

## 优缺点
### 优点：
* 毫秒数在高位，自增序列在低位，整个ID都是趋势递增的。
* 不依赖数据库等第三方系统，以服务的方式部署，稳定性更高，生成ID的性能也是非常高的。
* 可以根据自身业务特性分配bit位，非常灵活。

### 缺点：
* 强依赖机器时钟，如果机器上时钟回拨，会导致发号重复或者服务会处于不可用状态。

多台机器上部署时，要保证机器的时间一致（需要用到NTP保证系统时间精确）。

## Java版本Snowflake实现
```
/**
 * @author Ricky Fung
 */
public class SnowflakeIdGenerator implements IdGenerator {

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

    public SnowflakeIdGenerator(WorkerIdAssigner workerIdAssigner, long epoch) {
        this(workerIdAssigner.getWorkId(), epoch);
    }

    public SnowflakeIdGenerator(long workerId, long epoch) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId must in [0, %d]", MAX_WORKER_ID));
        }
        this.workerId = workerId;
        if(epoch<0 || epoch>System.currentTimeMillis()){
            throw new IllegalArgumentException(String.format("epoch must in (0, %d]", System.currentTimeMillis()));
        }
        this.epoch = epoch;
    }

    @Override
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

    @Override
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
```

## 集群方式部署
过程：
1.同一应用中的机器将在应用名节点下创建临时顺序节点，节点的Value值为 host+pid(当前机器主机名+进程编号)，如下：
```
/snowflake/node/0
/snowflake/node/1
/snowflake/node/2
```
2. 如果机器断开连接，节点将被删除，列如1号机器断开连接后
```
/snowflake/node/0
/snowflake/node/2
/snowflake/node/3
```

整个过程中只是在 Worker 启动的时候会对外部有依赖 (需要从 Zookeeper 获取 Worker 号) 之后就可以独立工作了，做到了去中心化。


