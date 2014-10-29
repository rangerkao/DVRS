#!/bin/sh
#���}����linux�U���۰ʰ���}���A�i�H�@���}���۰ʰ���}���ե�
#�]�i�H�@���W�߱Ұ�JAVA�{���ϥ�
#
#http://www.cnblogs.com/baibaluo/archive/2011/08/31/2160934.html
###################################
#?��?�q�ε{��?��??
#�ݭn���u???�ҥH��Java�{�ǦW??�ק�?��??
###################################
#JDK�Ҧb���|
JAVA_HOME="/usr/java/jdk1.7.0_51"

#����{�ǨϥΪ̡A���F�w����ĳ���ϥ�root
RUNNING_USER=root

#Java�{�ǩҦb��m�Aclasses�W�@�h
APP_HOME=/root/apache-tomcat-7.0.50/webapps/DVRS/WEB-INF/

#�ݭn�Ұʪ�java�]main��k�]main��k?�^
APP_MAINCLASS=program.RFPmain

#���X���㪺classPath�A�]�A���wlib�ؿ��U���Ҧ�jar
CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar;do
CLASSPATH="$CLASSPATH":"$i"
done

#java�������ҰʰѼ�
JAVA_OPTS="-ms512m -mx512m -Xmn256m -Djava.awt.headless=true -XX:MaxPermSize=128m"

###################################
#(���)�P�_�{�ǬO�_�w�Ұ�
#
#����
#�ϥ�JDK�۱a��JPS�R�O��grep�R�O�զX�A���̬d��pid
#jps �[ l �ѼơA������java������]��?
#�ϥ�awk�A���ΥXpid ($1����)�A��Java�{�ǦW?($2����)
###################################
#��l��psid�ܶq�]�����^
psid=0

checkpid() {
   javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAINCLASS`
 
   if [ -n "$javaps" ]; then
      psid=`echo $javaps | awk '{print $1}'`
   else
      psid=0
   fi
}
 
###################################
#(���)�Ұʵ{��
#
#�����G
#1. �����_��checkpid��ơA��s$psid����?�q
#2. �p�G�{�Ǥw�g�Ұʡ]$psid�����_0�^�A�h���ܵ{�Ǥw�Ұ�
#3. �p�G�{�ǨS���Q�ҰʡA�h����ҰʩR�O��
#4. �ҰʩR�O����Z�A�A���ե�checkpid���
#5. �p�G�B�J4�����G��T�{�{�Ǫ�pid,�h���L[OK]�A�_�h���L[Failed]
#�`�N�Gecho -n ��ܥ��L�r�ŦZ�A������
#�`�N: "nohup �Y�R�O >/dev/null 2>&1 &" ���Ϊk
###################################
start() {
   checkpid
 
   if [ $psid -ne 0 ]; then
      echo "================================"
      echo "warn: $APP_MAINCLASS already started! (pid=$psid)"
      echo "================================"
   else
      echo -n "Starting $APP_MAINCLASS ..."
      JAVA_CMD="nohup $JAVA_HOME/bin/java $JAVA_OPTS -classpath $CLASSPATH $APP_MAINCLASS >/dev/null 2>&1 &"
      su - $RUNNING_USER -c "$JAVA_CMD"
      checkpid
      if [ $psid -ne 0 ]; then
         echo "(pid=$psid) [OK]"
      else
         echo "[Failed]"
      fi
   fi
}
 
###################################
#(���)����{��
#
#�����G
#1. �����_��checkpid��ơA��s$psid�����ܶq
#2. �p�G�{�Ǥw�Ұʡ]$psid�����_0�^�A�h�}�l���氱��A�_�h�A���ܵ{�ǥ��B��
#3. �ϥ�kill -9 pid�R�O�i��j������i�{
#4. �B��kill�R�O��򱵨�Z�A���W�d�ݤW�@�y�R�O����^��: $?
#5. �p�G�B�J4�����G$?���_0,?���L[OK]�A�_�h���L[Failed]
#6. ���F����java�{�ǳQ�Ұʦh���A�o���W�[���`�ˬd�i�{�A���`�������B�z�]�B��եΥ�stop�^�C
#�`�N�Gecho -n ��ܥ��L�r�ŦZ�A��?��
#�`�N: �bshell?�{���A"$?" ��ܤW�@�y�R�O�Ϊ̤@�Ө�ƪ���^��
###################################
stop() {
   checkpid
 
   if [ $psid -ne 0 ]; then
      echo -n "Stopping $APP_MAINCLASS ...(pid=$psid) "
      su - $RUNNING_USER -c "kill -9 $psid"
      if [ $? -eq 0 ]; then
         echo "[OK]"
      else
         echo "[Failed]"
      fi
 
      checkpid
      if [ $psid -ne 0 ]; then
         stop
      fi
   else
      echo "================================"
      echo "warn: $APP_MAINCLASS is not running"
      echo "================================"
   fi
}
 
###################################
#(���)�լd�{�ǹB�檬�A
#
#�����G
#1. �����ե�checkpid��ơA��s$psid����?�q
#2. �p�G�{�Ǥw�g�Ұʡ]$psid�����_0�^�A�h���ܥ��b�B��}��ܥXpid
#3. �_�h�A���ܵ{�ǥ��B��
###################################
status() {
   checkpid
 
   if [ $psid -ne 0 ];  then
      echo "$APP_MAINCLASS is running! (pid=$psid)"
   else
      echo "$APP_MAINCLASS is not running"
   fi
}

###################################
#(��?)���L�t���ٹҰѼ�
###################################
info() {
   echo "System Information:"
   echo "****************************"
   echo `head -n 1 /etc/issue`
   echo `uname -a`
   echo
   echo "JAVA_HOME=$JAVA_HOME"
   echo `$JAVA_HOME/bin/java -version`
   echo
   echo "APP_HOME=$APP_HOME"
   echo "APP_MAINCLASS=$APP_MAINCLASS"
   echo "****************************"
}
 
###################################
#Ū���}�����Ĥ@�ӰѼ�($1)�A�i��P�_
#�Ѽƨ��Ƚd��G{start|stop|restart|status|info}
#�p�ѼƤ��b���w�d�򤧤��A�h���L���U�H��
###################################

case "$1" in
   'start')
      start
      ;;
   'stop')
     stop
     ;;
   'restart')
     stop
     start
     ;;
   'status')
     status
     ;;
   'info')
     info
     ;;
   *)
     echo "Usage: $0 {start|stop|restart|status|info}"
    ;;
esac
