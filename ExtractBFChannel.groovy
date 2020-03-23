#@ImagePlus(label = "Multichannel stack with BrightField image and Fluorescence") stack
#@Integer(label="Channel Index for BrightField Image", value = 3 ) bfChannel
#@output ImagePlus bfImage
#@Boolean(label="Normalize Bright Field image") normalize

import ij.IJ
import ij.plugin.ImageCalculator;

// Extract brightfield channel
IJ.run(stack, "Duplicate...", "duplicate channels="+bfChannel);
IJ.run("Grays");
bfImage = IJ.getImage();
IJ.run(bfImage, "Enhance Contrast", "saturated=0.35");
bfImage.setTitle(stack.getTitle()+"-BF");
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
if (normalize) {
	img_temp.close();
}