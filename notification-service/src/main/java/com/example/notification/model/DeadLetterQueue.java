package com.example.notification.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dead_letter_queue")
public class DeadLetterQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String msgTopic;
    private Integer msgPartition;
    private Long msgOffset;
    private String msgKey;
    private String errorMessage;

    @Override
    public String toString() {
        return String.format("[%d] (%s, %d, %d) (%s)", id, msgTopic, msgPartition, msgOffset, msgKey);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMsgTopic() {
        return msgTopic;
    }

    public void setMsgTopic(String msgTopic) {
        this.msgTopic = msgTopic;
    }

    public Integer getMsgPartition() {
        return msgPartition;
    }

    public void setMsgPartition(Integer msgPartition) {
        this.msgPartition = msgPartition;
    }

    public Long getMsgOffset() {
        return msgOffset;
    }

    public void setMsgOffset(Long msgOffset) {
        this.msgOffset = msgOffset;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public void setMsgKey(String msgKey) {
        this.msgKey = msgKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
