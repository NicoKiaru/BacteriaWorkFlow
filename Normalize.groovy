#@ImagePlus img_in
#@output ImagePlus img_out

import ij.IJ;
import ij.plugin.ImageCalculator;
IJ.run(img_in,"Duplicate...", " ");
img_blur = IJ.getImage();
IJ.run(img_blur,"Gaussian Blur...", "sigma=200");
ic = new ImageCalculator();
img_out = ic.run("Divide create 32-bit", img_in, img_blur);
img_blur.changes=false;
img_blur.close();

