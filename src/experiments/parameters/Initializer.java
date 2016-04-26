/*
 * Copyright (C) 2016 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package experiments.parameters;

import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Peter
 */
public class Initializer<T> {

    public final Param<T> param;
    public final Consumer<T> setter;
    public final Integer lazyPriority;

    public Initializer(Consumer<T> setter, Param<T> param, Integer lazyPriority) {
        this.param = param;
        this.setter = setter;
        this.lazyPriority = lazyPriority;
    }

    public void init(String name, String value, LazyMap lazyMap) {
        if (!param.isValid(value)) {
            throw new IllegalArgumentException(value + " is not valid for " + name + "; valid: " + param.validDescription());
        } else if (lazyPriority != null && lazyMap != null) {
            lazyMap.put(name, e -> setter.accept(param.get(value)), lazyPriority);
        } else {
            setter.accept(param.get(value));
        }
    }
}
