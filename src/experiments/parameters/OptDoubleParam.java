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

/**
 *
 * @author Peter
 */
public class OptDoubleParam implements Param<Double> {

    @Override
    public boolean isValid(String param) {
        if(param == null || param.isEmpty()) {
            return true;
        }
        try {
            double d = Double.parseDouble(param);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String validDescription() {
        return "any double or null";
    }

    @Override
    public Double get(String param) {
        if(param == null || param.isEmpty()) {
            return null;
        }
        return Double.parseDouble(param);
    }
    
}
