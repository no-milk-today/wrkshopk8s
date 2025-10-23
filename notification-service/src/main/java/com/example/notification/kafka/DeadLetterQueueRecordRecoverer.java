package com.example.notification.kafka;

import com.example.notification.model.DeadLetterQueue;
import com.example.notification.repository.DeadLetterQueueRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterQueueRecordRecoverer implements ConsumerRecordRecoverer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueRecordRecoverer.class);

    private final DeadLetterQueueRepository repository;

    public DeadLetterQueueRecordRecoverer(DeadLetterQueueRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception e) {
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        deadLetterQueue.setMsgTopic(record.topic());
        deadLetterQueue.setMsgPartition(record.partition());
        deadLetterQueue.setMsgOffset(record.offset());
        if (record.key() instanceof String key) {
            deadLetterQueue.setMsgKey(key);
        }
        deadLetterQueue.setErrorMessage(e.getMessage());

        repository.save(deadLetterQueue);
        log.info("Message {} saved to DLQ", record.key());
    }
}
