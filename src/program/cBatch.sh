echo start...
pwd

#setting libs path 
libDir=lib/* 
temp=.:

append(){ 
                temp=$temp":"$1 
} 

for file in $libDir;    do 
    append $file 
done 

#javac -encoding MS950 -classpath $temp  program/IJatool.java
#javac -encoding MS950 -classpath $temp  program/Jatool.java
javac -encoding UTF8 -classpath $temp  program/suspendGPRS.java
javac -encoding UTF8 -classpath $temp  program/resumeFunction.java
javac -encoding UTF8 -classpath $temp  program/DVRSmain.java
echo finished


