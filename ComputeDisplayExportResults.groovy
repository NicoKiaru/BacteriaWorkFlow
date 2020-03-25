import ij.IJ
import ij.ImagePlus
import java.io.File

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options
import net.imglib2.cache.img.DiskCachedCellImgFactory
import net.imglib2.cache.img.DiskCachedCellImgOptions
import java.util.function.Function

import net.imglib2.img.Img
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.numeric.ARGBType

import net.imglib2.realtransform.AffineTransform3D

import bdv.util.volatiles.VolatileViews
import bdv.util.BdvFunctions

import net.imglib2.view.Views
import net.imglib2.Cursor

import net.imglib2.FinalInterval

import bdv.util.BdvOptions
import bdv.util.BdvHandle

import net.imglib2.img.display.imagej.ImageJFunctions

import bdv.util.BdvSource
import bdv.util.volatiles.VolatileViews
import net.imglib2.*
import net.imglib2.img.Img
import net.imglib2.util.Intervals
import net.imglib2.util.Util
import net.imglib2.view.Views
import ij.plugin.frame.RoiManager
import java.util.ArrayList
import ij.gui.Roi
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import java.nio.file.Paths
import java.util.stream.Collectors

import net.imglib2.realtransform.AffineTransform3D
import javax.swing.JTable
import javax.swing.JScrollPane
import javax.swing.*

import java.text.DecimalFormat

import javax.swing.ListSelectionModel
import bdv.util.Affine3DHelpers

import net.imglib2.util.Util

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingUtilities
import javax.swing.event.RowSorterEvent

// MorphoLibJ needed
#@File(style="directory") pathToLoad
#@int pixPerBacteria
#@boolean closeImages
#@boolean displayMeasurements
#@boolean displayBacteria
#@boolean exportResults
#@bdv.util.BdvHandle(required = false) bdvh
#@ObjectService os

fileNamePrefix = pathToLoad.toPath().subpath(pathToLoad.toPath().getNameCount()-1,pathToLoad.toPath().getNameCount()).toString()

println(fileNamePrefix)

// --------------- Initialization of certain variables
final ArrayList<BacteriaInfo> bacteriaNaturalOrder = new ArrayList<>()

// --------------- Get Images from folder
// Mask is special : used to retrieve the Rois
impMask = getImagePlus(fileNamePrefix+"-Mask.tif")
imgMask = getImage(impMask)

RoiManager roiManager = RoiManager.getRoiManager();
if (roiManager==null) {
    roiManager = new RoiManager();
}
roiManager.reset();
IJ.run("To ROI Manager")
nBacteria = roiManager.getCount()

roiManager.getRoisAsArray().eachWithIndex { roi, idx -> 
	bacterium = new BacteriaInfo()
	bacterium.roi = roi
	bacterium.idx = idx
	bacterium.px = roi.getContourCentroid()[0]
	bacterium.py = roi.getContourCentroid()[1]
	bacterium.originalFile = fileNamePrefix
	bacteriaNaturalOrder.add(bacterium)
}

// Close mask image
imp = IJ.getImage()
imp.close()

// Get BrightField Image
impBF = getImagePlus(fileNamePrefix+"-BF.tif")
imgBF = getImage(impBF)

// Measurements

// Gets fluorescent images
Stream<Path> walk = Files.walk(Paths.get(pathToLoad.toString()))
result = walk.map({x -> x.toString()})
		.filter({f -> f.endsWith(".tif")})
		.filter({f -> !(f.endsWith("-Label.tif"))})
		.filter({f -> !(f.endsWith("-Mask.tif"))})
		.filter({f -> !(f.endsWith("-BF.tif"))})
		.collect(Collectors.toList());


fluoImagesPlus = [:]
fluoImages = [:]

result.forEach({f -> 
	// Gets Suffix
	def parts = f.split("[-.]+");
	def channel = parts[parts.length-2]
	println("Getting channel " + channel )
	fluoImagesPlus[channel] = getImagePlus(fileNamePrefix+"-"+channel+".tif")
	fluoImages[channel] = getImage(fluoImagesPlus[channel])
});

// ------ Measurements

measurementOrder = [:]
idxM = -1

BacteriaInfo.measureFunctions['File'] = { bact -> fileNamePrefix }
measurementOrder[++idxM] = 'File'

BacteriaInfo.measureFunctions['Index'] = { bact -> bact.idx }
measurementOrder[++idxM] = 'Index'

BacteriaInfo.measureFunctions['Area'] = { bact -> bact.roi.getStatistics().area}
measurementOrder[++idxM] = 'Area'

