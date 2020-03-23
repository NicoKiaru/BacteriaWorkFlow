#@ImagePlus(label = "Stack to quantify") bf_fluo_Stack // Expects a multichannel with BrightField and Fluorescence
#@File(label="Path containing analysis scripts and classifier, named 'BacteriaPixelClassification.ilp'", style="directory") pathToScriptFiles
#@Integer(label="BrightField Channel Index", value = 3 ) bfChannel // Channel Index for BrightField Image
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

// Make correct LUTs
bf_fluo_Stack.setC(bfChannel);
IJ.run(bf_fluo_Stack, "Grays", "");
IJ.run(bf_fluo_Stack, "Enhance Contrast", "saturated=0.35");


// ----------------------- Extract Bright Field Image and Normalize it
extractBFChannelScriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"ExtractBFChannel.groovy";
extractBFChannelTask = ss.run(new File(extractBFChannelScriptFileName),true, "stack",bf_fluo_Stack,
																			 "bfChannel",bfChannel,
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
binary_image = result.getOutput("imgLabel") // get output from previous script
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

// Clean images
segment_image.close();	
bf_image.close();
									 