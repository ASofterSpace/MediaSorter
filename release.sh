#!/bin/bash

echo "Re-building with target Java 7 (such that the compiled .class files will be compatible with as many JVMs as possible)..."

cd src

# build build build!
javac -encoding utf8 -d ../bin -bootclasspath ../other/java7_rt.jar -source 1.7 -target 1.7 @sourcefiles.list

cd ..



echo "Creating the release file MediaSorter.zip..."

mkdir release

cd release

mkdir MediaSorter

# copy the main files
cp -R ../bin MediaSorter
cp ../UNLICENSE MediaSorter
cp ../README.md MediaSorter
cp ../run.sh MediaSorter
cp ../run.bat MediaSorter

# convert \n to \r\n for the Windows files!
cd MediaSorter
awk 1 ORS='\r\n' run.bat > rn
mv rn run.bat
cd ..

# create a version tag right in the zip file
cd MediaSorter
version=$(./run.sh --version_for_zip)
echo "$version" > "$version"
cd ..

# zip it all up
zip -rq MediaSorter.zip MediaSorter

mv MediaSorter.zip ..

cd ..
rm -rf release

echo "The file MediaSorter.zip has been created in $(pwd)"
