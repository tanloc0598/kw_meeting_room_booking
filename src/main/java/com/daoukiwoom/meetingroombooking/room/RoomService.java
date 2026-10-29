package com.daoukiwoom.meetingroombooking.room;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daoukiwoom.meetingroombooking.common.exception.ErrorCode;
import com.daoukiwoom.meetingroombooking.common.exception.KwException;
import com.daoukiwoom.meetingroombooking.room.dto.CreateRoomRequest;
import com.daoukiwoom.meetingroombooking.room.dto.RoomResponse;
import com.daoukiwoom.meetingroombooking.room.dto.UpdateRoomRequest;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponse> findAll(RoomActiveStatus activeStatus, Integer minCapacity, String location,
                                      boolean isAdmin) {
        if (activeStatus != RoomActiveStatus.ACTIVE && !isAdmin) {
            throw new AccessDeniedException("Only admin can list inactive rooms");
        }

        Specification<Room> spec = Specification.allOf(
                RoomSpecs.hasActive(activeStatus.getValue()),
                RoomSpecs.capacityAtLeast(minCapacity),
                RoomSpecs.locationContains(location));

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

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new KwException(ErrorCode.ROOM_NOT_FOUND, "Room " + id + " not found"));
    }
}
