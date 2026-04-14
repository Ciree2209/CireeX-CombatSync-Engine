package com.cireex.combatsync.lag;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling buffer of position snapshots — last 12 ticks (~600ms).
 */
public class SnapshotBuffer {

    private static final int MAX_SNAPSHOTS = 12;

    private final Deque<PositionSnapshot> snapshots = new ArrayDeque<>();

    public void addSnapshot(PositionSnapshot snapshot) {
        snapshots.addFirst(snapshot);

        // keep buffer small (AC-safe)
        while (snapshots.size() > MAX_SNAPSHOTS) {
            snapshots.removeLast();
        }
    }

    /** Returns the snapshot closest to targetTime, or null if buffer is empty. */
    public PositionSnapshot getSnapshotAt(long targetTime) {
        if (snapshots.isEmpty()) {
            return null;
        }

        PositionSnapshot closest = null;
        long closestDiff = Long.MAX_VALUE;

        for (PositionSnapshot snap : snapshots) {
            long diff = Math.abs(snap.getTime() - targetTime);
            if (diff < closestDiff) {
                closestDiff = diff;
                closest = snap;
            }

            // early exit once we've passed the target time
            if (snap.getTime() < targetTime) {
                break;
            }
        }

        return closest != null ? closest : snapshots.peekFirst();
    }

    public PositionSnapshot getLatest() {
        return snapshots.peekFirst();
    }

    public PositionSnapshot getOldest() {
        return snapshots.peekLast();
    }

    public int size() {
        return snapshots.size();
    }

    public boolean isEmpty() {
        return snapshots.isEmpty();
    }

    public void clear() {
        snapshots.clear();
    }

    public long getTimeRange() {
        if (snapshots.size() < 2) {
            return 0;
        }
        return snapshots.peekFirst().getTime() - snapshots.peekLast().getTime();
    }
}
