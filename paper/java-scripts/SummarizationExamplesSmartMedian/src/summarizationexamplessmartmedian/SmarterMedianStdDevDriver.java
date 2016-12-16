package summarizationexamplessmartmedian;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import utils.ConverterXmlToMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SortedMapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SmarterMedianStdDevDriver {

    public static class MedianStdDevMapper extends
            Mapper<Object, Text, IntWritable, SortedMapWritable> {

        private IntWritable commentLength = new IntWritable();
        private static final LongWritable ONE = new LongWritable(1);
        private IntWritable outHour = new IntWritable();

        private final static SimpleDateFormat frmt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS");

        @SuppressWarnings("deprecation")
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            Map<String, String> parsed = ConverterXmlToMap.transformXmlToMap(value
                    .toString());

            String strDate = parsed.get("CreationDate");

            String text = parsed.get("Text");

            if (strDate == null || text == null) {
                return;
            }

            try {
                Date creationDate = frmt.parse(strDate);
                outHour.set(creationDate.getHours());

                commentLength.set(text.length());
                SortedMapWritable outCommentLength = new SortedMapWritable();
                outCommentLength.put(commentLength, ONE);

                context.write(outHour, outCommentLength);

            } catch (ParseException e) {
                System.err.println(e.getMessage());
                return;
            }
        }
    }

    public static class MedianStdDevCombiner
            extends
            Reducer<IntWritable, SortedMapWritable, IntWritable, SortedMapWritable> {

        @SuppressWarnings("rawtypes")
        protected void reduce(IntWritable key,
                Iterable<SortedMapWritable> values, Context context)
                throws IOException, InterruptedException {

            SortedMapWritable outValue = new SortedMapWritable();

            for (SortedMapWritable v : values) {
                for (Entry<WritableComparable, Writable> entry : v.entrySet()) {
                    LongWritable count = (LongWritable) outValue.get(entry
                            .getKey());

                    if (count != null) {
                        count.set(count.get()
                                + ((LongWritable) entry.getValue()).get());
                    } else {
                        outValue.put(entry.getKey(), new LongWritable(
                                ((LongWritable) entry.getValue()).get()));
                    }
                }
            }

            context.write(key, outValue);
        }
    }

    public static class MedianStdDevReducer
            extends
            Reducer<IntWritable, SortedMapWritable, IntWritable, MedianStdDevTuple> {

        private MedianStdDevTuple result = new MedianStdDevTuple();
        private TreeMap<Integer, Long> commentLengthCounts = new TreeMap<Integer, Long>();

        @SuppressWarnings("rawtypes")
        @Override
        public void reduce(IntWritable key, Iterable<SortedMapWritable> values,
                Context context) throws IOException, InterruptedException {

            float sum = 0;
            long totalComments = 0;
            commentLengthCounts.clear();
            result.setMedian(0);
            result.setStdDev(0);

            for (SortedMapWritable v : values) {
                for (Entry<WritableComparable, Writable> entry : v.entrySet()) {
                    int length = ((IntWritable) entry.getKey()).get();
                    long count = ((LongWritable) entry.getValue()).get();

                    totalComments += count;
                    sum += length * count;

                    Long storedCount = commentLengthCounts.get(length);
                    if (storedCount == null) {
                        commentLengthCounts.put(length, count);
                    } else {
                        commentLengthCounts.put(length, storedCount + count);
                    }
                }
            }

            long medianIndex = totalComments / 2L;
            long previousComments = 0;
            long comments = 0;
            int prevKey = 0;
            for (Entry<Integer, Long> entry : commentLengthCounts.entrySet()) {
                comments = previousComments + entry.getValue();
                if (previousComments <= medianIndex && medianIndex < comments) {
                    if (totalComments % 2 == 0) {
                        if (previousComments == medianIndex) {
                            result.setMedian((float) (entry.getKey() + prevKey) 
                                    / 2.0f);
                        } else {
                            result.setMedian(entry.getKey());
                        }
                    } else {
                        result.setMedian(entry.getKey());
                    }
                    break;
                }
                previousComments = comments;
                prevKey = entry.getKey();
            }

            float mean = sum / totalComments;

            float sumOfSquares = 0.0f;
            for (Entry<Integer, Long> entry : commentLengthCounts.entrySet()) {
                sumOfSquares += (entry.getKey() - mean)
                        * (entry.getKey() - mean) * entry.getValue();
            }

            result.setStdDev((float) Math.sqrt(sumOfSquares
                    / (totalComments - 1)));

            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: MedianStdDevDriver <in> <out>");
            System.exit(2);
        }
        Job job = new Job(conf,
                "StackOverflow Comment Length Median StdDev By Hour");
        job.setJarByClass(SmarterMedianStdDevDriver.class);
        job.setMapperClass(MedianStdDevMapper.class);
        job.setCombinerClass(MedianStdDevCombiner.class);
        job.setReducerClass(MedianStdDevReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(SortedMapWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(MedianStdDevTuple.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class MedianStdDevTuple implements Writable {

        private float median = 0;
        private float stddev = 0f;

        public float getMedian() {
            return median;
        }

        public void setMedian(float median) {
            this.median = median;
        }

        public float getStdDev() {
            return stddev;
        }

        public void setStdDev(float stddev) {
            this.stddev = stddev;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            median = in.readFloat();
            stddev = in.readFloat();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeFloat(median);
            out.writeFloat(stddev);
        }

        @Override
        public String toString() {
            return median + "\t" + stddev;
        }
    }
}
