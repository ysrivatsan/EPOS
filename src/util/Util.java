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
package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Peter
 */
public class Util {
    public static void clearDirectory(File dir) {
        if(dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) { //some JVMs return null for empty dirs
                for (File f : files) {
                    if (f.isDirectory()) {
                        clearDirectory(f);
                    }
                    f.delete();
                }
            }
        }
    }
    
    public static <T> List<T> repeat(int times, T item) {
        List<T> list = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            list.add(item);
        }
        return list;
    }

    public static String merge(List<String> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = list.iterator();
        String s = null;
        while (iter.hasNext() && (s = iter.next()) == null) {
        }
        if (s != null) {
            sb.append(s);
        }
        while (iter.hasNext()) {
            s = iter.next();
            if (s != null) {
                sb.append(' ');
                sb.append(s);
            }
        }
        return sb.toString();
    }
    
    public static List<String> trimSplit(String str, String regex) {
        return Arrays.asList(Arrays.stream(str.split(regex)).map(s -> s.trim()).toArray(n -> new String[n]));
    }
}
