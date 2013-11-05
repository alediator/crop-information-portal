/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.geo-solutions.it/
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.nrl.ndvi;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.annotations.Action;
import it.geosolutions.geobatch.annotations.CheckConfiguration;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.zonal.ZonalStatsDescriptor;
import it.geosolutions.jaiext.zonal.ZonalStatsRIF;
import it.geosolutions.jaiext.zonal.ZoneGeometry;
import it.geosolutions.nrl.dto.StatsBean;
import it.geosolutions.nrl.dto.StatsBean.CLASSIFIER_TYPE;
import it.geosolutions.nrl.dto.StatsBean.MASK_TYPE;
import it.geosolutions.tools.commons.writer.CSVWriter;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.xml.bind.JAXB;

import org.apache.commons.collections.set.ListOrderedSet;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.utils.FeatureAggregation;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.image.jai.Registry;
import org.geotools.process.vector.CollectGeometries;
import org.geotools.process.vector.ReprojectProcess;
import org.geotools.referencing.CRS;
import org.geotools.resources.image.ImageUtilities;
import org.jaitools.imageutils.ROIGeometry;
import org.jaitools.media.jai.vectorbinarize.VectorBinarizeDescriptor;
import org.jaitools.media.jai.vectorbinarize.VectorBinarizeRIF;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Generate a NDVI CSV file
 * 
 * @author adiaz
 */
@Action(configurationClass = NDVIStatsConfiguration.class)
public class NDVIStatsAction extends BaseAction<EventObject> {

private final static Logger LOGGER = LoggerFactory
        .getLogger(NDVIStatsAction.class);
private static DateFormat MONTH_FORMAT = new SimpleDateFormat("MMM");
private static ROIGeometry defaultROIMask;

private NDVIStatsConfiguration configuration;
private DataStore dbStore = null;
private DataStore defaultCropMaskStore;
@SuppressWarnings("rawtypes")
private FeatureCollection defaultMaskFeaturecollection;

/**
 * Static block to register JAI components
 */
static {
    try {
        Registry.registerRIF(JAI.getDefaultInstance(),
                new ZonalStatsDescriptor(), new ZonalStatsRIF(),
                "it.geosolutions.jaiext.roiaware");
        Registry.registerRIF(JAI.getDefaultInstance(),
                new VectorBinarizeDescriptor(), new VectorBinarizeRIF(),
                Registry.JAI_TOOLS_PRODUCT);

    } catch (Throwable e) {
        // swallow exception in case the op has already been registered.
    }

}


public NDVIStatsAction(final NDVIStatsConfiguration configuration) {
    super(configuration);
    this.configuration = configuration;
}

/**
 * Execute process
 */
public Queue<EventObject> execute(Queue<EventObject> events)
        throws ActionException {

    // return object
    final Queue<EventObject> ret = new LinkedList<EventObject>();

    while (events.size() > 0) {
        final EventObject ev;
        try {
            if ((ev = events.remove()) != null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Working on incoming event: " + ev.getSource());
                }
                if (ev instanceof FileSystemEvent) {
                    FileSystemEvent fileEvent = (FileSystemEvent) ev;
                    File file = fileEvent.getSource();
                    processXMLFile(file);
                }

                // add the event to the return
                ret.add(ev);

            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Encountered a NULL event: SKIPPING...");
                }
                continue;
            }
        } catch (Exception ioe) {
            final String message = "Unable to produce the output: "
                    + ioe.getLocalizedMessage();
            if (LOGGER.isErrorEnabled())
                LOGGER.error(message, ioe);

            throw new ActionException(this, message);
        }
    }
    return ret;
}

@Override
@CheckConfiguration
public boolean checkConfiguration() {
    try {
        getDefaultMaskFeaturecollection();
        getDbStore();
        if (dbStore == null){
            throw new ActionException(this, "Incorrect configuration, can't load dbStore");
        }
        if (defaultMaskFeaturecollection == null){
            throw new ActionException(this, "Incorrect configuration, can't load mask");
        }
    } catch (Exception e) {
        LOGGER.equals(e);
    } finally {
        if (defaultCropMaskStore != null)
            defaultCropMaskStore.dispose();
        if(dbStore != null){
            dbStore.dispose();
        }
    }

    return true;
}

/**
 * Obtain default mask features collection
 * 
 * @return collection of feature in the mask
 * 
 * @throws IOException
 */
