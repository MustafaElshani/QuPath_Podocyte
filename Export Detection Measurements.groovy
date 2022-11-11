def name = getProjectEntry().getImageName() + ' Detec.txt'
def path = buildFilePath(PROJECT_BASE_DIR, 'Detection Results')
mkdirs(path)
path = buildFilePath(path, name)
saveDetectionMeasurements(path)
print 'Results exported to ' + path
