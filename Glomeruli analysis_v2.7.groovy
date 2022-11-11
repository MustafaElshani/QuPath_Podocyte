//Version 2.7 Glomerulus Analysis 30/03/2020
//Script is based on manually annotated Glomerulus 
// @author Mustafa Elshani

//Detect cells using Hoescht channel, annnotate Podocyte positive cells based on p57 nuclear staining (FITC Channel), 
selectAnnotations()
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImage": "H3342",  "requestedPixelSizeMicrons": 0.2,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 0.5,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 100.0,  "threshold": 300.0,  "watershedPostProcess": true,  "cellExpansionMicrons": 0.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true}');
///Classiying Cells based on FITC  >2100 Podocyte <2100 OtherGlomerularCell, USING CLASSIFIER
runObjectClassifier("p57Podocytes")
//Use these lines below if sepcific detection are require to be removed such as negative cells 
//removal = getCellObjects().findAll{it.getPathClass().toString().contains("Negative")}
//removeObjects(removal, true)

import qupath.lib.gui.ml.PixelClassifierTools
def annotations = getAnnotationObjects()
def project = getProject()
//Detect and annotate podocyte marker (NQO1) based on trained classfications A750 >2200 NQO1 Positive

def classifier = project.getPixelClassifiers().get('NQO1_Glom_Classifier')
//Classify NQO1 aggregates A750 >4000 NQO1 Positive
def classifier2 = project.getPixelClassifiers().get('NQO1aggregates')

//Define image data
def imageData = getCurrentImageData()

//Convert Nqo1 Positive areas to annotations
PixelClassifierTools.createAnnotationsFromPixelClassifier(imageData, classifier, annotations, 10, 0, false, false)

//Convert NQO1 aggregations to postive are to detection 
PixelClassifierTools.createDetectionsFromPixelClassifier(imageData, classifier2, annotations, 10, 0, true, false)

//Select Glomerulus annoatation 
selectObjects { p -> p.getPathClass() == getPathClass("Glomerulus") }
//This line creates a 0.2Mciron expansion of original Gloumerulus annotation and deletes original, to avoid intersection with child annotations from classfiers above
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 0.2,  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": true}');
clearSelectedObjects(true);
clearSelectedObjects();
resolveHierarchy()

//Add intensity measurements to annotations
selectAnnotations()
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.2,  "region": "ROI",  "tileSizeMicrons": 25.0,  "channel1": false,  "channel2": false,  "channel3": true,  "channel4": true,  "channel5": true,  "doMean": true,  "doStdDev": true,  "doMinMax": true,  "doMedian": true,  "doHaralick": false,  "haralickMin": NaN,  "haralickMax": NaN,  "haralickDistance": 1,  "haralickBins": 32}');
import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathObjects
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathObjectTools
//def imageData = getCurrentImageData() //Don't think its needed as called above
def server = imageData.getServer()
def pixelHeight = server.getPixelCalibration().getPixelHeightMicrons()
def pixelWidth = server.getPixelCalibration().getPixelWidthMicrons()
Set classList = []
for (object in getAllObjects().findAll{it.isAnnotation() || it.isDetection()}) {
    classList << object.getPathClass()
}
println(classList)
hierarchy = getCurrentHierarchy()

//NQO1 Positive classified  area measuremnts 
for (annotation in getAnnotationObjects()){
    def AnnotationArea = annotation.getROI().getArea()

    for (aClass in classList){
        if (aClass){
             
            def tiles = qupath.lib.objects.PathObjectTools.getDescendantObjects(annotation,null,PathAnnotationObject).findAll{it.getPathClass() == getPathClass("Nqo1GlomPos")}
            double TotalArea = 0
    
            for (def tile in tiles){
                TotalArea += tile.getROI().getArea()
    }
annotation.getMeasurementList().putMeasurement("Nqo1GlomPos px",  AnnotationArea)
annotation.getMeasurementList().putMeasurement("Nqo1GlomPos  μm^2",  AnnotationArea*pixelHeight*pixelWidth)
annotation.getMeasurementList().putMeasurement("Nqo1GlomPos  μm^2",  TotalArea*pixelHeight*pixelWidth)
annotation.getMeasurementList().putMeasurement("Nqo1GlomPos area %", TotalArea/AnnotationArea*100)
    }
    }
}
//NQO1 aggregates classified Areas measuremnts 
for (annotation in getAnnotationObjects()){
    def AnnotationArea = annotation.getROI().getArea()

    for (aClass in classList){
        if (aClass){
             
            def tiles = qupath.lib.objects.PathObjectTools.getDescendantObjects(annotation,null,PathDetectionObject).findAll{it.getPathClass() == getPathClass("NQO1aggregates")}
            double TotalArea = 0
    
            for (def tile in tiles){
                TotalArea += tile.getROI().getArea()
    }
annotation.getMeasurementList().putMeasurement("NQO1aggregates px",  AnnotationArea)
annotation.getMeasurementList().putMeasurement("NQO1aggregates μm^2",  AnnotationArea*pixelHeight*pixelWidth)
annotation.getMeasurementList().putMeasurement("NQO1aggregates μm^2",  TotalArea*pixelHeight*pixelWidth)
annotation.getMeasurementList().putMeasurement("NQO1aggregates area %", TotalArea/AnnotationArea*100)
    }
    }
}

//Adds measurements to NQO1 aggregates such as area, perimeter and circularity
selectObjects { p -> p.getPathClass() == getPathClass("NQO1aggregates") && p.isDetection() }
runPlugin('qupath.lib.plugins.objects.ShapeFeaturesPlugin', '{"area": true,  "perimeter": True,  "circularity": True,  "useMicrons": true}');

//Removes NQO1 aggregates with area greater than 15^m2 , This is work in progress
def ANNOTATION_AREA_MICRONS = 15
def LargeObjects = getDetectionObjects().findAll {it.getPathClass().toString().contains("NQO1aggregates") && it.getROI().getScaledArea(pixelWidth, pixelHeight) > ANNOTATION_AREA_MICRONS }
removeObjects(LargeObjects, true)
fireHierarchyUpdate()

println("done")
