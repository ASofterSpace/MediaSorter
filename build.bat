IF NOT EXIST ..\Toolbox-Java\ (
	echo "It looks like you did not yet get the Toolbox-Java project - please do so (and put it as a folder next to this folder.)"
	EXIT 1
)

cd src\com\asofterspace

rd /s /q toolbox

md toolbox
cd toolbox

md coders
md configuration
md io
md utils
md web

cd ..\..\..\..

copy "..\Toolbox-Java\src\com\asofterspace\toolbox\*.java" "src\com\asofterspace\toolbox"
copy "..\Toolbox-Java\src\com\asofterspace\toolbox\coders\*.*" "src\com\asofterspace\toolbox\coders"
copy "..\Toolbox-Java\src\com\asofterspace\toolbox\configuration\*.*" "src\com\asofterspace\toolbox\configuration"
copy "..\Toolbox-Java\src\com\asofterspace\toolbox\io\*.*" "src\com\asofterspace\toolbox\io"
copy "..\Toolbox-Java\src\com\asofterspace\toolbox\utils\*.*" "src\com\asofterspace\toolbox\utils"
copy "..\Toolbox-Java\src\com\asofterspace\toolbox\web\*.*" "src\com\asofterspace\toolbox\web"

rd /s /q bin

md bin

cd src

dir /s /B *.java > sourcefiles.list

javac -deprecation -Xlint:all -encoding utf8 -d ../bin @sourcefiles.list

pause
