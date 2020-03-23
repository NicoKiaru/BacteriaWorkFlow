#@ImagePlus(label = "Multichannel stack with BrightField image and Fluorescence") stack
#@Integer(label="Channel Index for BrightField Image", value = 3 ) bfChannel
#@File(style="directory") pathToSave
#@Boolean(label="Normalize Bright Field image") normalize

import ij.IJ
import File.*;
import ij.plugin.ImageCalculator;

// Extract brightfield channel
IJ.run(stack, "Duplicate...", "duplicate channels="+bfChannel);
IJ.run("Grays");
bfImage = IJ.getImage();
if (normalize) {
	IJ.run(bfImage,"Duplicate...", " ");
	img_blur = IJ.getImage();
	IJ.run(img_blur,"Gaussian Blur...", "sigma=200");
	ic = new ImageCalculator();
	img_out = ic.run("Divide create 32-bit", bfImage, img_blur);
	img_blur.changes=false;
	img_blur.close();
	img_temp = bfImage;
	bfImage = img_out;
}
bfImage.setTitle(stack.getTitle()+"-BF");
IJ.run(bfImage, "Enhance Contrast", "saturated=0.35");
IJ.saveAs(bfImage, "Tiff", pathToSave.getAbsolutePath()+File.separator+stack.getTitle());
bfImage.close();
stack.close();
if (normalize) {
	img_temp.close();
}