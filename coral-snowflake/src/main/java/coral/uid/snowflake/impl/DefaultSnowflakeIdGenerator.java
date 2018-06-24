package coral.uid.snowflake.impl;

import coral.uid.snowflake.SnowflakeIdGenerator;
import coral.uid.snowflake.support.TwitterSnowflake;
import coral.util.SystemPropertiesUtils;

/**
 * @author Ricky Fung
 * @version 1.0
 * @since 2018-06-24 22:31
 */
public class DefaultSnowflakeIdGenerator implements SnowflakeIdGenerator {
    private TwitterSnowflake twitterSnowflake;

    public DefaultSnowflakeIdGenerator() {
        long epoch = SystemPropertiesUtils.getLong("coral.epoch");
        long workerId = SystemPropertiesUtils.getLong("coral.workerId");
        this.twitterSnowflake = new TwitterSnowflake(epoch, workerId);
    }

    public DefaultSnowflakeIdGenerator(long epoch, long workerId) {
        this.twitterSnowflake = new TwitterSnowflake(epoch, workerId);
    }

    @Override
    public long getId() {
        return twitterSnowflake.getUid();
    }

    @Override
    public long[] getIdBatch(int batchSize) {
        if (batchSize<1) {
            throw new IllegalArgumentException("batchSize must be positive number!");
        }
        long[] arr = new long[batchSize];
        for (int i=0; i<batchSize; i++) {
            arr[i] = getId();
        }
        return arr;
    }

    @Override
    public String parseId(long uid) {
        return twitterSnowflake.parseUid(uid);
    }
}
