// Starts with : 
// - an hyperstack containing an image to align on a certain channel
// - an image containing a mask on which the signal will be maximzed

//ImagePlus hyperStack
//ImagePlus mask

#@int hyperStack
#@int mask

#@int fluoChannelIndex
#@int range

selectImage(hyperStack);
run("Make Composite");
run("Select None");
run("Duplicate...", "duplicate channels="+fluoChannelIndex);
fluoImage = getTitle();

selectImage(mask);
run("Select None");
run("Create Selection");
run("Make Inverse");
roiManager("Add");
makeRectangle(range, range, getWidth()-2*range-1, getHeight()-2*range-1);
roiManager("Add");
roiManager("Select", newArray(roiManager("count")-1,roiManager("count")-2));
roiManager("AND");
roiManager("Add");

roiManager("Select", newArray(roiManager("count")-2,roiManager("count")-3));
roiManager("Delete");

idxRoiManager = roiManager("count")-1;

roiManager("Add");
roiManager("Select", idxRoiManager+1);

selectWindow(fluoImage);

bestXS = 0;
bestYS = 0;
bestMax = -1;

for (xShift = -range;xShift<=range;xShift++) {
	for (yShift = -range;yShift<=range;yShift++) {
		roiManager("Select", idxRoiManager);
		//roiManager("Add");
		//roiManager("Select", idxRoiManager+1);
		roiManager("translate", xShift, yShift);
		// Hack to update the translation
		roiManager("Select", idxRoiManager+1);
		roiManager("Select", idxRoiManager);
		
		getStatistics(area, mean, min, max, std, histogram);
		//print("xShift\t"+xShift+"\t yShift \t"+yShift+"\t v \t"+mean);
		if (mean>bestMax) {
			bestMax = mean;
			bestXS = xShift;
			bestYS = yShift;
		}		
		//roiManager("Select", idxRoiManager+1);
		//roiManager("Delete");
		
		roiManager("translate", -xShift, -yShift);
	}
}

print("Estimated Shift : ["+bestXS+":"+bestYS+"]");

selectImage(hyperStack);
setSlice(fluoChannelIndex);
run("Translate...", "x="+(-bestXS)+" y="+(-bestYS)+" slice interpolation=None");

// Done : now cleaning

roiManager("Select", idxRoiManager+1);
roiManager("Delete");

roiManager("Select", idxRoiManager);
roiManager("Delete");



selectWindow(fluoImage);
close();

/*
selectWindow("20200306_condD_24h_pic3.czi");
selectWindow("Result of 20200306_condD_24h_pic3_BacteriaPixelClassification_Simple Segment-lblImage");
selectWindow("20200306_condD_24h_pic3.czi");
selectWindow("Result of 20200306_condD_24h_pic3_BacteriaPixelClassification_Simple Segment-lblImage");
selectWindow("20200306_condD_24h_pic3.czi");
run("Duplicate...", "duplicate channels=2");
close();
run("Select None");
run("Duplicate...", "duplicate channels=2");
selectWindow("Result of 20200306_condD_24h_pic3_BacteriaPixelClassification_Simple Segment-lblImage");
run("Create Selection");
run("Make Inverse");
selectWindow("20200306_condD_24h_pic3-1.czi");
run("Restore Selection");
run("Measure");
roiManager("Add");


roiManager("Select", 579);
roiManager("translate", 10, 10);
run("Measure");*/
