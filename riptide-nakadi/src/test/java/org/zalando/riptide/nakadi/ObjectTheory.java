package org.zalando.riptide.nakadi;

/*
 * ⁣​
 * Riptide: Nakadi
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
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
 * ​⁣
 */

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@Ignore
@RunWith(Theories.class)
public abstract class ObjectTheory {

    @Theory
    public void equalsOnSame(Object x, Object y) {
        assumeThat(x, is(not(equalTo(null))));
        assumeThat(x == y, is(true));
        assertThat(x.equals(y), is(true));
    }

    @Theory
    public void equalsIsReflexive(Object x) {
        assumeThat(x, is(not(equalTo(null))));
        assertThat(x.equals(x), is(true));
    }

    @Theory
    public void equalsIsSymmetric(Object x, Object y) {
        assumeThat(x, is(not(equalTo(null))));
        assumeThat(y, is(not(equalTo(null))));
        assumeThat(y.equals(x), is(true));
        assertThat(x.equals(y), is(true));
    }

    @Theory
    public void equalsIsTransitive(Object x, Object y, Object z) {
        assumeThat(x, is(not(equalTo(null))));
        assumeThat(y, is(not(equalTo(null))));
        assumeThat(z, is(not(equalTo(null))));
        assumeThat(x.equals(y) && y.equals(z), is(true));
        assertThat(z.equals(x), is(true));
    }

    @Theory
    public void equalsIsFalseOnNull(Object x) {
        assumeThat(x, is(not(equalTo(null))));
        assertThat(x.equals(null), is(false));
    }

    @Theory
    public void isConsistent(Object x, Object y) {
        assumeThat(x, is(not(equalTo(null))));
        assumeThat(y, is(not(equalTo(null))));
        int a = x.hashCode(), b = y.hashCode();
        assumeThat(x.equals(y), is(true));
        assertThat(a, is(equalTo(b)));
    }

    @Theory
    public void staysSame(Object x, Object y) {
        assumeThat(x, is(not(equalTo(null))));
        boolean equals = x.equals(y);
        int hashCode = x.hashCode();
        String string = x.toString();

        for (int i = 0; i < 10; i++) {
            assertThat(x.equals(y), is(equals));
            assertThat(x.hashCode(), is(hashCode));
            assertThat(x.toString(), is(string));
        }
    }
}
