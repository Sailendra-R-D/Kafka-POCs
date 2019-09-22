package com.github.sailendra;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerCallbackDemo {
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092";

    public static void main(String[] args) {
        final Logger logger = LoggerFactory.getLogger(ProducerCallbackDemo.class);

        //create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //create producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for (int i = 1; i < 20; i++) {
            //create a producer record
            final ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", String.format("hello world_%s",i));
            //send data - asynchronous
            producer.send(record, (RecordMetadata recordMetadata, Exception e) ->{
                    //here executes everytime
                    if (e == null) {
                        //record sent successfully
                        logger.info("\nReceived new metadata, \n" +
                                "Topic : " + recordMetadata.topic() + " \n" +
                                "Partition : " + recordMetadata.partition() + " \n" +
                                "Offset : " + recordMetadata.offset() + " \n" +
                                "Timestamp : " + recordMetadata.timestamp()
                        );
                    } else {
                        logger.error("error while producing ", e);
                    }
                }
            );
        }
        //flush data
        producer.flush();

        //flush and close producer
        producer.close();
    }
}
