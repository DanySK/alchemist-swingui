/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.l10n;

/**
 */
// CHECKSTYLE:OFF
public enum Res {

    ADD_EFFECT, ALCHEMIST, ALCHEMIST_XML, ASPECT_RATIO, ATTACH_MONITOR, BLUE, BUILD_FILL_OVAL, BUILD_DRAW_CIRCLE, BUILD_DRAW_IMAGE, CANNOT_LOAD_STORE_OPTIONS, CHANGE_RANDOM_SEED, COLOR, COLOR_FUNCTION, CONTROLS, DONE, DETACH_MONITOR, EFFECT, EFFECT_BUILDER, EFFECTS_GROUP, EFFECT_TAB, ENABLE_DRAW_LINKS, FILE, FILE_NOT_VALID, FILE_PROCESSED, FLOW_TAB, GREEN, HEIGHT, HOME_TAB, IS_NOT_AN_INTEGER, IS_NOT_INITIALIZED_YET, JAR_FILE, JAR_LOAD_SUCCESSFULL, LOAD, LOAD_PARALLEL, LOAD_SINGLE, LOAD_JAR, MAX_REACTIVITY, MONITORS, NEAREST_NODE_IS, NO_DESCRIPTION_AVAILABLE, OPEN, OPEN_PERSPECTIVE, OUTPUT_MONITORS, PAUSE, PLAY, PROCESS_FILE, QUIT, RANDOM, RANDOM_REINIT_FAIL, RANDOM_REINIT_SUCCESS, REACTIVITY, READY_TO_PROCESS, REAL_TIME, RED, REMOVE_EFFECT, RGB, PERSPECTIVES, PERSPECTIVE, SAVE, SHOW_GROUP, SIZE, START, STEP, STOP, SWITCH_TO_PARALLEL, SWITCH_TO_SINGLE, TIME, USE_MOLECULE, USER_SELECTED, WARNING, WIDTH;

    private final static String english(final Res r) {
        switch (r) {
        case ADD_EFFECT:
            return "Add new effect";
        case ALCHEMIST:
            return "Alchemist";
        case ALCHEMIST_XML:
            return "Alchemist XML";
        case ASPECT_RATIO:
            return "Aspect ratio";
        case ATTACH_MONITOR:
            return "Attach monitor";
        case BLUE:
            return "Blue";
        case BUILD_FILL_OVAL:
            return "Build new fill oval effect";
        case BUILD_DRAW_CIRCLE:
            return "Build new draw circle effect";
        case BUILD_DRAW_IMAGE:
            return "Build new draw image effect";
        case CANNOT_LOAD_STORE_OPTIONS:
            return "The system was unable to load or save the options. Rollbacking to default.";
        case CHANGE_RANDOM_SEED:
            return "Change random seed";
        case COLOR:
            return "Color";
        case COLOR_FUNCTION:
            return "Color function";
        case CONTROLS:
            return "Controls";
        case DONE:
            return "Done";
        case DETACH_MONITOR:
            return "Detach monitor";
        case EFFECT:
            return "Effect";
        case EFFECT_BUILDER:
            return "Effect builder";
        case EFFECTS_GROUP:
            return "Effects";
        case EFFECT_TAB:
            return "View";
        case ENABLE_DRAW_LINKS:
            return "Draw links";
        case FILE:
            return "File";
        case FILE_NOT_VALID:
            return "Invalid file";
        case FILE_PROCESSED:
            return "File processed";
        case FLOW_TAB:
            return "Flow control";
        case GREEN:
            return "Green";
        case HEIGHT:
            return "Image Height";
        case HOME_TAB:
            return "Home";
        case IS_NOT_AN_INTEGER:
            return "is not an Integer, or is out of admissible range";
        case IS_NOT_INITIALIZED_YET:
            return "is not initialized yet";
        case JAR_FILE:
            return "JAR File";
        case JAR_LOAD_SUCCESSFULL:
            return "has been successfully included into the current ClassPath";
        case LOAD:
            return "Load";
        case LOAD_PARALLEL:
            return "Load (parallel)";
        case LOAD_SINGLE:
            return "Load (single thread)";
        case LOAD_JAR:
            return "Load JAR File";
        case MAX_REACTIVITY:
            return "Max Reactivity";
        case MONITORS:
            return "Monitors";
        case NEAREST_NODE_IS:
            return "Nearest node is";
        case NO_DESCRIPTION_AVAILABLE:
            return "No description available";
        case OPEN:
            return "Open...";
        case OPEN_PERSPECTIVE:
            return "Open new perspective";
        case OUTPUT_MONITORS:
            return "Output Monitors";
        case PAUSE:
            return "Pause";
        case PLAY:
            return "Play";
        case PROCESS_FILE:
            return "Process File";
        case QUIT:
            return "Quit";
        case RANDOM:
            return "Random";
        case RANDOM_REINIT_FAIL:
            return "Unable to re-initialize the RandomEngine";
        case RANDOM_REINIT_SUCCESS:
            return "RandomEngine successfully re-initialized";
        case REACTIVITY:
            return "Reactivity";
        case READY_TO_PROCESS:
            return "Ready to process";
        case REAL_TIME:
            return "Real Time";
        case RED:
            return "Red";
        case REMOVE_EFFECT:
            return "Remove selected";
        case RGB:
            return "RGB";
        case PERSPECTIVES:
            return "Working Perspectives";
        case PERSPECTIVE:
            return "Perspective";
        case SAVE:
            return "Save";
        case SHOW_GROUP:
            return "Show";
        case SIZE:
            return "Size";
        case START:
            return "Start";
        case STEP:
            return "Run single step";
        case STOP:
            return "Stop";
        case SWITCH_TO_PARALLEL:
            return "Enable multithreaded simulation, no reproducibility guaranteed (experimental feature)";
        case SWITCH_TO_SINGLE:
            return "Disable multithreading, reproducibility guaranteed";
        case TIME:
            return "Time";
        case USE_MOLECULE:
            return "Draw only nodes containing a molecule";
        case USER_SELECTED:
            return "User selected";
        case WARNING:
            return "Warning";
        case WIDTH:
            return "Image Width";
        default:
            return "IMPLEMENT ME, FOOL!";
        }
    }

    public static String get(final Res r) {
        return english(r);
    }

//    public static String get(final Res r, final Language l) {
//        final String res = lang(r, l);
//        return res == null ? english(r) : res;
//    }

//    private final static String italian(final Res r) {
//        switch (r) {
//        case WARNING:
//            return "Attenzione";
//        case ADD_EFFECT:
//            return "Aggiungi nuovo effetto";
//        case ALCHEMIST:
//            return "Alchemist";
//        default:
//            return "IMPLEMENTAMI, CRETINO!";
//        }
//    }

//    private static String lang(final Res r, final Language l) {
//        switch (l) {
//        case ENGLISH:
//            return english(r);
//        case ITALIAN:
//            return italian(r);
//        default:
//            return english(r);
//        }
//    }

//    public static void setLanguage(final Language l) {
//        curLang = l;
//    }

}
