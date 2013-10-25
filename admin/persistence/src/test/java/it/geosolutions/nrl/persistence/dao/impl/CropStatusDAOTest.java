/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.nrl.persistence.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import it.geosolutions.nrl.model.CropStatus;

import org.junit.Test;

/**
 *
 * @author adiaz
 */
public class CropStatusDAOTest extends BaseDAOTest {


    private static final String FACTOR = "factor0";

    public CropStatusDAOTest() {
    }

    @Test
    public void testRemove() {

        // @UniqueConstraint(columnNames = {"factor" , "district" , "province" , "year", "month", "dek"})})

         Long id;
         String crop = "crop";
         String factor = FACTOR;
         String month = "Oct";
         Integer dec = 1;

        {
        	CropStatus cs = new CropStatus();
        	cs.setCrop(crop);
        	cs.setDec(dec);
        	cs.setFactor(factor);
        	cs.setMonth(month);
        	cs.setMax(1.1);

            cropStatusDAO.persist(cs);
            id = cs.getRowId();
        }

        assertNotNull(id);
        LOGGER.info("Saved CropStatus " + id);;

        {
            // test remove
            try {
                cropStatusDAO.removeByPK(crop, month, factor, dec);
            } catch (Exception e) {
            	 fail("Unexpected exception : " + e.getMessage());
            }
        }
    }

}