#!/bin/sh
#web程式呼叫時會設定在tomcat/bin
echo '第1版'
pwd
cd ../webapps/RFP_Struts/WEB-INF/classes/
pwd
#echo "`ls -altr`"

programdir="." 

num=$# 
temp=$CLASSPATH 
#setting libs path 
libs=../lib/* 
append(){ 
                temp=$temp":"$1 
} 
for file in $libs;    do 
                append $file 
done 
export CLASSPATH=$temp:.:../:$programdir 
#export LANG=zh_CN 
#nohup java -classpath $CLASSPATH    program.hello #&
java -classpath $CLASSPATH    program.hello
echo "finished"
