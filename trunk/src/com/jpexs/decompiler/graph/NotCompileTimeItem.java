/*
 * Copyright (C) 2013 JPEXS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.graph;

import java.util.List;

/**
 *
 * @author JPEXS
 */
public class NotCompileTimeItem extends GraphTargetItem {

    public GraphTargetItem object;

    public NotCompileTimeItem(GraphSourceItem instruction, GraphTargetItem object) {
        super(instruction, NOPRECEDENCE);
        this.object = object;
    }

    @Override
    public boolean isCompileTime() {
        return false;
    }

    @Override
    public GraphTargetItem getThroughNotCompilable() {
        if (object == null) {
            return object;
        }
        return object.getThroughNotCompilable();
    }

    @Override
    public String toString(boolean highlight, List<Object> localData) {
        return object.toString(highlight, localData);
    }

    @Override
    public boolean hasReturnValue() {
        return object.hasReturnValue();
    }
}