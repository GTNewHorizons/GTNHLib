package com.gtnewhorizon.gtnhlib.transformation;

import lombok.Getter;

/**
 * Describes whether a given transformation request is to transform, untransform, or to toggle a transformation.
 */
public enum TransformationAction {

    /**
     * Transforms to this form. Does nothing if the player is already this form.
     */
    TRANSFORM(1),
    /**
     * Untransforms the player, no matter what transformation they have currently.
     */
    UNTRANSFORM(2),
    /**
     * If the player has this form already, untransform. If they have any other form, transform to this form.
     */
    TOGGLE(3),
    //
    ;

    @Getter
    private final int id;

    TransformationAction(final int id) {
        this.id = id;
    }

    public static TransformationAction getById(final int id) {
        for (final TransformationAction type : values()) {
            if (type.id == id) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Could not find direction %d", id));
    }
}
