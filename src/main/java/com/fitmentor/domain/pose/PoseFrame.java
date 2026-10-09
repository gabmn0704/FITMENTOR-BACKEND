package com.fitmentor.domain.pose;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PoseFrame(
	List<PosePoint> points,
	String exercise,
	@JsonProperty("frame_width") int frameWidth,
	@JsonProperty("frame_height") int frameHeight
) {
	public PoseFrame {
		frameWidth = frameWidth > 0 ? frameWidth : 16;
		frameHeight = frameHeight > 0 ? frameHeight : 9;
	}

	public PoseFrame(List<PosePoint> points, String exercise) {
		this(points, exercise, 16, 9);
	}
}
