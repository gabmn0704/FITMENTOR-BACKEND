package com.fitmentor.domain.exercise;

/**
 * Posture rule used by the local pose analyzer: the angle formed at joint {@code vertex}
 * by the points {@code first}-{@code vertex}-{@code last} should lie in [min, max] degrees.
 *
 * @param correction message shown to the user when the angle is out of range.
 */
public record AngleRule(String first, String vertex, String last, double minDegrees, double maxDegrees, String correction) {
}