@SuppressWarnings("rawtypes")
public SimpleFeatureCollection getDefaultMaskFeaturecollection() throws IOException {

    try {
        if (defaultMaskFeaturecollection == null) { // only need to load once
            final URL url= new URL(configuration.getDefaultMaskUrl());
            defaultCropMaskStore = new ShapefileDataStore(url);
            String typeName = defaultCropMaskStore.getTypeNames()[0];

            FeatureSource source = defaultCropMaskStore
                    .getFeatureSource(typeName);

            defaultMaskFeaturecollection = source.getFeatures();
        }
    } finally {
        if (defaultCropMaskStore != null)
            defaultCropMaskStore.dispose();
    }

    return (SimpleFeatureCollection)defaultMaskFeaturecollection;
}

/**
 * Obtain DB store
 * 
 * @return
 * @throws IOException
 */
private DataStore getDbStore() throws IOException {
    if (dbStore == null) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dbtype", configuration.getDbType());
        params.put("host", configuration.getDbHost());
        params.put("port", configuration.getDbPort());
        params.put("schema", configuration.getDbSchema());
        params.put("database", configuration.getDbName());
        params.put("user", configuration.getDbUser());
        params.put("passwd", configuration.getDbPasswd());
        dbStore = DataStoreFinder.getDataStore(params);
    }

    return dbStore;
}

/**
 * This method obtain all data for the resources configured and generate the CSV file 
 * 
 * @param file
 * 
 * @throws Exception
 */
private void processXMLFile(File file) throws Exception {

    StatsBean sb = JAXB.unmarshal(file, StatsBean.class);

    if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Working NDVI action : " + sb.toString());
    }

    CLASSIFIER_TYPE classifier = sb.getClassifier();
    MASK_TYPE mask = sb.getForestMask();

    String ndviFileName = sb.getNdviFileName();

    SimpleFeatureCollection fc = getClassifiers(classifier, mask);

    GridCoverage2D coverage = null;
    try {
        coverage = getNdviTiff(ndviFileName);
        generateCSV(coverage, fc, classifier, mask, ndviFileName, configuration.getCsvSeparator());
    } finally {
        if (coverage != null) {
            disposeCoverage(coverage);
        }
    }

}

/**
 * Obtain CSV file path
 * 
 * @param classifier
 * @param mask
 * @param ndviFileName
 * @return
 */
private String getCSVFullPath(CLASSIFIER_TYPE classifier, MASK_TYPE mask,
        String ndviFileName) {
    return configuration.getOutputDirectory() + "/" + "pak_NDVI_"
            + classifier.toString().toLowerCase() + "_"
            + mask.toString().toLowerCase() + "_"
            + ndviFileName.replaceAll("dv_", "").replaceAll(".tif", "")
            + ".csv";
}

/**
 * Generate CSV file with the parameters
 * 
 * @param coverage tiff file to use in stats
 * @param fc zones to obtain the NDVI
 * @param classifier
 * @param mask
 * @param ndviFileName
 * @param csvSeparator
 * 
 * @throws Exception
 */
