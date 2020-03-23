import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

#@File(style="directory") pathToLoad
#@String channelDotDetect
#@int smoothing
#@int prominence

fileNamePrefix = pathToLoad.toPath().subpath(pathToLoad.toPath().getNameCount()-1,pathToLoad.toPath().getNameCount()).toString()

println(fileNamePrefix)

imp = getImagePlus(fileNamePrefix+"-"+channelDotDetect+".tif")

IJ.run("FeatureJ Laplacian", "compute smoothing="+smoothing);
impLaplacian = IJ.getImage();
IJ.run(impLaplacian, "Find Maxima...", "prominence="+prominence+" exclude light output=[Single Points]");
impDots = IJ.getImage();
IJ.run(impDots, "Divide...", "value=255 stack");
IJ.saveAs(impDots, "Tiff", pathToLoad.toString()+File.separator+fileNamePrefix+"-"+channelDotDetect+"Dots_[P_"+prominence+"][S_"+smoothing+"].tif");
impDots.close()
imp.close()
impLaplacian.close()

ImagePlus getImagePlus(String fileName) {
	IJ.open(pathToLoad.toString()+File.separator+fileName)
	return IJ.getImage();
}
