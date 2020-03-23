#@ImagePlus imp (label="Input Image")
#@output ImagePlus imgLabel

import ij.*
import ij.gui.*
import ij.process.*
import inra.ijpb.binary.*
import inra.ijpb.morphology.strel.*
import inra.ijpb.morphology.*
import ij.plugin.ImageCalculator
import ij.plugin.frame.RoiManager


IJ.run(imp,"Duplicate...", " ");
bact = IJ.getImage();
IJ.run(bact,"Macro...", "code=v=(v!=1)*1"); //Assumes label 1 = background, rest is bacteria (seed and border)

IJ.run(imp,"Duplicate...", " ");
seeds = IJ.getImage();
IJ.run(seeds,"Macro...", "code=v=(v==3)*255"); // Assumes label 3 = seeds
IJ.run(seeds,"Median...", "radius=1"); // Filters a bit nucleus to avoid spurious isolated cells


def voronoi = seeds.duplicate()
// Get Voronoi
IJ.run(voronoi, "Voronoi", "")
//voronoi.show()
voronoi.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE)

voronoi.setProcessor(voronoi.getProcessor().createMask())
voronoi.getProcessor().invert()

// Label Voronoi and store
def voronoi_labels = BinaryImages.componentsLabeling(voronoi, 8, 16)
voronoi_labels.show();

IJ.run(seeds,"16-bit","");
IJ.run(bact,"16-bit","");
IJ.run("Calculator Plus", "i1=["+voronoi_labels.getTitle()+"] i2=["+bact.getTitle()+"] operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");

imgLabel = IJ.getImage();

voronoi_labels.close();
seeds.changes = false; 
seeds.close();
bact.changes = false; 
bact.close();

imgLabel.setTitle(imp.getTitle().substring(0,imp.getTitle().size()-5)+"-lblImage");

