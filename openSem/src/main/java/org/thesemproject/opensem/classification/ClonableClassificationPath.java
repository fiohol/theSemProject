/*
 * Copyright 2016 The Sem Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thesemproject.opensem.classification;

/**
 * ClassificationPath che gestisce il cloning
 *
 * @author The Sem Project
 */
public class ClonableClassificationPath extends ClassificationPath {

    /**
     * Costruttore standard
     * @param technology tecnologia di classificazione
     */
    public ClonableClassificationPath(String technology) {
        super(technology);
    }

    /**
     * Costruttore a partire da un classification path;
     * @param cp classificationPath
     */
    public ClonableClassificationPath(ClassificationPath cp) {
        super(cp.technology);
        this.path = cp.path;
        this.score = cp.score;
    }

    /**
     * Crea un classificationPath clone di quello contenuto nell'oggetto
     * @return clone
     */
    public ClassificationPath clone() {
        ClassificationPath cp = new ClassificationPath(technology);
        for (int i = 0; i < path.length; i++) {
            cp.path[i] = path[i];
            cp.score[i] = score[i];
        }
        return cp;
    }

}