BacteriaInfo.measureFunctions['Mean BF'] = { bact -> 
												bact.roi.setImage(impBF)
												bact.roi.getStatistics().mean
											}
measurementOrder[++idxM] = 'Mean BF'

fluoImages.each { chName, img ->
	if (chName.contains("Dots")) {
		BacteriaInfo.measureFunctions['Number '+chName] = { bact -> 
													bact.roi.setImage(fluoImagesPlus[chName])
													bact.roi.getStatistics().mean*bact.roi.getStatistics().area
												}
			measurementOrder[++idxM] = 'Number '+chName
	} else {
		BacteriaInfo.measureFunctions['Mean '+chName] = { bact -> 
														bact.roi.setImage(fluoImagesPlus[chName])
														bact.roi.getStatistics().mean
													}
				 measurementOrder[++idxM] = 'Mean '+chName
													
		BacteriaInfo.measureFunctions['Max '+chName] = { bact -> 
														bact.roi.setImage(fluoImagesPlus[chName])
														bact.roi.getStatistics().max
													}
				 measurementOrder[++idxM] = 'Max '+chName
													
		BacteriaInfo.measureFunctions['Min '+chName] = { bact -> 
														bact.roi.setImage(fluoImagesPlus[chName])
														bact.roi.getStatistics().min
													}
				 measurementOrder[++idxM] = 'Min '+chName
													
		BacteriaInfo.measureFunctions['stdDev '+chName] = { bact -> 
														bact.roi.setImage(fluoImagesPlus[chName])
														bact.roi.getStatistics().stdDev
													}
				 measurementOrder[++idxM] = 'stdDev '+chName

	}
	

													

}

bacteriaNaturalOrder.each{ it.measure() }

bacteriaSortedByArea = bacteriaNaturalOrder.sort{a,b -> b.values['Area'] <=> a.values['Area'] }

if (displayBacteria) {
	bdvh = displayBacteria(bacteriaNaturalOrder, bdvh)
}

if (displayMeasurements) {
	displayBacteriaMeasures(bacteriaSortedByArea, bacteriaNaturalOrder, bdvh)
	//printBacteriaMeasures(bacteriaSortedByArea)
}

if (exportResults) {
	results = getBacteriaMeasures(bacteriaSortedByArea)
	File file = new File(pathToLoad.toString()+File.separator+"results.txt")
    file.write(results)
}

if (closeImages) {
	impBF.close()
	fluoImagesPlus.each { chName, img ->
		img.close()
	}
	
}

// ----------------- Get Reordered Images


// ------------------ Inner Classes

class BacteriaInfo {
	int idx
	Roi roi
	double px
	double py
	String originalFile
	
	Map<String, Object> values = new HashMap<>()
	static Map<String, Function<BacteriaInfo, Object>> measureFunctions = new HashMap<>()

	public void measure() {
		BacteriaInfo.measureFunctions.each{ name, f ->
			values.put(name, f(this))
		}
	}
}

String getBacteriaMeasures(List<BacteriaInfo> list) {
	str = ""
	
	formatter = new DecimalFormat("#.##")
	if (list.size()>0) {
		// Text
		//list.get(0).values.each{ k,v -> str+=k+"\t"	}
		//str+="\n"

		columnNames = new String[measurementOrder.keySet().size()]
		for (int i = 0;i<measurementOrder.keySet().size(); i++) {
			columnNames[i] = measurementOrder.get(i)
		}

		columnNames.each{str+=it+"\t"}
		str+="\n"
	
		list.each {
			bact -> 
			columnNames.each{measure -> 
					if (measure instanceof String) {
						str+=bact.values.get(measure)+"\t"
					} else {
						str+=formatter.format(bact.values.get(measure))+"\t"
					}
				}
			//bact.values.each{ k,v -> str+=formatter.format(v)+"\t"	}
			str+="\n"
		}
		//println(str)
	}	
	return str;
}