private void generateCSV(GridCoverage2D coverage, SimpleFeatureCollection fc,
        CLASSIFIER_TYPE classifier, MASK_TYPE mask, String ndviFileName,
        String csvSeparator) throws Exception {

    // Prepare for CSV generation
    String csvPath = getCSVFullPath(classifier, mask, ndviFileName);

    // obtain header
    List<String> header = getHeader(classifier);

    // values
    String year = "";
    String month = "";
    String dekad = "";
    String factor = "NDVI_avg";
    String distr = "";
    String prov = "";

    // Obtain year, month, decad from the name of the file:
    // dv_20130101_20130110.tif
    year = ndviFileName.substring(3, 7);
    month = ndviFileName.substring(7, 9);
    Calendar cal = new GregorianCalendar(Integer.decode(year),
            Integer.decode(month), 1);
    month = MONTH_FORMAT.format(cal.getTime());
    dekad = ndviFileName.substring(9, 11);
    dekad = dekad.equals("01") ? "1" : dekad.equals("11") ? "2" : "3";

    @SuppressWarnings("unchecked")
    Set<Object[]> data = new ListOrderedSet();
    data.add(header.toArray());
    int i = 1;

    List<FeatureAggregation> result = new ArrayList<FeatureAggregation>();

    // only one band
    int[] bands = new int[] { 0 };
    StatsType[] stats = new StatsType[] { StatsType.MEAN };

    // get the world to grid transformation
    final GridGeometry2D gg2D = coverage.getGridGeometry();
    final MathTransform worldToGrid = gg2D
            .getGridToCRS(PixelInCell.CELL_CORNER).inverse();
    final CoordinateReferenceSystem rasterCRS=gg2D.getCoordinateReferenceSystem(); 

    
    // ROI for the MASK in raster space
    final ROIGeometry maskROI = getROIMask(mask, worldToGrid,rasterCRS);

    // getting the ROI in raster space for the zones
    final List<ROI> zonesROI = new ArrayList<ROI>();
    SimpleFeatureIterator iterator = null;

    // Selection of the FeatureCollection CoordinateReferenceSystem
    final CoordinateReferenceSystem featureCollectionCRS=fc.getSchema().getCoordinateReferenceSystem();
    if(featureCollectionCRS==null){
        throw new IllegalArgumentException("The input features need a CRS");
    }
    // do we need to reproject?
    if(!CRS.equalsIgnoreMetadata(rasterCRS, featureCollectionCRS)){
        // create a transformation
        final MathTransform transform = CRS.findMathTransform(featureCollectionCRS, rasterCRS,true);// lenient tranformation
        if(!transform.isIdentity()){
            // reproject
            fc= new ReprojectProcess().execute(
                    fc, 
                    featureCollectionCRS, 
                    rasterCRS);
            
        }
    }
    // Cycle on the features for creating a list of geometries
    try {
        iterator = fc.features();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();        
            
            // zones ROI
            ROI transformedROI = new ROIGeometry(JTS.transform(
                    (Geometry) feature.getDefaultGeometry(), worldToGrid));
            
            zonesROI.add(transformedROI);

            // CSV Data
            if (CLASSIFIER_TYPE.DISTRICT.equals(classifier)
                    || CLASSIFIER_TYPE.PROVINCE.equals(classifier)) {
                prov = feature.getProperty("province").getValue().toString();
            }
            if (CLASSIFIER_TYPE.DISTRICT.equals(classifier)) {
                distr = feature.getProperty("district").getValue().toString();
            }
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("rowId", i++);
            parameters.put("year", year);
            parameters.put("mon", month);
            parameters.put("dec", dekad);
            parameters.put("factor", factor);
            parameters.put("prov", prov);
            parameters.put("distr", distr);
            // parameters.put("NDVI_avg", avg.toString());
            FeatureAggregation featureAgregation = new FeatureAggregation(
                    parameters, header, ",", true);
            result.add(featureAgregation);
        }
    } finally {
        if (iterator != null)
            iterator.close();
        if (dbStore != null) {
            dbStore.dispose();
        }
    }
    // Definition of the ZonalStats operation
    RenderedOp op = ZonalStatsDescriptor.create(coverage.getRenderedImage(),
            null, null, zonesROI, null, maskROI, false, bands, stats, null,
            null, null, null, false, null);
    // Calculation of the ZonalStats property
    @SuppressWarnings("unchecked")
    List<ZoneGeometry> statsResult = (List<ZoneGeometry>) op
            .getProperty(ZonalStatsDescriptor.ZS_PROPERTY);
    int index = 0;
    for (ZoneGeometry statResult : statsResult) {
        FeatureAggregation featureAgregation = result.get(index++);
        Double ndvi = (Double) statResult.getStatsPerBandNoClassifierNoRange(0)[0]
                .getResult();
        // If the mean is 0, then no calculations are performed
        if (ndvi != 0.0){
            // apply NDVI: Physical value = pixel value*0.004 - 0.1
            ndvi = (ndvi * 0.004) - 0.1;
            featureAgregation.getProperties().put("NDVI_avg", ndvi.toString());
            data.add(featureAgregation.toRow());
        }
    }

    File csv = new File(csvPath);
    CSVWriter.writeCsv(LOGGER, data, csv, csvSeparator, true);
}

/**
 * Obtain the ROIGeometry
 * 
 * @param mask
 * @param worldToGrid
 * @param rasterCRS 
 * @return
 * @throws MismatchedDimensionException
 * @throws TransformException
 * @throws Exception
 */
