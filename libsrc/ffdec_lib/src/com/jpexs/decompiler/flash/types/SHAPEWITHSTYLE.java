/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.types;

import com.jpexs.decompiler.flash.tags.base.NeedsCharacters;
import com.jpexs.decompiler.flash.types.shaperecords.EndShapeRecord;
import com.jpexs.decompiler.flash.types.shaperecords.SHAPERECORD;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author JPEXS
 */
public class SHAPEWITHSTYLE extends SHAPE implements NeedsCharacters, Serializable {

    public FILLSTYLEARRAY fillStyles;

    public LINESTYLEARRAY lineStyles;

    @Override
    public void getNeededCharacters(Set<Integer> needed) {
        fillStyles.getNeededCharacters(needed);
        lineStyles.getNeededCharacters(needed);
        for (SHAPERECORD r : shapeRecords) {
            r.getNeededCharacters(needed);
        }
    }

    @Override
    public boolean replaceCharacter(int oldCharacterId, int newCharacterId) {
        boolean modified = false;
        modified |= fillStyles.replaceCharacter(oldCharacterId, newCharacterId);
        modified |= lineStyles.replaceCharacter(oldCharacterId, newCharacterId);
        for (SHAPERECORD r : shapeRecords) {
            modified |= r.replaceCharacter(oldCharacterId, newCharacterId);
        }
        return modified;
    }

    @Override
    public boolean removeCharacter(int characterId) {
        boolean modified = false;
        modified |= fillStyles.removeCharacter(characterId);
        modified |= lineStyles.removeCharacter(characterId);
        for (SHAPERECORD r : shapeRecords) {
            modified |= r.removeCharacter(characterId);
        }
        return modified;
    }

    public static SHAPEWITHSTYLE createEmpty(int shapeNum) {
        SHAPEWITHSTYLE ret = new SHAPEWITHSTYLE();
        ret.shapeRecords = new ArrayList<>();
        ret.shapeRecords.add(new EndShapeRecord());
        ret.fillStyles = new FILLSTYLEARRAY();
        ret.fillStyles.fillStyles = new FILLSTYLE[0];
        ret.lineStyles = new LINESTYLEARRAY();
        if ((shapeNum == 1 || shapeNum == 2 || shapeNum == 3)) {
            ret.lineStyles.lineStyles = new LINESTYLE[0];
        } else if (shapeNum == 4) {
            ret.lineStyles.lineStyles = new LINESTYLE2[0];
        }

        return ret;
    }
}
