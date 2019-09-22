package com.github.sailendra;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {
    private ConsumerDemoWithThread(){
    }

    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();
    }

    private void run(){
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class.getName());

        String topic = "first_topic";
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-consumer1-appln";

        CountDownLatch latch = new CountDownLatch(1);

        logger.info("creating the consumer thread");
        ConsumerRunnable myConsumerRunnable = new ConsumerRunnable(latch,bootstrapServers,groupId,topic);

        //start the thread
        Thread myConsumerThread = new Thread(myConsumerRunnable);
        myConsumerThread.start();

        //add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("caught shutdown hook");
            myConsumerRunnable.shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("application has exited");
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("application got interrupted",e);
        }finally{
            logger.info("application is closing");
        }
    }

    public class ConsumerRunnable implements Runnable{
        KafkaConsumer<String,String> consumer;
        private CountDownLatch latch;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        private ConsumerRunnable(CountDownLatch latch,String bootstrapServers, String groupId, String topic){
            this.latch = latch;

            //create consumer config
            Properties properties  =new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

            //create consumer
            consumer = new KafkaConsumer<>(properties);

            //subscribe to consumer
            consumer.subscribe(Collections.singleton(topic));
        }

        @Override
        public void run() {
            //poll for new data
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Key : " + record.key() + ", Value : " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            }catch(WakeupException e){
                logger.info("received shutdown signal!");
            }finally{
                consumer.close();
                //tell our main code we're done with the consumer
                latch.countDown();
            }
        }

        private void shutdown(){
        consumer.wakeup();
        }
    }
}
