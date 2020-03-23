#@ImagePlus imp

import ij.ImagePlus;
import ij.gui.Roi;
import java.util.HashSet;
import ij.process.ImageProcessor;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor


rois = labelImageToRoiArray(imp)
putRoisToRoiManager(rois,false);


	//------------- HELPERS

	public ArrayList<Roi> labelImageToRoiArray(ImagePlus imp) {
		ArrayList<Roi> roiArray = new ArrayList<>();
		ImageProcessor ip = imp.getProcessor();
		float[][] pixels = ip.getFloatArray();
		
		HashSet<Float> existingPixelValues = new HashSet<>();

		for (int x=0;x<ip.getWidth();x++) {
			for (int y=0;y<ip.getHeight();y++) {
				existingPixelValues.add((pixels[x][y]));
			}
		}

		// Converts data in case thats a RGB Image	
		fp = new FloatProcessor(ip.getWidth(), ip.getHeight())	
		fp.setFloatArray(pixels)
		imgFloatCopy = new ImagePlus("FloatLabel",fp)

		existingPixelValues.each { v ->
			fp.setThreshold( v,v,ImageProcessor.NO_LUT_UPDATE);
			Roi roi = ThresholdToSelection.run(imgFloatCopy);
			roi.setName(Integer.toString((int) (double) v));
			roiArray.add(roi);
		}
		return roiArray;
	}

    public static void putRoisToRoiManager(ArrayList<Roi> rois, boolean keepROISName) {
        RoiManager roiManager = RoiManager.getRoiManager();
        if (roiManager==null) {
            roiManager = new RoiManager();
        }
		    roiManager.reset();
        for (int i = 0; i < rois.size(); i++) {
        	if (!keepROISName) {
        		rois.get(i).setName(""+i);
        	}
            roiManager.addRoi(rois.get(i));
        }
        roiManager.runCommand("Show All");
}