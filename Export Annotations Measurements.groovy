def name = getProjectEntry().getImageName() + ' Anno.txt'
def path = buildFilePath(PROJECT_BASE_DIR, 'Annotation Results')
mkdirs(path)
path = buildFilePath(path, name)
saveAnnotationMeasurements(path)
print 'Results exported to ' + path