// --------------- Functions
void displayBacteriaMeasures(List<BacteriaInfo> list, List<BacteriaInfo> completeOrderedlist, BdvHandle bdvh) { //List<BacteriaInfo> completeOrderedlist,
	str = ""
	if (list.size()>0) {
		
		// JTable
		columnNames = new String[measurementOrder.keySet().size()]
		for (int i = 0;i<measurementOrder.keySet().size(); i++) {
			columnNames[i] = measurementOrder.get(i)
		}
		
		//columnNames = list.get(0).values.keySet() as String[]
		
		indexColumnIndex = -1
		
		columnNames.eachWithIndex{ name, idx ->
			if (name.equals("Index")) indexColumnIndex = idx		
		}

		Object[][] data = new Object[list.size()][BacteriaInfo.measureFunctions.keySet().size()]

		formatter = new DecimalFormat("#.##")

		list.eachWithIndex{ bact, idxBact ->
			columnNames.eachWithIndex { p, idxP -> 										
				data[idxBact][idxP] = //formatter.format(
					list.get(idxBact).values[columnNames[idxP]]
					//)
			}
		}


		TableModel model = new DefaultTableModel(data, columnNames) {
		      public Class getColumnClass(int column) {
		        Class returnValue;
		        if ((column >= 0) && (column < getColumnCount())) {
		          returnValue = getValueAt(0, column).getClass();
		        } else {
		          returnValue = Object.class;
		        }
		        return returnValue;
		      }
		};
		
		JTable table = new JTable(model);
		
		//table.setColumnSelectionAllowed(true);
		
		RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);

		table.setRowSorter(sorter);

		table.getRowSorter().addRowSorterListener({e -> 
			if (e.getType()==RowSorterEvent.Type.SORTED) {
				
				if (bdvh!=null) {
					// Get 
					List<BacteriaInfo> newOrderedList = new ArrayList<>()
					for (int i = 0; i<list.size() ; i++) {
						currentIndex = sorter.convertRowIndexToModel(i)
						//table.getModel().getValueAt( i, indexColumnIndex )
						//System.out.println(currentIndex)
						newOrderedList.add(completeOrderedlist.get(currentIndex))
						
					}
					
					SwingUtilities.invokeLater({
						sources = bdvh.getViewerPanel().getState().getSources()
						
						sources.each{src ->	bdvh.getViewerPanel().removeSource(src.getSpimSource())	}
												
						displayBacteria(newOrderedList, bdvh)
					
					});
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS)
		table.setFillsViewportHeight(true)
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
		//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

		table.getSelectionModel().addListSelectionListener( {event -> 
            if (bdvh!=null) {
            	//println("table.getSelectedRow()="+table.getSelectedRow())
		        AffineTransform3D view = new AffineTransform3D()
		        bdvh.getViewerPanel().getState().getViewerTransform(view)  
		        scaleX = Affine3DHelpers.extractScale(view,0)
		           
	            at3D = new AffineTransform3D()
	            at3D.translate(0,-table.getSelectedRow()*pixPerBacteria,0)
	            at3D.scale(scaleX)
	            bdvh.getViewerPanel().setCurrentViewerTransform(at3D)
            }
    	});
 
    	f=new JFrame("Bacteria Data - "+fileNamePrefix); 
    	f.add(scrollPane);          
    	f.setSize(600,800);    
    	f.setVisible(true); 

	}
	
}

BdvHandle displayBacteria(List<BacteriaInfo> list, BdvHandle bdvh) {
	
	def imgMaskReordered = getReorderedImage(imgMask, list)
	
	def transform = new AffineTransform3D()
	if (bdvh == null) { 
		def bss = BdvFunctions.show(VolatileViews.wrapAsVolatile(imgMaskReordered), fileNamePrefix+"-Mask", BdvOptions.options().is2D())
		bss.setDisplayRange(0,1000)
		bdvh = bss.getBdvHandle()
	} else {		
		def bss = BdvFunctions.show(VolatileViews.wrapAsVolatile(imgMaskReordered), fileNamePrefix+"-Mask", BdvOptions.options().addTo(bdvh).sourceTransform(transform))
		bss.setDisplayRange(0,1000)
	}
	
	def imgBFReordered = getReorderedImage(imgBF, list)
	
	transform.translate(pixPerBacteria,0,0)
	
	bss = BdvFunctions.show(VolatileViews.wrapAsVolatile(imgBFReordered), fileNamePrefix+"-BF", BdvOptions.options().addTo(bdvh).sourceTransform(transform))
	bss.setDisplayRange(0,2000)
	
	fluoImages.each { chName, img ->
		transform.translate(pixPerBacteria,0,0)
		def imgReordered = getReorderedImage(img, list)
		def bssf = BdvFunctions.show(VolatileViews.wrapAsVolatile(imgReordered), fileNamePrefix+chName, BdvOptions.options().addTo(bdvh).sourceTransform(transform))
		if (chName.equals("Green")) {
			bssf.setColor(new ARGBType(ARGBType.rgba(0,255,0,0)))
			bssf.setDisplayRange(0,1000)
		}
		if (chName.equals("Red")) {
			bssf.setColor(new ARGBType(ARGBType.rgba(255,0,0,0)))
			bssf.setDisplayRange(30,150)
		}
		if (chName.contains("Dots")) {
			bssf.setColor(new ARGBType(ARGBType.rgba(255,255,0,0)))
			bssf.setDisplayRange(0,1)
		}
	}
	iniView = new AffineTransform3D()
	iniView.scale(3)
    bdvh.getViewerPanel().setCurrentViewerTransform(iniView)
	return bdvh
}

/**
 * Gets an extended ImagePlus from a filename
 */
ImagePlus getImagePlus(String fileName) {
	IJ.open(pathToLoad.toString()+File.separator+fileName)
	return IJ.getImage();
}

/**
 * Gets an extended Img (ImgLib2 structure) from a ImagePlus
 */
RandomAccessibleInterval getImage(ImagePlus imp) {
	def img = ImageJFunctions.wrap(imp)

	return Views.expandZero(img,[pixPerBacteria,pixPerBacteria,0] as long[]);
}


Img getReorderedImage(RandomAccessibleInterval rai_in, ArrayList<BacteriaInfo> pBact) {
	if (Util.getTypeFromInterval(rai_in).getClass().equals(UnsignedShortType.class)) {
		return getReorderedImageShort(rai_in, pBact)
	} else {
		return getReorderedImageByte(rai_in, pBact)
	}
}


Img getReorderedImageByte(RandomAccessibleInterval rai_in, ArrayList<BacteriaInfo> pBact) {
	
	def cellDimensions = [ pixPerBacteria, pixPerBacteria, 1 ] as int[]
	
	final DiskCachedCellImgOptions factoryOptions = options()
	                .cellDimensions( cellDimensions )
	                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
	                .maxCacheSize( 10 )
	
	// Creates cached image factory of Type UnsignedShort
	final DiskCachedCellImgFactory<UnsignedByteType> factory = new DiskCachedCellImgFactory<>( new UnsignedByteType(), factoryOptions );
	
	final Img<UnsignedByteType> img = factory.create(new FinalInterval([pixPerBacteria, pixPerBacteria*nBacteria, 1] as long[]), { cell -> 
									
									int cellPosX = cell.min( 0 ) / pixPerBacteria
									int cellPosY = cell.min( 1 ) / pixPerBacteria

									int idxBacteria = cellPosY * 1 + cellPosX									

									if (idxBacteria<pBact.size()) {
										int px = pBact.get(idxBacteria).px
										int py = pBact.get(idxBacteria).py
										
										def viewOnBacterium = Views.interval( rai_in, [ px - pixPerBacteria / 2  , py - pixPerBacteria / 2   ] as long[], 
																				      [ px + pixPerBacteria / 2-1, py + pixPerBacteria / 2-1 ] as long [] );
																				  
	            						def cursorOnBacterium = Views.flatIterable( viewOnBacterium ).cursor();
		                            	
		                            	def Cursor<UnsignedByteType> out = Views.flatIterable(cell).cursor()
		                            	while (out.hasNext()) {
		                            		out.next().set(cursorOnBacterium.next())
		                            	}
									}
									
	                            }, options().initializeCellsAsDirty(true));

	return img;
}


Img getReorderedImageShort(RandomAccessibleInterval rai_in, ArrayList<BacteriaInfo> pBact) {
	
	def cellDimensions = [ pixPerBacteria, pixPerBacteria, 1 ] as int[]
	
	final DiskCachedCellImgOptions factoryOptions = options()
	                .cellDimensions( cellDimensions )
	                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
	                .maxCacheSize( 10 )
	
	// Creates cached image factory of Type UnsignedShort
	final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>( new UnsignedShortType(), factoryOptions );
	
	final Img<UnsignedShortType> img = factory.create(new FinalInterval([pixPerBacteria, pixPerBacteria*nBacteria, 1] as long[]), { cell -> 
									
									int cellPosX = cell.min( 0 ) / pixPerBacteria
									int cellPosY = cell.min( 1 ) / pixPerBacteria

									int idxBacteria = cellPosY * 1 + cellPosX									

									if (idxBacteria<pBact.size()) {
										int px = pBact.get(idxBacteria).px
										int py = pBact.get(idxBacteria).py
										
										def viewOnBacterium = Views.interval( rai_in, [ px - pixPerBacteria / 2  , py - pixPerBacteria / 2   ] as long[], 
																				      [ px + pixPerBacteria / 2-1, py + pixPerBacteria / 2-1 ] as long [] );
																				  
	            						def cursorOnBacterium = Views.flatIterable( viewOnBacterium ).cursor();
		                            	
		                            	def Cursor<UnsignedShortType> out = Views.flatIterable(cell).cursor()
		                            	while (out.hasNext()) {
		                            		out.next().set(cursorOnBacterium.next())
		                            	}
									}
									
	                            }, options().initializeCellsAsDirty(true));

	return img;
}