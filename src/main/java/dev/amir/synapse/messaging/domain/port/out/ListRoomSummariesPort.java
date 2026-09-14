package dev.amir.synapse.messaging.domain.port.out;

@FunctionalInterface
public interface ListRoomSummariesPort {
  RoomSummaryPage findRoomSummaries(RoomSummarySearchCriteria criteria);
}
