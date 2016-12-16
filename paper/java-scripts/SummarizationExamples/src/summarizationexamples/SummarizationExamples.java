/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package summarizationexamples;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import utils.ConverterXmlToMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 *
 * @author estacao_de_trabalho
 */
public class SummarizationExamples {

    /**
     * @param args the command line arguments
     */
    public static class MinMaxCountMapper extends
            Mapper<Object, Text, Text, MinMaxCountTuple> {

        private Text outUserId = new Text();
        private MinMaxCountTuple outTuple = new MinMaxCountTuple();

        private final static SimpleDateFormat frmt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS");

        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            Map<String, String> parsed = ConverterXmlToMap.transformXmlToMap(
                    value.toString());
            String strDate = parsed.get("CreationDate");
            String userId = parsed.get("UserId");

            if (strDate == null || userId == null) {
                return;
            }

            try {
                Date creationDate = frmt.parse(strDate);
                outTuple.setMin(creationDate);
                outTuple.setMax(creationDate);
                outTuple.setCount(1);
                outUserId.set(userId);
                context.write(outUserId, outTuple);
            } catch (ParseException e) {
                System.out.println("Ocorreu um erro ao converter a data de criação");
            }
        }
    }

    public static class MinMaxCountReducer extends
            Reducer<Text, MinMaxCountTuple, Text, MinMaxCountTuple> {

        private MinMaxCountTuple result = new MinMaxCountTuple();

        @Override
        public void reduce(Text key, Iterable<MinMaxCountTuple> values,
                Context context) throws IOException, InterruptedException {

            result.setMin(null);
            result.setMax(null);
            int sum = 0;

            for (MinMaxCountTuple val : values) {

                if (result.getMin() == null
                        || val.getMin().compareTo(result.getMin()) < 0) {
                    result.setMin(val.getMin());
                }

                if (result.getMax() == null
                        || val.getMax().compareTo(result.getMax()) > 0) {
                    result.setMax(val.getMax());
                }

                sum += val.getCount();
            }
            result.setCount(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: MinMaxCountDriver <in> <out>");
            System.exit(2);
        }
        Job job = new Job(conf, "StackOverflow Comment Date Min Max Count");
        job.setJarByClass(SummarizationExamples.class);
        job.setMapperClass(MinMaxCountMapper.class);
        job.setCombinerClass(MinMaxCountReducer.class);
        job.setReducerClass(MinMaxCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(MinMaxCountTuple.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class MinMaxCountTuple implements Writable {

        private Date min = new Date();
        private Date max = new Date();
        private long count = 0;

        private final static SimpleDateFormat frmt = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS");

        public Date getMin() {
            return min;
        }

        public void setMin(Date min) {
            this.min = min;
        }

        public Date getMax() {
            return max;
        }

        public void setMax(Date max) {
            this.max = max;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            min = new Date(in.readLong());
            max = new Date(in.readLong());
            count = in.readLong();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeLong(min.getTime());
            out.writeLong(max.getTime());
            out.writeLong(count);
        }

        @Override
        public String toString() {
            return frmt.format(min) + "\t" + frmt.format(max) + "\t" + count;
        }
    }

}
