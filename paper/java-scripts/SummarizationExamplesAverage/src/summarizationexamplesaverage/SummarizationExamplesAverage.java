package summarizationexamplesaverage;

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
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SummarizationExamplesAverage {

    public static class AverageMapper extends
            Mapper<Object, Text, IntWritable, CountAverageTuple> {

        private IntWritable outHour = new IntWritable();
        private CountAverageTuple outCountAverage = new CountAverageTuple();

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

                outCountAverage.setCount(1);
                outCountAverage.setAverage(text.length());

                context.write(outHour, outCountAverage);

            } catch (ParseException e) {
                System.err.println(e.getMessage());
                return;
            }
        }
    }

    public static class AverageReducer
            extends
            Reducer<IntWritable, CountAverageTuple, IntWritable, CountAverageTuple> {

        private CountAverageTuple result = new CountAverageTuple();

        @Override
        public void reduce(IntWritable key, Iterable<CountAverageTuple> values,
                Context context) throws IOException, InterruptedException {

            float sum = 0;
            float count = 0;

            for (CountAverageTuple val : values) {
                sum += val.getCount() * val.getAverage();
                count += val.getCount();
            }

            result.setCount(count);
            result.setAverage(sum / count);

            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: AverageDriver <in> <out>");
            System.exit(2);
        }
        Job job = new Job(conf, "StackOverflow Average Comment Length");
        job.setJarByClass(SummarizationExamplesAverage.class);
        job.setMapperClass(AverageMapper.class);
        job.setCombinerClass(AverageReducer.class);
        job.setReducerClass(AverageReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(CountAverageTuple.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class CountAverageTuple implements Writable {

        private float count = 0f;
        private float average = 0f;

        public float getCount() {
            return count;
        }

        public void setCount(float count) {
            this.count = count;
        }

        public float getAverage() {
            return average;
        }

        public void setAverage(float average) {
            this.average = average;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            count = in.readFloat();
            average = in.readFloat();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeFloat(count);
            out.writeFloat(average);
        }

        @Override
        public String toString() {
            return count + "\t" + average;
        }
    }
}
