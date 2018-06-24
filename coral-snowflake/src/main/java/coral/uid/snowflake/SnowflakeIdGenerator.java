package coral.uid.snowflake;

import coral.core.IdGenerator;

/**
 * @author Ricky Fung
 * @version 1.0
 * @since 2018-06-24 19:02
 */
public interface SnowflakeIdGenerator extends IdGenerator {

    long[] getIdBatch(int batchSize);

    String parseId(long uid);
}
