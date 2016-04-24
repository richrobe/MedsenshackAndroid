package de.medsenshack.data;

/**
 * Created by Jigoku969 on 24.04.2016.
 */
public enum ActivityClass {
    IDLE, SIT, STAND, WALK, STAIRS_UP, RUN;

    public String toString() {
        switch (this) {
            case IDLE:
                return "0";
            case SIT:
                return "1";
            case STAND:
                return "2";
            case WALK:
                return "3";
            case STAIRS_UP:
                return "4";
            case RUN:
                return "5";
            default:
                return "-1";
        }
    }
}
