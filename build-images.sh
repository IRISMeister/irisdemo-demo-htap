#!/bin/bash
VERSION=`cat ./VERSION`
DOCKER_TAG="version-${VERSION}"

source ./buildtools.sh

build_images() {
PROJECTS_FOLDER=$PWD/$1/projects

for JAR_FILE_WITH_PATH in $(find $PROJECTS_FOLDER -name *iris*.jar -o -name *mysql*.jar -o -name *master*.jar); do
  
	JAR_FILE_ORG=${JAR_FILE_WITH_PATH##*[/]}
	JAR_FILE=${JAR_FILE_ORG%-*}.jar
	IMAGE_NAME=${JAR_FILE%*.jar}-${DOCKER_TAG}

	echo "#" 
	echo "# Found $JAR_FILE_WITH_PATH! Building image $IMAGE_NAME:"; 
	echo "#"

	# We must copy the file to app.jar because that is how
	# the Dockerfile expects it to be called to add it to the image
	echo "#" 
	echo "# Copying $JAR_FILE_WITH_PATH to $PROJECTS_FOLDER/app.jar so that the image can use it..."; 
	echo "#"

	cp -f $JAR_FILE_WITH_PATH $PROJECTS_FOLDER/app.jar
	exit_if_error "Could not copy file $JAR_FILE_WITH_PATH to $PROJECTS_FOLDER/app.jar"

	IMAGE_FULL_NAME=dpmeister/irisdemo-demo-htap:${IMAGE_NAME}
	docker build --build-arg VERSION=${DOCKER_TAG} -t ${IMAGE_FULL_NAME} ./$1
	exit_if_error "build of ${IMAGE_NAME} failed."

	echo ${IMAGE_FULL_NAME} >> ./images_built

	rm $PROJECTS_FOLDER/app.jar		

	echo ""
	echo "---------------------------------------------------------------------------"
	echo "END building image ${IMAGE_NAME}..."
	echo "---------------------------------------------------------------------------"
	echo ""

done

}

# funtion build_java_project will add a line with the full image name of each image built
# But we need to start with an empty file:
rm -f ./images_built

build_images "image-master"
build_images "image-ingest-worker"
build_images "image-query-worker"

##UI_IMAGE_NAME=intersystemsdc/irisdemo-demo-htap:ui-${DOCKER_TAG}
##docker build -t $UI_IMAGE_NAME ./image-ui
##echo $UI_IMAGE_NAME >> ./images_built



