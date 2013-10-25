/*
 *  nrl Crop Information Portal
 *  https://github.com/geosolutions-it/crop-information-portal
 *  Copyright (C) 2007-2013 GeoSolutions S.A.S.
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
package it.geosolutions.nrl.persistence.dao;

import it.geosolutions.nrl.model.CropStatus;

import com.googlecode.genericdao.dao.jpa.GenericDAO;

/**
 * 
 * @author adiaz
 */
public interface CropStatusDAO extends GenericDAO<CropStatus, Long> {

	/**
	 * Remove a CropStatus record by id
	 * 
	 * @param crop
	 * @param month
	 * @param factor
	 * @param dec
	 * 
	 * @return
	 */
	public boolean removeByPK(String crop, String month, String factor,
			Integer dec);
}