private ROIGeometry getROIMask(MASK_TYPE mask, MathTransform worldToGrid, CoordinateReferenceSystem rasterCRS)
        throws Exception {

    if (MASK_TYPE.STANDARD.equals(mask)) {
        return getDefaultROIMask(worldToGrid,rasterCRS);
    } else if (MASK_TYPE.CUSTOM.equals(mask)) {
        // TODO: CUSTOM MASK
    }
    // if(MASK_TYPE.DISABLED.equals(mask)){ return null!!}

    return null;
}

/**
 * Obtain the default ROIGeometry
 * 
 * @param worldToGrid
 * @param rasterCRS 
 * @return
 * @throws Exception
 */
private ROIGeometry getDefaultROIMask(MathTransform worldToGrid, CoordinateReferenceSystem rasterCRS)
        throws Exception {
    if (defaultROIMask == null) {
        // get the feature collection
        SimpleFeatureCollection defaultMaskFeaturecollection = getDefaultMaskFeaturecollection();
        final CoordinateReferenceSystem featureCollectionCRS=defaultMaskFeaturecollection.getSchema().getCoordinateReferenceSystem();
        if(featureCollectionCRS==null){
            throw new IllegalArgumentException(" The input ROI needs a CRS");
        }
        // do we need to reproject?
        if(!CRS.equalsIgnoreMetadata(rasterCRS, featureCollectionCRS)){
            // create a transformation
            final MathTransform transform = CRS.findMathTransform(featureCollectionCRS, rasterCRS,true);// lenient tranformation
            if(!transform.isIdentity()){
                // reproject
                defaultMaskFeaturecollection= new ReprojectProcess().execute(
                        defaultMaskFeaturecollection, 
                        featureCollectionCRS, 
                        rasterCRS);
                
            }
        }
        
        // Transformation from the Model space to the Raster space
        defaultROIMask = new ROIGeometry(
                JTS.transform(
                        collectGeometries(defaultMaskFeaturecollection),
                        worldToGrid));
    }
    return defaultROIMask;
}

private List<String> getHeader(CLASSIFIER_TYPE classifier) {

    // ID_ndv distr prov year mon dec factor NDVI_avg

    List<String> header = new ArrayList<String>();
    header.add("rowId");
    if (CLASSIFIER_TYPE.DISTRICT.equals(classifier)) {
        header.add("distr");
    }
    header.add("prov");
    header.add("year");
    header.add("mon");
    header.add("dec");
    header.add("factor");
    header.add("NDVI_avg");
    return header;
}

/**
 * Obtain the coverage file 
 * 
 * @param ndviFileName
 * 
 * @return
 * @throws IOException
 */
private GridCoverage2D getNdviTiff(String ndviFileName) throws IOException {
    File file = new File(configuration.getTiffDirectory() + "/" + ndviFileName);

    GeoTiffReader reader = new GeoTiffReader(file, new Hints(
            Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

    try {
        return (GridCoverage2D) reader.read(null);
    } finally {
        if (reader != null) {
            try {
                reader.dispose();
            } catch (Exception e) {
                LOGGER.debug(e.getLocalizedMessage(), e);
            }
        }
    }
}

private SimpleFeatureCollection getClassifiers(CLASSIFIER_TYPE classifier,
        MASK_TYPE mask) throws Exception {
    String featureSource = CLASSIFIER_TYPE.DISTRICT.equals(classifier) ? "district_boundary"
            : CLASSIFIER_TYPE.PROVINCE.equals(classifier) ? "province_boundary"
                    : null; // TODO: CUSTOM

    return getClassifiers(featureSource);
}

private SimpleFeatureCollection getClassifiers(String featureSource)
        throws IOException {
    return getDbStore().getFeatureSource(featureSource).getFeatures();
}

private Geometry collectGeometries(FeatureCollection maskCollection) {
    if (maskCollection != null && !maskCollection.isEmpty()) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Mask flag is set on true");
        }
        try {
            CollectGeometries geometriesCollecter = new CollectGeometries();

            return geometriesCollecter.execute(maskCollection, null);

        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    return null;
}

/**
 * Cleans up a coverage and its internal rendered image
 * 
 * @param coverage
 */
public static void disposeCoverage(GridCoverage coverage) {
    RenderedImage ri = coverage.getRenderedImage();
    if (coverage instanceof GridCoverage2D) {
        ((GridCoverage2D) coverage).dispose(true);
    }
    if (ri instanceof PlanarImage) {
        ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
    }
}
}
