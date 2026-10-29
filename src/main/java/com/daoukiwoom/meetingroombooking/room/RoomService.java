package com.daoukiwoom.meetingroombooking.room;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daoukiwoom.meetingroombooking.booking.BookingRepository;
import com.daoukiwoom.meetingroombooking.booking.BookingStatus;
import com.daoukiwoom.meetingroombooking.common.exception.ErrorCode;
import com.daoukiwoom.meetingroombooking.common.exception.KwException;
import com.daoukiwoom.meetingroombooking.room.dto.CreateRoomRequest;
import com.daoukiwoom.meetingroombooking.room.dto.RoomResponse;
import com.daoukiwoom.meetingroombooking.room.dto.UpdateRoomRequest;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<RoomResponse> findAll(RoomActiveStatus activeStatus, Integer minCapacity, String location,
                                      Instant startAt, Instant endAt, boolean isAdmin) {
        if (activeStatus != RoomActiveStatus.ACTIVE && !isAdmin) {
            throw new AccessDeniedException("Only admin can list inactive rooms");
        }
        validateTimeRange(activeStatus, startAt, endAt);

        Specification<Room> spec = Specification.allOf(
                RoomSpecs.hasActive(activeStatus.getValue()),
                RoomSpecs.capacityAtLeast(minCapacity),
                RoomSpecs.locationContains(location),
                RoomSpecs.availableBetween(startAt, endAt));

        List<Room> rooms = roomRepository.findAll(spec, Sort.by("code"));
        return rooms.stream().map(RoomResponse::from).toList();
    }

    public RoomResponse getById(Long id) {
        return RoomResponse.from(findRoom(id));
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        if (roomRepository.existsByCode(request.code())) {
            throw new KwException(ErrorCode.ROOM_CODE_DUPLICATED,
                    "Room code '" + request.code() + "' already exists");
        }

        Room room = new Room(request.name(), request.code(), request.location(), request.capacity());
        return RoomResponse.from(roomRepository.save(room));
    }

    /** Không cần save(): entity đang được Hibernate theo dõi, commit sẽ tự UPDATE (dirty checking). */
    @Transactional
    public RoomResponse update(Long id, UpdateRoomRequest request) {
        Room room = findRoom(id);
        room.update(request.name(), request.location(), request.capacity());
        return RoomResponse.from(room);
    }

    /** BR10: không vô hiệu hoá phòng còn booking CONFIRMED chưa kết thúc (kể cả đang diễn ra). */
    @Transactional
    public RoomResponse updateStatus(Long id, boolean active) {
        Room room = findRoom(id);
        if (active) {
            room.activate();
        } else if (room.isActive()) {
            if (bookingRepository.existsByRoomIdAndStatusAndEndAtAfter(id, BookingStatus.CONFIRMED, Instant.now())) {
                throw new KwException(ErrorCode.ROOM_HAS_FUTURE_BOOKINGS,
                        "Room " + id + " still has upcoming bookings; cancel them first");
            }
            room.deactivate();
        }
        return RoomResponse.from(room);
    }

    /** startAt/endAt đi theo cặp; tìm phòng trống chỉ có nghĩa với phòng đang hoạt động. */
    private void validateTimeRange(RoomActiveStatus activeStatus, Instant startAt, Instant endAt) {
        if (startAt == null && endAt == null) {
            return;
        }
        if (startAt == null || endAt == null) {
            throw new KwException(ErrorCode.INVALID_SEARCH_PARAMS, "startAt and endAt must be provided together");
        }
        if (!endAt.isAfter(startAt)) {
            throw new KwException(ErrorCode.INVALID_SEARCH_PARAMS, "endAt must be after startAt");
        }
        if (activeStatus != RoomActiveStatus.ACTIVE) {
            throw new KwException(ErrorCode.INVALID_SEARCH_PARAMS, "Availability search only applies to active rooms");
        }
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new KwException(ErrorCode.ROOM_NOT_FOUND, "Room " + id + " not found"));
    }
}
