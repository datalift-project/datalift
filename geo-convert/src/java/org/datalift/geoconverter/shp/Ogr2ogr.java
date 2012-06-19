/*
 * Copyright / Copr. IGN
 * Contributor(s) : F. Hamdi
 *
 * Contact: hamdi.faycal@gmail.com
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.geoconverter.shp;

import java.util.Vector;

import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.gdal.ogr.Driver;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Layer;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.CoordinateTransformation;

import org.datalift.fwk.log.Logger;


public class Ogr2ogr
{
	private static boolean bSkipFailures = false;
	private static int nGroupTransactions = 200;
	private static boolean bPreserveFID = false;
	private static final int OGRNullFID = -1;
	private static int nFIDToFetch = OGRNullFID;

	/**
	 * Convert SHP (any format) to SHP (WGS84)
	 * @param  args       the conversion arguments
	 */

	public void convert (String[] args)
	{
		String pszFormat = "ESRI Shapefile";
		String pszDataSource = null;
		String pszDestDataSource = null;
		Vector<String> papszLayers = new Vector<String>();
		Vector<String> papszDSCO = new Vector<String>();
		Vector<String> papszLCO = new Vector<String>();
		boolean bTransform = false;
		String pszOutputSRSDef = null;
		SpatialReference poOutputSRS = null;
		SpatialReference poSourceSRS = null;
		String pszNewLayerName = null;
		int eGType = -2;

		ogr.DontUseExceptions();

		/* -------------------------------------------------------------------- */
		/*      Register format(s).                                             */
		/* -------------------------------------------------------------------- */
		ogr.RegisterAll();

		/* -------------------------------------------------------------------- */
		/*      Processing command line arguments.                              */
		/* -------------------------------------------------------------------- */
		args = ogr.GeneralCmdLineProcessor( args );

		for (String arg:args){
			Logger.getLogger().info(arg);
		}

		for( int iArg = 0; iArg < args.length; iArg++ )
		{
			if( args[iArg].equalsIgnoreCase("-f") && iArg < args.length-1 )
			{
				pszFormat = args[++iArg];
			}
			else if( args[iArg].equalsIgnoreCase("-lco") && iArg < args.length-1 )
			{
				papszLCO.addElement(args[++iArg]);
			}
			else if( args[iArg].equalsIgnoreCase("-t_srs") && iArg < args.length-1 )
			{
				pszOutputSRSDef = args[++iArg];
				bTransform = true;
			}
			else if( pszDestDataSource == null )
				pszDestDataSource = args[iArg];
			else if( pszDataSource == null )
				pszDataSource = args[iArg];
		}


		/* -------------------------------------------------------------------- */
		/*      Open data source.                                               */
		/* -------------------------------------------------------------------- */
		DataSource poDS;

		poDS = ogr.Open( pszDataSource, false );

		/* -------------------------------------------------------------------- */
		/*      Report failure                                                  */
		/* -------------------------------------------------------------------- */
		if( poDS == null )
		{
			Logger.getLogger().error("FAILURE:\n" + 
					"Unable to open datasource ` " + pszDataSource + "' with the following drivers.");

			for( int iDriver = 0; iDriver < ogr.GetDriverCount(); iDriver++ )
			{
				Logger.getLogger().error("  . " + ogr.GetDriver(iDriver).GetName() );
			}

		}

		/* -------------------------------------------------------------------- */
		/*      Try opening the output datasource as an existing, writable      */
		/* -------------------------------------------------------------------- */


		/* -------------------------------------------------------------------- */
		/*      Find the output driver.                                         */
		/* -------------------------------------------------------------------- */
		DataSource poODS = null;
		Driver poDriver = null;

		int iDriver;

		poDriver = ogr.GetDriverByName(pszFormat);
		if( poDriver == null )
		{
			Logger.getLogger().error("Unable to find driver `" + pszFormat +"'." );
			Logger.getLogger().error( "The following drivers are available:" );

			for( iDriver = 0; iDriver < ogr.GetDriverCount(); iDriver++ )
			{
				Logger.getLogger().error("  . " + ogr.GetDriver(iDriver).GetName() );
			}
		}

		if( poDriver.TestCapability( ogr.ODrCCreateDataSource ) == false )
		{
			Logger.getLogger().error( pszFormat + " driver does not support data source creation.");
		}

		/* -------------------------------------------------------------------- */
		/*      Create the output data source.                                  */
		/* -------------------------------------------------------------------- */
		poODS = poDriver.CreateDataSource( pszDestDataSource, papszDSCO );
		if( poODS == null )
		{
			Logger.getLogger().error( pszFormat + " driver failed to create "+ pszDestDataSource );
		}

		/* -------------------------------------------------------------------- */
		/*      Parse the output SRS definition if possible.                    */
		/* -------------------------------------------------------------------- */
		if( pszOutputSRSDef != null )
		{
			poOutputSRS = new SpatialReference();
			if( poOutputSRS.SetFromUserInput( pszOutputSRSDef ) != 0 )
			{
				Logger.getLogger().error( "Failed to process SRS definition: " + pszOutputSRSDef );
			}
		}

		int nLayerCount = 0;
		Layer[] papoLayers = null;

		/* -------------------------------------------------------------------- */
		/*      Process each data source layer.                                 */
		/* -------------------------------------------------------------------- */
		if ( papszLayers.size() == 0)
		{
			nLayerCount = poDS.GetLayerCount();
			papoLayers = new Layer[nLayerCount];

			for( int iLayer = 0; 
					iLayer < nLayerCount; 
					iLayer++ )
			{
				Layer        poLayer = poDS.GetLayer(iLayer);

				if( poLayer == null )
				{
					Logger.getLogger().error("FAILURE: Couldn't fetch advertised layer " + iLayer + "!");
				}

				papoLayers[iLayer] = poLayer;
			}
		}
		/* -------------------------------------------------------------------- */
		/*      Process specified data source layers.                           */
		/* -------------------------------------------------------------------- */
		else
		{
			nLayerCount = papszLayers.size();
			papoLayers = new Layer[nLayerCount];

			for( int iLayer = 0; 
					iLayer < papszLayers.size(); 
					iLayer++ )
			{
				Layer poLayer = poDS.GetLayerByName((String)papszLayers.get(iLayer));

				if( poLayer == null )
				{
					Logger.getLogger().error("FAILURE: Couldn't fetch advertised layer " + (String)papszLayers.get(iLayer) + "!");
				}

				papoLayers[iLayer] = poLayer;
			}
		}

		long[] panLayerCountFeatures = new long[nLayerCount];

		/* Second pass to do the real job */
		for( int iLayer = 0; 
				iLayer < nLayerCount; 
				iLayer++ )
		{
			Layer poLayer = papoLayers[iLayer];

			if( !TranslateLayer( poDS, poLayer, poODS, papszLCO, pszNewLayerName, bTransform, poOutputSRS,
					poSourceSRS, eGType, panLayerCountFeatures[iLayer] ) && !bSkipFailures )
			{
				Logger.getLogger().error(
						"Terminating translation prematurely after failed\n" +
								"translation of layer " + poLayer.GetLayerDefn().GetName() + " (use -skipfailures to skip errors)");

			}
		}

		/* -------------------------------------------------------------------- */
		/*      Close down.                                                     */
		/* -------------------------------------------------------------------- */
		/* We must explicetely destroy the output dataset in order the file */
		/* to be properly closed ! */
		poODS.delete();
		poDS.delete();
	}



	private int wkbFlatten(int eType)
	{
		return eType & (~ogrConstants.wkb25DBit);
	}

	/************************************************************************/
	/*                               SetZ()                                 */
	/************************************************************************/
	private void SetZ (Geometry poGeom, double dfZ )
	{
		if (poGeom == null)
			return;
		switch (wkbFlatten(poGeom.GetGeometryType()))
		{
		case ogr.wkbPoint:
			poGeom.SetPoint(0, poGeom.GetX(), poGeom.GetY(), dfZ);
			break;

		case ogr.wkbLineString:
		case ogr.wkbLinearRing:
		{
			int i;
			for(i=0;i<poGeom.GetPointCount();i++)
				poGeom.SetPoint(i, poGeom.GetX(i), poGeom.GetY(i), dfZ);
			break;
		}

		case ogr.wkbPolygon:
		case ogr.wkbMultiPoint:
		case ogr.wkbMultiLineString:
		case ogr.wkbMultiPolygon:
		case ogr.wkbGeometryCollection:
		{
			int i;
			for(i=0;i<poGeom.GetGeometryCount();i++)
				SetZ(poGeom.GetGeometryRef(i), dfZ);
			break;
		}

		default:
			break;
		}
	}


	/************************************************************************/
	/*                           TranslateLayer()                           */
	/************************************************************************/

	private boolean TranslateLayer( DataSource poSrcDS, 
			Layer poSrcLayer,
			DataSource poDstDS,
			Vector<String> papszLCO,
			String pszNewLayerName,
			boolean bTransform, 
			SpatialReference poOutputSRS,
			SpatialReference poSourceSRS,
			int eGType,
			long nCountLayerFeatures )

	{
		Layer poDstLayer;
		FeatureDefn poSrcFDefn;
		int eErr;
		boolean bForceToPolygon = false;
		boolean bForceToMultiPolygon = false;
		boolean bForceToMultiLineString = false;

		if( pszNewLayerName == null )
			pszNewLayerName = poSrcLayer.GetLayerDefn().GetName();

		if( wkbFlatten(eGType) == ogr.wkbPolygon )
			bForceToPolygon = true;
		else if( wkbFlatten(eGType) == ogr.wkbMultiPolygon )
			bForceToMultiPolygon = true;
		else if( wkbFlatten(eGType) == ogr.wkbMultiLineString )
			bForceToMultiLineString = true;

		/* -------------------------------------------------------------------- */
		/*      Setup coordinate transformation if we need it.                  */
		/* -------------------------------------------------------------------- */
		CoordinateTransformation poCT = null;

		if( bTransform )
		{
			if( poSourceSRS == null )
				poSourceSRS = poSrcLayer.GetSpatialRef();

			if( poSourceSRS == null )
			{
				Logger.getLogger().error("Can't transform coordinates, source layer has no\n" +
						"coordinate system.  Use -s_srs to set one." );
			}

			/*CPLAssert( null != poSourceSRS );
            CPLAssert( null != poOutputSRS );*/

			poCT = new CoordinateTransformation( poSourceSRS, poOutputSRS );
		}

		/* -------------------------------------------------------------------- */
		/*      Get other info.                                                 */
		/* -------------------------------------------------------------------- */
		poSrcFDefn = poSrcLayer.GetLayerDefn();

		if( poOutputSRS == null )
			poOutputSRS = poSrcLayer.GetSpatialRef();

		/* -------------------------------------------------------------------- */
		/*      Find the layer.                                                 */
		/* -------------------------------------------------------------------- */

		/* GetLayerByName() can instanciate layers that would have been */
		/* 'hidden' otherwise, for example, non-spatial tables in a */
		/* Postgis-enabled database, so this apparently useless command is */
		/* not useless... (#4012) */
		gdal.PushErrorHandler("CPLQuietErrorHandler");
		poDstLayer = poDstDS.GetLayerByName(pszNewLayerName);
		gdal.PopErrorHandler();
		gdal.ErrorReset();

		int iLayer = -1;
		if( poDstLayer != null )
		{
			int nLayerCount = poDstDS.GetLayerCount();
			for( iLayer = 0; iLayer < nLayerCount; iLayer++ )
			{
				Layer        poLayer = poDstDS.GetLayer(iLayer);

				if( poLayer != null
						&& poLayer.GetName().equals(poDstLayer.GetName()) )
				{
					break;
				}
			}

			if (iLayer == nLayerCount)
				/* shouldn't happen with an ideal driver */
				poDstLayer = null;
		}

		/* -------------------------------------------------------------------- */
		/*      If the layer does not exist, then create it.                    */
		/* -------------------------------------------------------------------- */
		if( poDstLayer == null )
		{
			if( eGType == -2 )
			{
				eGType = poSrcFDefn.GetGeomType();

			}

			if( poDstDS.TestCapability( ogr.ODsCCreateLayer ) == false)
			{
				Logger.getLogger().error(
						"Layer " + pszNewLayerName + "not found, and CreateLayer not supported by driver.");
				return false;
			}

			gdal.ErrorReset();

			poDstLayer = poDstDS.CreateLayer( pszNewLayerName, poOutputSRS,
					eGType, papszLCO );

			if( poDstLayer == null )
				return false;
		}

		else
		{
			if( papszLCO.size() > 0 )
			{
				Logger.getLogger().error("WARNING: Layer creation options ignored since an existing layer is\n" +
						"         being appended to." );
			}
		}

		/* -------------------------------------------------------------------- */
		/*      Add fields.  Default to copy all field.                         */
		/*      If only a subset of all fields requested, then output only      */
		/*      the selected fields, and in the order that they were            */
		/*      selected.                                                       */
		/* -------------------------------------------------------------------- */
		int iField;

		/* Initialize the index-to-index map to -1's */
		int nSrcFieldCount = poSrcFDefn.GetFieldCount();
		int[] panMap = new int [nSrcFieldCount];
		for( iField=0; iField < nSrcFieldCount; iField++)
			panMap[iField] = -1;

		FeatureDefn poDstFDefn = poDstLayer.GetLayerDefn();

		int nDstFieldCount = 0;
		if (poDstFDefn != null)
			nDstFieldCount = poDstFDefn.GetFieldCount();
		for( iField = 0; iField < nSrcFieldCount; iField++ )
		{
			FieldDefn poSrcFieldDefn = poSrcFDefn.GetFieldDefn(iField);
			FieldDefn oFieldDefn = new FieldDefn( poSrcFieldDefn.GetNameRef(),
					poSrcFieldDefn.GetFieldType() );
			oFieldDefn.SetWidth( poSrcFieldDefn.GetWidth() );
			oFieldDefn.SetPrecision( poSrcFieldDefn.GetPrecision() );

			/* The field may have been already created at layer creation */
			int iDstField = -1;
			if (poDstFDefn != null)
				iDstField = poDstFDefn.GetFieldIndex(oFieldDefn.GetNameRef());
			if (iDstField >= 0)
			{
				panMap[iField] = iDstField;
			}
			else if (poDstLayer.CreateField( oFieldDefn ) == 0)
			{
				/* now that we've created a field, GetLayerDefn() won't return NULL */
				if (poDstFDefn == null)
					poDstFDefn = poDstLayer.GetLayerDefn();

				/* Sanity check : if it fails, the driver is buggy */
				if (poDstFDefn != null &&
						poDstFDefn.GetFieldCount() != nDstFieldCount + 1)
				{
					Logger.getLogger().error(
							"The output driver has claimed to have added the " + oFieldDefn.GetNameRef() + " field, but it did not!");
				}
				else
				{
					panMap[iField] = nDstFieldCount;
					nDstFieldCount ++;
				}
			}
		}

		/* -------------------------------------------------------------------- */
		/*      Transfer features.                                              */
		/* -------------------------------------------------------------------- */
		Feature poFeature;
		int nFeaturesInTransaction = 0;

		int iSrcZField = -1;

		poSrcLayer.ResetReading();

		if( nGroupTransactions > 0)
			poDstLayer.StartTransaction();

		while( true )
		{
			Feature      poDstFeature = null;

			if( nFIDToFetch != OGRNullFID )
			{
				// Only fetch feature on first pass.
				if( nFeaturesInTransaction == 0 )
					poFeature = poSrcLayer.GetFeature(nFIDToFetch);
				else
					poFeature = null;
			}
			else
				poFeature = poSrcLayer.GetNextFeature();

			if( poFeature == null )
				break;

			int nParts = 0;
			int nIters = 1;

			for(int iPart = 0; iPart < nIters; iPart++)
			{

				if( ++nFeaturesInTransaction == nGroupTransactions )
				{
					poDstLayer.CommitTransaction();
					poDstLayer.StartTransaction();
					nFeaturesInTransaction = 0;
				}

				gdal.ErrorReset();
				poDstFeature = new Feature( poDstLayer.GetLayerDefn() );

				if( poDstFeature.SetFromWithMap( poFeature, 1, panMap ) != 0 )
				{
					if( nGroupTransactions > 0)
						poDstLayer.CommitTransaction();

					Logger.getLogger().error(
							"Unable to translate feature " + poFeature.GetFID() + " from layer " +
									poSrcFDefn.GetName() );

					poFeature.delete();
					poFeature = null;
					poDstFeature.delete();
					poDstFeature = null;
					return false;
				}

				if( bPreserveFID )
					poDstFeature.SetFID( poFeature.GetFID() );

				Geometry poDstGeometry = poDstFeature.GetGeometryRef();
				if (poDstGeometry != null)
				{
					if (nParts > 0)
					{
						/* For -explodecollections, extract the iPart(th) of the geometry */
						Geometry poPart = poDstGeometry.GetGeometryRef(iPart).Clone();
						poDstFeature.SetGeometryDirectly(poPart);
						poDstGeometry = poPart;
					}

					if (iSrcZField != -1)
					{
						SetZ(poDstGeometry, poFeature.GetFieldAsDouble(iSrcZField));
						/* This will correct the coordinate dimension to 3 */
						Geometry poDupGeometry = poDstGeometry.Clone();
						poDstFeature.SetGeometryDirectly(poDupGeometry);
						poDstGeometry = poDupGeometry;
					}

					if( poCT != null )
					{
						eErr = poDstGeometry.Transform( poCT );
						if( eErr != 0 )
						{
							if( nGroupTransactions > 0)
								poDstLayer.CommitTransaction();

							Logger.getLogger().error("Failed to reproject feature" + poFeature.GetFID() + " (geometry probably out of source or destination SRS).");
							if( !bSkipFailures )
							{
								poFeature.delete();
								poFeature = null;
								poDstFeature.delete();
								poDstFeature = null;
								return false;
							}
						}
					}
					else if (poOutputSRS != null)
					{
						poDstGeometry.AssignSpatialReference(poOutputSRS);
					}

					if( bForceToPolygon )
					{
						poDstFeature.SetGeometryDirectly(ogr.ForceToPolygon(poDstGeometry));
					}

					else if( bForceToMultiPolygon )
					{
						poDstFeature.SetGeometryDirectly(ogr.ForceToMultiPolygon(poDstGeometry));
					}

					else if ( bForceToMultiLineString )
					{
						poDstFeature.SetGeometryDirectly(ogr.ForceToMultiLineString(poDstGeometry));
					}
				}

				gdal.ErrorReset();
				if( poDstLayer.CreateFeature( poDstFeature ) != 0
						&& !bSkipFailures )
				{
					if( nGroupTransactions > 0 )
						poDstLayer.RollbackTransaction();

					poDstFeature.delete();
					poDstFeature = null;
					return false;
				}

				poDstFeature.delete();
				poDstFeature = null;
			}

			poFeature.delete();
			poFeature = null;

		}

		if( nGroupTransactions > 0 )
			poDstLayer.CommitTransaction();

		return true;
	}

	/************************************************************************/
	/*                               main example                           */
	/************************************************************************/
	/*public static void main (String[] args)
	{
		
		// !! IMPORTANT : il faut tout d'abord convertir vers wgs84
		
		String fileIn = "/home/hamdi/workspace/ogr2ogr/testconvert/DEPARTEMENT.shp";
		
		//String fileOut = "/home/hamdi/workspace/ogr2ogr/testconvert/DEPARTEMENT.csv";
		//String[] agrum = {"-f", "CSV", fileOut, fileIn, "-lco", "GEOMETRY=AS_WKT"};
		
		String fileOut = "/home/hamdi/workspace/ogr2ogr/testconvert/DEPARTEMENT.gml";
		String[] agrum = {"-f", "GML", fileOut, fileIn};
		
		Ogr2ogr testconv = new Ogr2ogr();
		testconv.convert(agrum);
	}*/

}
