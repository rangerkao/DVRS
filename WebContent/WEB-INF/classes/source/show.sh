#!/bin/sh
#此腳本為linux下的自動執行腳本，可以作為開機自動執行腳本調用
#也可以作為獨立啟動JAVA程式使用
#
#http://www.cnblogs.com/baibaluo/archive/2011/08/31/2160934.html
###################################
#?境?量及程序?行??
#需要根据???境以及Java程序名??修改?些??
###################################
#JDK所在路徑
JAVA_HOME="/usr/java/jdk1.7.0_51"

#執行程序使用者，為了安全建議不使用root
RUNNING_USER=root

#Java程序所在位置，classes上一層
APP_HOME=/root/apache-tomcat-7.0.50/webapps/DVRS/WEB-INF/

#需要啟動的java（main方法（main方法?）
APP_MAINCLASS=program.RFPmain

#拼出完整的classPath，包括指定lib目錄下的所有jar
CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar;do
CLASSPATH="$CLASSPATH":"$i"
done

#java虛擬機啟動參數
JAVA_OPTS="-ms512m -mx512m -Xmn256m -Djava.awt.headless=true -XX:MaxPermSize=128m"

###################################
#(函數)判斷程序是否已啟動
#
#說明
#使用JDK自帶的JPS命令及grep命令組合，准确查找pid
#jps 加 l 參數，表示顯示java的完整包路?
#使用awk，分割出pid ($1部分)，及Java程序名?($2部分)
###################################
#初始化psid變量（全局）
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
#(函數)啟動程序
#
#說明：
#1. 首先起用checkpid函數，刷新$psid全局?量
#2. 如果程序已經啟動（$psid不等于0），則提示程序已啟動
#3. 如果程序沒有被啟動，則執行啟動命令行
#4. 啟動命令執行后，再次調用checkpid函數
#5. 如果步驟4的結果能確認程序的pid,則打印[OK]，否則打印[Failed]
#注意：echo -n 表示打印字符后，不換行
#注意: "nohup 某命令 >/dev/null 2>&1 &" 的用法
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
#(函數)停止程序
#
#說明：
#1. 首先起用checkpid函數，刷新$psid全局變量
#2. 如果程序已啟動（$psid不等于0），則開始直行停止，否則，提示程序未運行
#3. 使用kill -9 pid命令進行強制殺死進程
#4. 運行kill命令行緊接其后，馬上查看上一句命令的返回值: $?
#5. 如果步驟4的結果$?等于0,?打印[OK]，否則打印[Failed]
#6. 為了防止java程序被啟動多次，這里增加反复檢查進程，反复殺死的處理（運行調用用stop）。
#注意：echo -n 表示打印字符后，不?行
#注意: 在shell?程中，"$?" 表示上一句命令或者一個函數的返回值
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
#(函數)調查程序運行狀態
#
#說明：
#1. 首先調用checkpid函數，刷新$psid全局?量
#2. 如果程序已經啟動（$psid不等于0），則提示正在運行并表示出pid
#3. 否則，提示程序未運行
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
#(函?)打印系統還境參數
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
#讀取腳本的第一個參數($1)，進行判斷
#參數取值範圍：{start|stop|restart|status|info}
#如參數不在指定範圍之內，則打印幫助信息
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
