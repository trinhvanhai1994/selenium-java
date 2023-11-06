/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.api.problems;

import org.gradle.api.Incubating;

/**
 * Specifies the main categories of problems.
 *
 * @since 8.4
 */
@Incubating
public class ProblemGroup {

    public static final String GENERIC_ID = "generic";
    public static final String DEPRECATION_ID = "deprecation";
    public static final String VERSION_CATALOG_ID = "version_catalog";
    public static final String TYPE_VALIDATION_ID = "type_validation";

    private String id;

    public ProblemGroup(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
