package com.gtnewhorizon.gtnhlib.client.model.loading;

import com.github.bsideup.jabel.Desugar;
import org.joml.Vector3f;

@Desugar
public record ModelDisplay(Vector3f rotation, Vector3f translation, Vector3f scale) {

    public static final ModelDisplay DEFAULT = new ModelDisplay(
        new Vector3f(0, 0, 0),
        new Vector3f(0, 0, 0),
        new Vector3f(1, 1, 1));

    public enum Position {

        THIRDPERSON_RIGHTHAND,
        THIRDPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND,
        FIRSTPERSON_LEFTHAND,
        GUI, // inventory
        HEAD,
        GROUND, // dropped item I think
        FIXED; // item frames

        public static Position getByName(String name) {
            return switch (name) {
                case "thirdperson_righthand" -> THIRDPERSON_RIGHTHAND;
                case "thirdperson_lefthand" -> THIRDPERSON_LEFTHAND;
                case "firstperson_righthand" -> FIRSTPERSON_RIGHTHAND;
                case "firstperson_lefthand" -> FIRSTPERSON_LEFTHAND;
                case "gui" -> GUI;
                case "head" -> HEAD;
                case "ground" -> GROUND;
                case "fixed" -> FIXED;
                default -> null;
            };
        }
    }
}
