/*
 * Copyright 2010-2013 10gen Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.hadoop.hive.output;

import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.mapred.BSONFileOutputFormat;
import com.mongodb.hadoop.mapred.output.BSONFileRecordWriter;
import com.mongodb.hadoop.splitter.BSONSplitter;
import com.mongodb.hadoop.util.MongoConfigUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.hive.ql.io.HiveOutputFormat;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TaskAttemptContext;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.util.Properties;

/**
 * An OutputFormat that writes BSON files
 */
@SuppressWarnings("deprecation")
public class HiveBSONFileOutputFormat<K, V>
    extends BSONFileOutputFormat<K, V> implements HiveOutputFormat<K, V> {

    private static final Log LOG = LogFactory.getLog(HiveBSONFileOutputFormat.class);
    public static final String MONGO_OUTPUT_FILE = "mongo.output.file";

    /**
     * create the final output file
     *
     * @param jc              the job configuration
     * @param fileOutputPath  the file that the output should be directed at
     * @param valueClass      the value class used to create
     * @param tableProperties the tableInfo for this file's corresponding table
     * @return RecordWriter for the output file
     */
    @Override
    public RecordWriter getHiveRecordWriter(final JobConf jc,
                                            final Path fileOutputPath,
                                            final Class<? extends Writable> valueClass,
                                            final boolean isCompressed,
                                            final Properties tableProperties,
                                            final Progressable progress) throws IOException {

        LOG.info("Output going into " + fileOutputPath);

        FileSystem fs = fileOutputPath.getFileSystem(jc);
        FSDataOutputStream outFile = fs.create(fileOutputPath);

        FSDataOutputStream splitFile = null;
        if (MongoConfigUtil.getBSONOutputBuildSplits(jc)) {
            Path splitPath = new Path(fileOutputPath.getParent(), "." + fileOutputPath.getName() + ".splits");
            splitFile = fs.create(splitPath);
        }

        long splitSize = BSONSplitter.getSplitSize(jc, null);

        return new HiveBSONFileRecordWriter(outFile, splitFile, splitSize);
    }


    /**
     * A Hive Record Write that calls the BSON one
     */
    public class HiveBSONFileRecordWriter<K, V>
        extends BSONFileRecordWriter<K, V>
        implements RecordWriter {

        public HiveBSONFileRecordWriter(final FSDataOutputStream outFile, final FSDataOutputStream splitFile, final long splitSize) {
            super(outFile, splitFile, splitSize);
        }

        @Override
        public void close(final boolean toClose) throws IOException {
            super.close((TaskAttemptContext) null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write(final Writable value) throws IOException {
            super.write(null, (BSONWritable) value);
        }
    }
}
