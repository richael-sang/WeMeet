package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.dto.BookingDTO;
import com.onlinemeetingbookingsystem.dto.BookingStatisticsDTO;
import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.repository.BookingRepository;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import com.onlinemeetingbookingsystem.repository.UserRepository;
import com.onlinemeetingbookingsystem.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Override
    public List<BookingDTO> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<BookingDTO> dtoList = new ArrayList<>();

        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
            BookingDTO dto = new BookingDTO();

            dto.setRoomId(booking.getId());
            dto.setRoomName(booking.getMeetingRoom().getRoomName());
            dto.setReason(booking.getReason());
            dto.setStatus(booking.getStatus());
            dto.setCreatedAt(booking.getCreatedAt());
            dto.setUserSequenceNumber(i + 1);

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getBookingsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return bookingRepository.findByUser(user);
    }

    @Override
    public List<Booking> getBookingsByRoomId(Long roomId) {
        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Meeting room not found with id: " + roomId));
        return bookingRepository.findByMeetingRoom(meetingRoom);
    }

    @Override
    public List<Booking> getBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    @Override
    public List<Booking> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<Booking> getBookingsByRoomAndDateRange(Long roomId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findByRoomAndDateRange(roomId, startDate, endDate);
    }

    @Override
    public List<Booking> searchBookings(Long roomId, Booking.BookingStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> bookings = getAllBookings();

        if (roomId != null) {
            bookings = bookings.stream()
                    .filter(r -> r.getMeetingRoom().getId().equals(roomId))
                    .collect(Collectors.toList());
        }

        if (status != null) {
            bookings = bookings.stream()
                    .filter(r -> r.getStatus() == status)
                    .collect(Collectors.toList());
        }

        if (startDate != null) {
            bookings = bookings.stream()
                    .filter(r -> r.getStartTime().isAfter(startDate) || r.getStartTime().isEqual(startDate))
                    .collect(Collectors.toList());
        }

        if (endDate != null) {
            bookings = bookings.stream()
                    .filter(r -> r.getEndTime().isBefore(endDate) || r.getEndTime().isEqual(endDate))
                    .collect(Collectors.toList());
        }

        return bookings;
    }

    @Override
    public Map<String, Long> getBookingCountByStatus() {
        Map<String, Long> countByStatus = new HashMap<>();
        
        for (Booking.BookingStatus status : Booking.BookingStatus.values()) {
            Long count = (long) bookingRepository.findByStatus(status).size();
            countByStatus.put(status.name(), count);
        }
        
        return countByStatus;
    }

    @Override
    public Map<String, Long> getBookingCountByRoom() {
        Map<String, Long> countByRoom = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        for (MeetingRoom room : rooms) {
            Long count = (long) bookingRepository.findByMeetingRoom(room).size();
            countByRoom.put(room.getRoomName() + " - " + room.getLocation(), count);
        }
        
        return countByRoom;
    }

    @Override
    public Map<String, Double> getRoomUtilizationRates() {
        Map<String, Double> utilizationRates = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        
        for (MeetingRoom room : rooms) {
            List<Booking> approvedBookings = bookingRepository.findByMeetingRoomAndStatus(room, Booking.BookingStatus.APPROVED);
            
            approvedBookings = approvedBookings.stream()
                    .filter(r -> r.getStartTime().isAfter(oneMonthAgo) && r.getEndTime().isBefore(now))
                    .collect(Collectors.toList());
            
            long totalMinutesInMonth = ChronoUnit.MINUTES.between(oneMonthAgo, now);
            long totalReservedMinutes = 0;
            
            for (Booking booking : approvedBookings) {
                LocalDateTime bookingStart = booking.getStartTime().isBefore(oneMonthAgo) ? oneMonthAgo : booking.getStartTime();
                LocalDateTime bookingEnd = booking.getEndTime().isAfter(now) ? now : booking.getEndTime();
                totalReservedMinutes += ChronoUnit.MINUTES.between(bookingStart, bookingEnd);
            }
            
            double utilizationRate = (double) totalReservedMinutes / totalMinutesInMonth * 100;
            utilizationRates.put(room.getRoomName() + " - " + room.getLocation(), Math.min(utilizationRate, 100.0));
        }
        
        return utilizationRates;
    }

    @Override
    public Map<LocalDate, Long> getBookingCountByDate(LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDate, Long> countByDate = new HashMap<>();
        
        List<Booking> bookings = getBookingsByDateRange(startDate, endDate);
        
        for (Booking booking : bookings) {
            LocalDate date = booking.getStartTime().toLocalDate();
            countByDate.put(date, countByDate.getOrDefault(date, 0L) + 1);
        }
        
        return countByDate;
    }

    @Override
    public Map<String, Long> getBookingCountByHour(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> countByHour = new HashMap<>();
        
        List<Booking> bookings = getBookingsByDateRange(startDate, endDate);
        
        for (Booking booking : bookings) {
            int hour = booking.getStartTime().getHour();
            String hourRange = String.format("%02d:00-%02d:00", hour, (hour + 1) % 24);
            countByHour.put(hourRange, countByHour.getOrDefault(hourRange, 0L) + 1);
        }
        
        return countByHour;
    }

    @Override
    public Map<String, Double> getRoomOccupancyRatesByDay(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Double> occupancyRates = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        // 计算日期范围内的工作日数（假设一周7天，每天工作8小时：9:00-17:00）
        long totalWorkDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        
        for (MeetingRoom room : rooms) {
            List<Booking> approvedBookings = bookingRepository.findByMeetingRoomAndStatus(room, Booking.BookingStatus.APPROVED);
            
            // 过滤日期范围内的预订
            approvedBookings = approvedBookings.stream()
                    .filter(r -> r.getStartTime().isAfter(startDate) && r.getEndTime().isBefore(endDate))
                    .collect(Collectors.toList());
            
            // 计算工作时间内的总分钟数
            long totalWorkMinutes = totalWorkDays * 8 * 60; // 每天8小时，每小时60分钟
            long totalBookedMinutes = 0;
            
            for (Booking booking : approvedBookings) {
                // 只计算工作时间内的预订时间（9:00-17:00）
                LocalDateTime bookingStart = booking.getStartTime();
                LocalDateTime bookingEnd = booking.getEndTime();
                
                // 调整时间到工作时间范围内
                if (bookingStart.getHour() < 9) {
                    bookingStart = bookingStart.withHour(9).withMinute(0);
                }
                if (bookingEnd.getHour() >= 17) {
                    bookingEnd = bookingEnd.withHour(17).withMinute(0);
                }
                
                // 如果开始时间晚于结束时间，则跳过（非工作时间的预订）
                if (bookingStart.isBefore(bookingEnd)) {
                    totalBookedMinutes += ChronoUnit.MINUTES.between(bookingStart, bookingEnd);
                }
            }
            
            double occupancyRate = (double) totalBookedMinutes / totalWorkMinutes * 100;
            occupancyRates.put(room.getRoomName() + " - " + room.getLocation(), Math.min(occupancyRate, 100.0));
        }
        
        return occupancyRates;
    }

    @Override
    public Map<String, Long> getAverageBookingDurationByRoom() {
        Map<String, Long> avgDurations = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        for (MeetingRoom room : rooms) {
            List<Booking> bookings = bookingRepository.findByMeetingRoom(room);
            
            if (!bookings.isEmpty()) {
                long totalMinutes = 0;
                
                for (Booking booking : bookings) {
                    totalMinutes += ChronoUnit.MINUTES.between(booking.getStartTime(), booking.getEndTime());
                }
                
                long avgMinutes = totalMinutes / bookings.size();
                avgDurations.put(room.getRoomName() + " - " + room.getLocation(), avgMinutes);
            }
        }
        
        return avgDurations;
    }

    @Override
    public Map<String, Long> getMostUsedRooms(int limit) {
        Map<String, Long> roomUsage = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        // 收集所有房间的使用次数
        for (MeetingRoom room : rooms) {
            Long usageCount = (long) bookingRepository.findByMeetingRoom(room).size();
            roomUsage.put(room.getRoomName() + " - " + room.getLocation(), usageCount);
        }
        
        // 按使用次数排序并返回前N个
        return roomUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    public Long getAverageBookingsPerDay(LocalDateTime startDate, LocalDateTime endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        
        if (totalDays <= 0) {
            return 0L;
        }
        
        List<Booking> bookings = getBookingsByDateRange(startDate, endDate);
        return (long) Math.ceil((double) bookings.size() / totalDays);
    }

    @Override
    public LocalDate getMostActiveDay(LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDate, Long> countByDate = getBookingCountByDate(startDate, endDate);
        
        if (countByDate.isEmpty()) {
            return null;
        }
        
        return countByDate.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public Long getAverageBookingDuration() {
        List<Booking> bookings = getAllBookings();
        
        if (bookings.isEmpty()) {
            return 0L;
        }
        
        long totalMinutes = 0;
        
        for (Booking booking : bookings) {
            totalMinutes += ChronoUnit.MINUTES.between(booking.getStartTime(), booking.getEndTime());
        }
        
        return totalMinutes / bookings.size();
    }

    @Override
    public BookingStatisticsDTO getComprehensiveStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        BookingStatisticsDTO stats = new BookingStatisticsDTO();
        
        // 使用新的带日期范围的计数方法
        stats.setTotalBookings(bookingRepository.countByStartTimeBetween(startDate, endDate));
        stats.setApprovedBookings(bookingRepository.countByStatusAndStartTimeBetween(Booking.BookingStatus.APPROVED, startDate, endDate));
        stats.setRejectedBookings(bookingRepository.countByStatusAndStartTimeBetween(Booking.BookingStatus.REJECTED, startDate, endDate));

        // 其他统计数据（这些已经使用了日期范围）
        stats.setAverageBookingDuration(getAverageBookingDuration(startDate, endDate)); 
        stats.setAverageBookingsPerDay(getAverageBookingsPerDay(startDate, endDate));
        stats.setMostActiveDay(getMostActiveDay(startDate, endDate));
        stats.setMostActiveDayCount(0L); // 初始化，下面会计算
        stats.setBookingCountByStatus(getBookingCountByStatus(startDate, endDate)); // 需要修改或创建这个方法
        stats.setRoomUtilizationRates(getRoomUtilizationRates(startDate, endDate)); // 需要修改或创建这个方法
        stats.setBookingCountByDate(getBookingCountByDate(startDate, endDate));
        stats.setBookingCountByHour(getBookingCountByHour(startDate, endDate));
        stats.setMostUsedRooms(getMostUsedRooms(5, startDate, endDate)); // 假设限制为5，需要修改或创建这个方法
        stats.setAverageBookingDurationByRoom(getAverageBookingDurationByRoom(startDate, endDate)); // 需要修改或创建这个方法
        
        // 计算 mostActiveDayCount (如果 mostActiveDay 存在)
        if (stats.getMostActiveDay() != null) {
            Map<LocalDate, Long> countByDate = stats.getBookingCountByDate();
            stats.setMostActiveDayCount(countByDate.getOrDefault(stats.getMostActiveDay(), 0L));
        }
        
        return stats;
    }

    // --- Helper methods potentially needing date range parameters ---

    // 修改 getAverageBookingDuration 以接受并使用日期范围
    private Long getAverageBookingDuration(LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> bookings = bookingRepository.findByDateRange(startDate, endDate);
        if (bookings.isEmpty()) {
            return 0L;
        }
        long totalDurationMinutes = 0;
        for (Booking booking : bookings) {
            if (booking.getStartTime() != null && booking.getEndTime() != null) {
                totalDurationMinutes += Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            }
        }
        return totalDurationMinutes / bookings.size();
    }

    // 修改 getBookingCountByStatus 以接受并使用日期范围
    private Map<String, Long> getBookingCountByStatus(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> countByStatus = new HashMap<>();
        for (Booking.BookingStatus status : Booking.BookingStatus.values()) {
            long count = bookingRepository.countByStatusAndStartTimeBetween(status, startDate, endDate);
            countByStatus.put(status.name(), count);
        }
        return countByStatus;
    }
    
    // 修改 getRoomUtilizationRates 以接受并使用日期范围
    private Map<String, Double> getRoomUtilizationRates(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Double> utilizationRates = new HashMap<>();
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        List<Booking> bookingsInRange = bookingRepository.findByDateRange(startDate, endDate); // Fetch bookings in range first

        long totalMinutesInRange = ChronoUnit.MINUTES.between(startDate, endDate);
        if (totalMinutesInRange <= 0) return utilizationRates; // Avoid division by zero

        for (MeetingRoom room : rooms) {
            // 在内存中过滤特定房间和已批准状态的预订
            List<Booking> approvedBookingsForRoom = bookingsInRange.stream()
                .filter(b -> b.getMeetingRoom() != null && b.getMeetingRoom().getId().equals(room.getId()) && b.getStatus() == Booking.BookingStatus.APPROVED)
                .collect(Collectors.toList());
            
            long totalReservedMinutes = 0;
            for (Booking booking : approvedBookingsForRoom) { // Use the filtered list
                // 确保预订时间在查询范围内
                LocalDateTime bookingStart = booking.getStartTime().isBefore(startDate) ? startDate : booking.getStartTime();
                LocalDateTime bookingEnd = booking.getEndTime().isAfter(endDate) ? endDate : booking.getEndTime();
                if (bookingStart.isBefore(bookingEnd)) { // 确保开始时间在结束时间之前
                    totalReservedMinutes += ChronoUnit.MINUTES.between(bookingStart, bookingEnd);
                }
            }
            
            double utilizationRate = (double) totalReservedMinutes / totalMinutesInRange * 100;
            utilizationRates.put(room.getRoomName() + " - " + room.getLocation(), Math.min(utilizationRate, 100.0));
        }
        
        return utilizationRates;
    }

    // 修改 getMostUsedRooms 以接受并使用日期范围
    private Map<String, Long> getMostUsedRooms(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> bookings = bookingRepository.findByDateRange(startDate, endDate);
        
        Map<String, Long> roomCounts = bookings.stream()
                .filter(b -> b.getMeetingRoom() != null)
                .collect(Collectors.groupingBy(b -> b.getMeetingRoom().getRoomName() + " - " + b.getMeetingRoom().getLocation(), Collectors.counting()));
        
        return roomCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    // 修改 getAverageBookingDurationByRoom 以接受并使用日期范围
    private Map<String, Double> getAverageBookingDurationByRoom(LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> bookings = bookingRepository.findByDateRange(startDate, endDate);
        
        Map<String, List<Booking>> bookingsByRoom = bookings.stream()
                .filter(b -> b.getMeetingRoom() != null)
                .collect(Collectors.groupingBy(b -> b.getMeetingRoom().getRoomName() + " - " + b.getMeetingRoom().getLocation()));

        Map<String, Double> avgDurations = new HashMap<>();
        for (Map.Entry<String, List<Booking>> entry : bookingsByRoom.entrySet()) {
            List<Booking> roomBookings = entry.getValue();
            if (roomBookings.isEmpty()) continue;
            
            long totalDuration = 0;
            int count = 0;
            for (Booking b : roomBookings) {
                if (b.getStartTime() != null && b.getEndTime() != null) {
                    totalDuration += Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
                    count++;
                }
            }
            avgDurations.put(entry.getKey(), count > 0 ? (double) totalDuration / count : 0.0);
        }
        return avgDurations;
    }

    @Override
    public Booking updateBooking(Booking booking) {
        return bookingRepository.save(booking);
    }
} 