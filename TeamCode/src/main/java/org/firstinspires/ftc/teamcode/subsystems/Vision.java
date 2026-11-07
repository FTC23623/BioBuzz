package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.objects.TagPosition;

/**
 * Something that sees AprilTags.
 *
 * <p>Every loop, {@link #Process()} looks at the newest picture and remembers the nearest
 * tag. Aim assist then asks three questions: is there a tag, which way is it, and how far.
 * Two cameras can answer them ({@link Vision_Limelight}, {@link Vision_Webcam}), and
 * {@link Vision_None} answers "no" when the robot has no camera. The drive never learns
 * which one it was handed. See docs/decisions/0011-vision-aim-assist.md.
 */
public interface Vision extends Subsystem {
    /** @return true when a tag was seen recently enough to trust */
    boolean SeesTag();

    /** @return the id of the nearest tag, or 0 when none is seen */
    int GetTagId();

    /**
     * @return degrees from straight ahead to the nearest tag. Left is positive, like yaw.
     *     0 when no tag is seen.
     */
    double GetBearing();

    /** @return inches from the camera to the nearest tag, or 0 when the camera can't tell */
    double GetRange();

    /**
     * @return inches from the camera, averaged over every tag in view from the nearest tag's
     *     group (our hive), so it doesn't jump when a different tag is the biggest. 0 when
     *     the camera can't tell. Used by steady range (docs/decisions/0048).
     */
    double GetGroupRange();

    /**
     * @return the nearest tag's tilt in the camera, degrees: its pitch, or its roll with
     *     {@code tagTiltFromRoll}. NaN when no tag is seen or the camera can't tell. Used to
     *     tell whether the hive has tipped up (docs/decisions/0049).
     */
    double GetTagTiltDegrees();

    /**
     * @return where the robot is from the tags alone: from the hive's nearest tag, and the
     *     Limelight's field position when it has the field map (docs/decisions/0054).
     *     {@link TagPosition#UNKNOWN} when no tag is seen or the camera can't tell.
     */
    TagPosition GetPosition();

    /**
     * @return degrees to the middle of the tag group the nearest tag is in (see
     *     {@code tagGroups} in BotProperties). Left is positive. 0 when no tag is seen.
     */
    double GetGroupBearing();

    /**
     * Which side of the hive the robot stands: degrees around the nearest tag from straight
     * in front of it. Left is positive, as the robot faces the hive. 0 when no tag is seen or
     * the camera can't tell. Aim uses it to pick which way to offset (docs/decisions/0044).
     */
    double GetSideDegrees();

    /** @return how many of our alliance's tags are in view right now */
    int CountTags();

    /**
     * @return how many tags are in view right now that are NOT ours (the other alliance's),
     *     so they were ignored. The indicator light blinks red when this is all the camera
     *     sees: the camera works, but the alliance is probably set wrong.
     */
    int CountIgnoredTags();
}
