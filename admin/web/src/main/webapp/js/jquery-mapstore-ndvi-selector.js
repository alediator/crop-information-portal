/**
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

/**
 *  JQuery simple plugin to show a NDVI Selector like mapstore 
 *  @see NDVI_Selector on mapstore
 *
 *  @author Alejandro Diaz
 */
(function($, window) {
	/**
	 *	api: method[jqueryNDVISelector]
	 * 	Select combo with ndvi options
	 *  idYear ´´String´´ dialog to show with a fade in
	 *  idMonth ´´String´´ button to handle the dialog open
	 *  idDekad ´´String´´ button to handle the dialog close
	 **/
	$.fn.jqueryNDVISelector = function(idYear, idMonth, idDekad){

		// TODO: load from getCapabilities!!! Now we need to change with each ndvi update!!
		var dimensions = ["1998-04-11T02:00:00.000Z/1998-04-20T02:00:00.000Z/PT1S", "1998-04-21T02:00:00.000Z/1998-04-30T02:00:00.000Z/PT1S", "1998-05-01T02:00:00.000Z/1998-05-10T02:00:00.000Z/PT1S", "1998-06-11T02:00:00.000Z/1998-06-20T02:00:00.000Z/PT1S", "2000-01-11T01:00:00.000Z/2000-01-20T01:00:00.000Z/PT1S", "2004-01-01T01:00:00.000Z/2004-01-10T01:00:00.000Z/PT1S", "2008-02-01T01:00:00.000Z/2008-02-10T01:00:00.000Z/PT1S", "2010-03-21T01:00:00.000Z/2010-03-31T02:00:00.000Z/PT1S", "2013-01-01T00:00:00.000Z/2013-01-10T00:00:00.000Z/PT1S", "2013-01-11T01:00:00.000Z/2013-01-20T01:00:00.000Z/PT1S", "2013-01-21T01:00:00.000Z/2013-01-31T01:00:00.000Z/PT1S"];
		var monthShortNames = {01: 'Jan', 02: 'Feb', 03: 'Mar', 04: 'Apr', 05: 'May', 06: 'Jun', 07: 'Jul', 08: 'Aug', 09: 'Sep', 10: 'Oct', 11: 'Nov', 12: 'Dec'};
	    var dekadsNames = {0: 'First', 1: 'Second', 2: 'Third'};
		var thisValues = {};


		/**
	     * Load data from 
	     * thisValues:{
	     *      2013:{
	     *          11:[0] // first dekad available for Nov 2013
	     *          12:[0,1] // first and second dekad available for Dec 2013
	     *      }
	     *  }
	     into each select
	     */
	    function loadData(idSelect){
            var options=[];
    		var selected = false;
	    	if(idSelect == idYear) {
	    		// Year
	            for(var key in thisValues){
	            	var option = {text: key, value: key};
	            	if(!selected){
	            		option.selected = true;
	            		selected = true;
	            	}
	                options.push(option);
	            }
	    	}else if (idSelect == idMonth) {
	    		// Month
	    		var mapTarget = thisValues[$(idYear + " option:selected").val()];
	            for(var key in mapTarget){
	            	var text = monthShortNames[key];
	            	var option = {text: text, value: key};
	            	if(!selected){
	            		option.selected = true;
	            		selected = true;
	            	}
	                options.push(option);
	            }
	    	}else{
	    		// Dekad
	    		var arrayTarget = thisValues[$(idYear + " option:selected").val()][$(idMonth + " option:selected").val()];
	    		for(var i = 0; i < arrayTarget.length; i++){
	    			var key = arrayTarget[i];
	    			var text = dekadsNames[key];
	                var option = {text: text, value: (key + 1)};
	            	if(!selected){
	            		option.selected = true;
	            		selected = true;
	            	}
	                options.push(option);
	            }
	    	}
            $(idSelect).replaceOptions(options);
	    }

		 /**
	     * create a structure to mantain available dekads in thisValues
	     * thisValues:{
	     *      2013:{
	     *          11:[0] // first dekad available for Nov 2013
	     *          12:[0,1] // first and second dekad available for Dec 2013
	     *      }
	     *  }
	     * then updates the fildset
	     */
	    function loadDimensions(dimensions){
	    	if(dimensions){
	        	var values = dimensions;
	            thisValues= {};
	            var max="0";min = "9999";
	            for(var i=0;i<values.length;i++){
	                var year = parseInt(values[i].substring(0,4));
	                var month = parseInt(values[i].substring(5,7));
	                var dek = parseInt(values[i].substring(8,9));
	                var dateString = values[i].substring(0,10);
	                if(!thisValues[year]){
	                    thisValues[year]={};
	                }
	                 if(!thisValues[year][month]){
	                    thisValues[year][month]=[];
	                }
	                //add the dekad to the available list
	                thisValues[year][month].push(dek);
	                //set max and min years
	                max = dateString >max ?dateString:max;
	                min = dateString <min ?dateString:min;
	            }
	            //no year
	            if(thisValues=={}){
	                this.disableAll();
	                return;
	            }
	            loadData(idYear);
	            loadData(idMonth);
	            loadData(idDekad);
	    	}
		}

		$(idYear).change(function(){
			loadData(idMonth);
			loadData(idDekad);
		});

		$(idMonth).change(function(){
			loadData(idDekad);
		});

		loadDimensions(dimensions);
	};
})(jQuery, window);


(function($, window) {
	/**
	 *	api: method[replaceOptions]
	 * 	Simple replace options for a select
	 **/
	$.fn.replaceOptions = function(options) {
		var self, $option;

		this.empty();
		self = this;

		$.each(options, function(index, option) {
		  $option = $("<option></option>")
		    .attr("value", option.value)
		    .text(option.text);
		    if(option.selected){
		    	$option.selected("selected");
		    }
		  self.append($option);
		});
	};
})(jQuery, window);