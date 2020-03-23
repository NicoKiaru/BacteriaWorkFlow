#@ImagePlus(label = "Stack to quantify") bf_fluo_Stack // Expects a multichannel with BrightField and Fluorescence
#@File(label="Path containing analysis scripts and classifier, named 'BacteriaPixelClassification.ilp'", style="directory") pathToScriptFiles
#@String(label="Channels, separated by ',', BF compulsory (ex : BF,Green,Red)") channelInfos
#@Boolean(label="Correct drift") correctDrift
#@Boolean(label="Export Data") export
#@File(style="Export Directory", required = false, style="directory" ) pathToSave
#@ScriptService ss
#@CommandService cs

import ch.epfl.biop.wrappers.ilastik.Ilastik_Classifier_Pixel
import java.io.File
import ij.IJ
import ij.plugin.frame.RoiManager
import org.apache.commons.io.FilenameUtils
import ij.Prefs
import ij.plugin.ChannelSplitter
import ij.ImagePlus

Prefs.blackBackground = true;

// Set channels infos from String
channels = channelInfos.split(",");

// Indexes of BrightField Image and fluorescent channels
bfChannelIndex = -1;
fluoChannels = [:] 

IJ.run("Make Composite");

channels.eachWithIndex { channel, index ->
	if (channel.trim().equals("BF")) {
		bfChannelIndex = index
		bf_fluo_Stack.setC(bfChannelIndex+1);
		IJ.run(bf_fluo_Stack, "Grays" , "");
		IJ.run(bf_fluo_Stack, "Enhance Contrast", "saturated=0.35");
		IJ.log("Channel "+bfChannelIndex+" is BF :  "+channel);
	} else {
		fluoChannels[channel] = index
		IJ.log("Channel "+fluoChannels[channel]+" is Fluo : "+channel);
	}
}

if (bfChannelIndex == -1 ) {
	println("Error, no brightField ('BF') channel defined");
	return;
}

fluoChannels.each { lut, index ->
	bf_fluo_Stack.setC(index+1);
	IJ.run(bf_fluo_Stack, lut.trim() , "");
	IJ.run(bf_fluo_Stack, "Enhance Contrast", "saturated=0.35");
}

// ----------------------- Extract Bright Field Image and Normalize it
extractBFChannelScriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"ExtractBFChannel.groovy";
extractBFChannelTask = ss.run(new File(extractBFChannelScriptFileName),true, "stack",bf_fluo_Stack,
																			 "bfChannel",bfChannelIndex+1, // Because 1 based
																			 "normalize", true)

result = extractBFChannelTask.get() // result contains the BF image

// ----------------------- Segment brightfield channel using an ilastik classifier (should be set with tif export)
bf_image = result.getOutput("bfImage") // get output from previous script

// Using and ilastik classifier with three classes : background, cell center, cell edges
classify = cs.run(Ilastik_Classifier_Pixel.class, true, "ilastikProjectFile", new File(pathToScriptFiles.getAbsolutePath()+File.separator+"BacteriaPixelClassification.ilp"), // assumes the classifier to be named this way
									 "export_source","Simple Segmentation",
									 "export_dtype","uint8",
									 "image_in", bf_image,
									 "verbose", false)

segment_image = classify.get().getOutput("image_out")
segment_image.show();
									
// ----------------------- Make cells from segmentation image -> expand seeds labeled regions until they go out of the bacteria or touch a neighbor
makeCellsScriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"MakeCells.groovy";
makeCellsTask = ss.run(new File(makeCellsScriptFileName),true, "imp",segment_image)
result = makeCellsTask.get()

// get Image label
label_image = result.getOutput("imgLabel") // get output from previous script

binary_image = label_image.duplicate()
binary_image.show()

IJ.run(binary_image, "8-bit","");
IJ.run(binary_image, "Max...", "value=1");
IJ.run(binary_image, "Multiply...", "value=255");

// Get bacteria outline and display regions on the original image
RoiManager roiManager = RoiManager.getRoiManager();
if (roiManager==null) {
    roiManager = new RoiManager();
}
roiManager.reset();
IJ.run(binary_image, "Analyze Particles...", "add");
roiManager.runCommand(bf_fluo_Stack,"Show All");

// Correct drift
if (correctDrift) {
	correctDriftScriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"FindAndCorrectDrift.ijm";
	fluoChannels.each { name, index ->
		correctDriftTask = ss.run(new File(correctDriftScriptFileName),true, 
			"hyperStack",bf_fluo_Stack.getID(), 
			"mask", binary_image.getID(), 
			"range", 10, 
			"fluoChannelIndex", index+1) // 1 based
		result = correctDriftTask.get()
	}
}

// Clean images
segment_image.close();	
bf_image.close();


// ------------------- Splits image for ImageJ use
fileNameWithOutExt = FilenameUtils.removeExtension(bf_fluo_Stack.getTitle())
channels = ChannelSplitter.split(bf_fluo_Stack)

fluoChannels.each { name, index ->
	IJ.log("img"+index+":"+channels[index])
	channels[index].setTitle(fileNameWithOutExt+"-"+name)
	channels[index].show()
}

channels[bfChannelIndex].setTitle(fileNameWithOutExt+"-BF")
channels[bfChannelIndex].show()

label_image.setTitle(fileNameWithOutExt+"-Label")
binary_image.setTitle(fileNameWithOutExt+"-Mask")

bf_fluo_Stack.changes = false
bf_fluo_Stack.close()

label_image.setCalibration(channels[bfChannelIndex].getCalibration())
binary_image.setCalibration(channels[bfChannelIndex].getCalibration())


// ------------------- Export to folder
if (export) {
	File directory = new File(pathToSave.getAbsolutePath()+File.separator+fileNameWithOutExt);
    if (!directory.exists()){
        directory.mkdir();
        // If you require it to make the entire directory path including parents,
        // use directory.mkdirs(); here instead.
    }
    pathToSave = directory
	// ------------------- Builds image for manual check TO BE DONE
 	saveImagesToTiff(channels[bfChannelIndex], binary_image, label_image)
 	
 	fluoChannels.each { name, idx ->
 		//IJ.log("idx:"+idx+":"+channels[idx])
 		//IJ.log("title:"+idx+":"+channels[idx].getTitle())
 		//channels[idx].show()
 		saveImagesToTiff(channels[idx])
 	}
}

void saveImagesToTiff(ImagePlus... imps) {
	imps.each{imp -> 
			fileNameWithOutExt = FilenameUtils.removeExtension(imp.getTitle())
		 	IJ.saveAs(imp, "Tiff", pathToSave.getAbsolutePath()+File.separator+fileNameWithOutExt+".tif")
		 	imp.changes = false
			imp.close()
	}
}



