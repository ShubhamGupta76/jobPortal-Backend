package com.job_Portal_Backend.job_portal_backend.messaging.entity;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_read_user", columnNames = {"message_id", "user_id"})
})
@Getter
@Setter
public class MessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime readAt;
}