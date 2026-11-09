package com.ourvision.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name="meetings")
public class Meeting {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=200) private String title;
    @Column(name="meeting_date", nullable=false) private LocalDate meetingDate;
    @Column(name="meeting_time") private LocalTime meetingTime;
    @Column(length=255) private String venue;
    @Column(columnDefinition="text") private String description;
    public Meeting() {}
    public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public LocalDate getMeetingDate(){return meetingDate;} public void setMeetingDate(LocalDate v){meetingDate=v;}
    public LocalTime getMeetingTime(){return meetingTime;} public void setMeetingTime(LocalTime v){meetingTime=v;}
    public String getVenue(){return venue;} public void setVenue(String v){venue=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
