package org.opentripplanner.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TimetableBuilder {

  private TripPattern pattern;
  private LocalDate serviceDate;
  private final Map<FeedScopedId, TripTimes> tripTimes = new HashMap<>();
  private final List<FrequencyEntry> frequencies = new ArrayList<>();

  TimetableBuilder() {}

  TimetableBuilder(Timetable tt) {
    pattern = tt.getPattern();
    serviceDate = tt.getServiceDate();
    frequencies.addAll(tt.getFrequencyEntries());
    addAllTripTimes(tt.getTripTimes());
  }

  public TimetableBuilder withTripPattern(TripPattern tripPattern) {
    this.pattern = tripPattern;
    return this;
  }

  public TimetableBuilder withServiceDate(LocalDate serviceDate) {
    this.serviceDate = serviceDate;
    return this;
  }

  /**
   * Add a new trip-times to the timetable. If the associated trip already exists, an exception is
   * thrown. This is considered a programming error. Use {@link #addOrUpdateTripTimes(TripTimes)}
   * if you want to replace an existing trip.
   */
  public TimetableBuilder addTripTimes(TripTimes tripTimes) {
    var trip = tripTimes.getTrip();
    if (this.tripTimes.containsKey(trip.getId())) {
      throw new IllegalStateException(
        "Error! TripTimes for the same trip is added twice. Trip: " + trip
      );
    }
    return addOrUpdateTripTimes(tripTimes);
  }

  /**
   * Add or update the trip-times. If the trip has an associated trip-times, then the trip-times
   * are replaced. If not, the trip-times it is added. Consider using
   * {@link #addTripTimes(TripTimes)}.
   */
  public TimetableBuilder addOrUpdateTripTimes(TripTimes tripTimes) {
    this.tripTimes.put(tripTimes.getTrip().getId(), tripTimes);
    return this;
  }

  public TimetableBuilder addAllTripTimes(List<TripTimes> tripTimes) {
    for (TripTimes it : tripTimes) {
      addTripTimes(it);
    }
    return this;
  }

  public TimetableBuilder removeTripTimes(TripTimes tripTimesToRemove) {
    tripTimes.remove(tripTimesToRemove.getTrip().getId());
    return this;
  }

  public TimetableBuilder removeAllTripTimes(Collection<TripTimes> tripTimesToBeRemoved) {
    for (TripTimes it : tripTimesToBeRemoved) {
      tripTimes.remove(it.getTrip().getId());
    }
    return this;
  }

  /**
   * Apply the same update to all trip-times including scheduled and frequency based
   * trip times.
   * <p>
   */
  public TimetableBuilder updateAllTripTimes(UnaryOperator<TripTimes> update) {
    tripTimes.replaceAll((t, tt) -> update.apply(tt));
    frequencies.replaceAll(it ->
      new FrequencyEntry(
        it.startTime,
        it.endTime,
        it.headway,
        it.exactTimes,
        update.apply(it.tripTimes)
      )
    );
    return this;
  }

  public TimetableBuilder addFrequencyEntry(FrequencyEntry frequencyEntry) {
    this.frequencies.add(frequencyEntry);
    return this;
  }

  public TripPattern getPattern() {
    return pattern;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  List<TripTimes> createImmutableOrderedListOfTripTimes() {
    return tripTimes.values().stream().sorted().toList();
  }

  public List<FrequencyEntry> getFrequencies() {
    return frequencies;
  }

  /**
   * The direction for all the trips in this timetable.
   */
  public Direction getDirection() {
    return Optional
      .ofNullable(getRepresentativeTripTimes())
      .map(TripTimes::getTrip)
      .map(Trip::getDirection)
      .orElse(Direction.UNKNOWN);
  }

  private TripTimes getRepresentativeTripTimes() {
    if (!tripTimes.isEmpty()) {
      return tripTimes.values().stream().findFirst().get();
    } else if (!frequencies.isEmpty()) {
      return frequencies.getFirst().tripTimes;
    } else {
      // Pattern is created only for real-time updates
      return null;
    }
  }

  public Timetable build() {
    return new Timetable(this);
  }
}
