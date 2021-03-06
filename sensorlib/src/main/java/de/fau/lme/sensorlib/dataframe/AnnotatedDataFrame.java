/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.sensorlib.dataframe;

/**
 * Created by gradl on 08.10.2015.
 */
public interface AnnotatedDataFrame {
    char getAnnotationChar();

    String getAnnotationString();

    Object getAnnotation();
}
