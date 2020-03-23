#@ImagePlus(label = "Stack to quantify") bf_fluo_Stack // Expects a multichannel with BrightField and Fluorescence
#@File(label="Path containing analysis scripts and classifier, named 'BacteriaPixelClassification.ilp'", style="directory") pathToScriptFiles
#@String(label="Channels, separated by ',', BF compulsory (ex : BF,Green,Red)") channelInfos
#@Boolean(label="Erase previous segmentation results") erasePreviousSegmentationResults
#@String(label="Command Dot Detection (ex: Green[1,50]; Red[1,100])") dotDetectionCommand 
#@File(style="Export", required = false, style="directory" ) pathToSave
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

// ---------------------- Do we need to Redo the segmentation ? 

fileNameWithOutExt = FilenameUtils.removeExtension(bf_fluo_Stack.getTitle())
println(pathToSave.getAbsolutePath()+File.separator+fileNameWithOutExt)
pathOfImage = pathToSave.getAbsolutePath()+File.separator+fileNameWithOutExt
File directory = new File(pathOfImage);

segmentationNeeded = false

if (!directory.exists()){
	// No results -> Need to do the segmentation
	segmentationNeeded = true
} else {
	// Results present
	segmentationNeeded = erasePreviousSegmentationResults
}

bf_fluo_Stack.show()

if (segmentationNeeded) {
	scriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"SegmentAndExport.groovy";
	task = ss.run(new File(scriptFileName),true, "bf_fluo_Stack",bf_fluo_Stack,
												 "pathToScriptFiles",pathToScriptFiles,
												 "channelInfos", channelInfos,
												 "correctDrift", true,
												 "export", true,
												 "pathToSave", pathToSave)
	task.get()
} else {
	//bf_fluo_Stack.close()
}

//dotDetectionCommand
dotDetectionCommand.split(";").each{ command -> 
	IJ.log(command)
	def (params) = command =~ /(.*)\[(\d*),(\d*)\]/
	channel = params[1].trim()
	smoothing = params[2].trim()
	prominence = params[3].trim()
	scriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"DotsDetect.groovy";
	task = ss.run(new File(scriptFileName),true, "pathToLoad",pathOfImage,
												 "channelDotDetect",channel,
												 "smoothing", smoothing,
												 "prominence", prominence)
	task.get()
	
}

scriptFileName = pathToScriptFiles.getAbsolutePath()+File.separator+"ComputeDisplayExportResults.groovy";
task = ss.run(new File(scriptFileName),true, "pathToLoad",pathOfImage,
											 "pixPerBacteria", 80,  // useless -> no display
											 "closeImages", true,
											 "displayMeasurements", false,
											 "displayBacteria", false,
											 "exportResults", true)
task.get()

