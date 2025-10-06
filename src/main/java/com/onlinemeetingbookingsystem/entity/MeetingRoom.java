package com.onlinemeetingbookingsystem.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_name", nullable = false, unique = true, length = 100)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoomType type = RoomType.LARGE_CLASSROOM;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "location", length = 255)
    private String location;
    
    @Column(name = "floor")
    private Integer floor;

    @Column(name = "has_projector")
    private boolean hasProjector;

    @Column(name = "has_screen")
    private boolean hasScreen;

    @Column(name = "has_speaker")
    private boolean hasSpeaker;
    
    @Column(name = "has_computer")
    private boolean hasComputer;
    
    @Column(name = "has_whiteboard")
    private boolean hasWhiteboard;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Setter
    @Getter
    @Column(name = "available", nullable = false)
    private boolean available = true;

    @OneToMany(mappedBy = "meetingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RoomType {
        COMPUTER_LAB, LARGE_CLASSROOM, SMALL_CLASSROOM
    }
} 