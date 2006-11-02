#!/bin/sh
# Installation script for the PHP/Java Bridge module and its back-ends.

#set -x
v=""
if test "X$1" = "X--verbose" || test "X$1" = "X-v" ; then
v="-v"
fi

if ! test -d modules; then
  echo "Nothing to install. Do a phpize && configure && make first, bye."
  exit 10
fi

php=`php-config --php-binary`
if test $? != 0; then php="`which php`"; fi
if test $? != 0; then php="`which php-cgi`"; fi
if test $? != 0; then 
 for i in /usr/bin/php /usr/bin/php-cgi /usr/local/bin/php /usr/local/bin/php-cgi; do
  if test -f $i; then php=$i; break; fi
 done
fi

if test X$php = X; then echo "php not installed, bye."; exit 3; fi

echo '<?php phpinfo();?>' | ${php} >/tmp/phpinfo.$$
ini=`fgrep "Scan this dir for additional" /tmp/phpinfo.$$ | head -1 |
sed 's/<[TtRr][/a-z ="0-9]*>/ => /g' | 
sed 's/<[/a-z ="0-9]*>//g' | 
sed 's/^.*=> //' | sed 's/ *$//; s/^ *//'`
ext=`fgrep extension_dir /tmp/phpinfo.$$ | head -1 |
sed 's/<[TtRr][/a-z ="0-9]*>/ => /g' | 
sed 's/<[/a-z ="0-9]*>//g' | 
sed 's/^.*=> //' | sed 's/ *$//; s/^ *//'`
phpini=`fgrep "Configuration File (php.ini) Path" /tmp/phpinfo.$$ | head -1 |
sed 's/<[TtRr][/a-z ="0-9]*>/ => /g' | 
sed 's/<[/a-z ="0-9]*>//g' | 
sed 's/^.*=> //' | sed 's/ *$//; s/^ *//'`
phpinc=`fgrep "include_path" /tmp/phpinfo.$$ | head -1 |
sed 's/<[TtRr][/a-z ="0-9]*>/ => /g' | 
sed 's/<[/a-z ="0-9]*>//g' | 
sed 's/^.*=> //' | sed 's/ *$//; s/^ *//' |
LANG=C awk '{for(i=1; i<=(split($0, ar, "[;:]")); i++) if(substr(ar[i], 1) != "." && substr(ar[i], 1) != "..") print ar[i] }' |
head -1`
/bin/rm -f /tmp/phpinfo.$$
if test -d $phpini; then phpini="${phpini}/php.ini"; fi

# install generic ini
make install >install.log || exit 1
mod_dir=`/bin/cat install.log | sed -n '/Installing shared extensions:/s///p' | awk '{print $1}'`
if test X$v != X; then echo "installed in $mod_dir"; fi

overwrite="no"
if test X$ini = X; then
echo ""
echo "This php installation does not have a config-file-scan-dir,"
echo "java.ini and java-servlet.ini not installed,"
echo "will use file $phpini instead."
if test -f ${phpini}; then
   echo -n "Overwrite existing php.ini file (yes/no): "; read overwrite;
else
   overwrite="yes"
fi
fi

if test -d "$ext"; then 
if test X$v != X; then echo "Using extension_dir: $ext"; fi
else
echo ""
echo "Warning: Your php installation is broken, the \"extension_dir\" does "
echo "not exist or is not a directory (see php.ini extension_dir setting)."
echo "Please correct this, for example type:"
echo "mkdir \"$ext\""
echo ""
fi

/bin/rm -f $ini 2>/dev/null
if test X$ini != X; then
 /bin/mkdir -p $ini
 /bin/cp $v java.ini $ini
else
  if test X$overwrite = "Xyes"; then
    if test -f $phpini; then
      /bin/rm $v -f "${phpini}.backup"
      /bin/mv $v "$phpini" "${phpini}.backup"
    fi
    echo "include_path=/usr/share/pear:." >"$phpini"
    /bin/cat java.ini >>"$phpini"
  fi
fi

if test 1 = 1; then
jre=modules/java
else
jre=java
sdk_home=/home/users/j/jo/jost2345/PHP-JAVA-BRIDGE-amd64/j2re1.4.2.amd64
 if ! $jre -classpath modules/JavaBridge.jar php.java.bridge.JavaBridge --version >/dev/null; then
    echo "Default JRE: $jre is not usuable."
    echo "You have configured against ${sdk_home} but the installed JRE is:"
    echo ""
    echo "  `$jre -version|head -5`"
    echo ""
    echo "Please configure as follows:"
    echo "  ./configure --with-java=$sdk_home,$sdk_home"
    echo "to avoid this warning -- the jre version must be >= the jdk version.";
    echo "Ignoring $jre and using ${sdk_home}/bin/java instead."
    jre=${sdk_home}/bin/java
    echo "Please either use the above ./configure command or add" 
    echo "  java.java=$jre"
    echo "to your php .ini file."
 fi
fi

# devel
/bin/rm $v -f /usr/share/java/JavaBridge.jar /usr/share/java/script-api.jar /usr/share/java/php-script.jar /usr/java/packages/lib/ext/JavaBridge.jar /usr/java/packages/lib/ext/php-script.jar
if test -f modules/php-script.jar; then
    echo ""
    echo "Do you want to install the development files for jdk1.6?";
    echo -n "install development files (yes/no): "; read devel;
    if test "X$devel" != "Xno"; then
	/bin/mkdir -p /usr/share/java 2>/dev/null

	/bin/cp $v modules/JavaBridge.jar \
	      modules/php-script.jar \
	      modules/script-api.jar /usr/share/java

	/bin/mkdir -p /usr/java/packages/lib/ext 2>/dev/null

	/bin/rm -f /usr/java/packages/lib/ext/JavaBridge.jar \
	      /usr/java/packages/lib/ext/php-script.jar 2>/dev/null

	/bin/ln $v -s /usr/share/java/JavaBridge.jar \
	         /usr/share/java/php-script.jar /usr/java/packages/lib/ext

	echo "Installed in /usr/share/java"
	echo ""
        echo "The development files have been installed on this computer."
	echo "Type (e.g.) /usr/java/jdk1.6.0/bin/jrunscript -l php-interactive"
	echo "to start an interactive php session."
	echo ""
      fi
fi

# convert Java -> PHP
if test X$phpinc = X; then phpinc="/usr/share/pear"; fi
echo "";
echo "Generate PHP classes from standard Java classes and install them in"
echo "the ${phpinc} directory?"
echo -n "convert java libraries (yes/no): "; read convert;
if ! test "X$convert" = "Xno"; then
$jre -Djava.library.path=./modules -Djava.class.path=./modules/JavaBridge.jar -Djava.awt.headless=true -Dphp.java.bridge.base=./modules php.java.bridge.JavaBridge --convert ${phpinc} unsupported/lucene.jar:unsupported/itext.jar
/bin/cp java/*.php ${phpinc}/java
echo ""
fi

check_document_root() {
    document_root="/var/www/html";
    if ! test -d /var/www/html; then
	document_root="/usr/local/var/www/html";
	echo ""
	echo "Enter the location of HTTP server document root (e.g.: /usr/local/htdocs)";
	echo -n "document root ($document_root): "; read document_root;
	if test X$document_root = X; then document_root="/usr/local/var/www/html";fi
	echo ""
	if ! test -d $document_root; then echo "$document_root is not directory"; exit 5; fi
    fi
}

install_sel_module() {
 (
	cd security/module; 
	/usr/bin/make; 
	/usr/sbin/semodule -i php-java-bridge.pp
	/usr/bin/chcon -t javabridge_exec_t $ext/RunJavaBridge
	/usr/bin/chcon -t bin_t $ext/java
	if test X$javabridgeservice != X; then /usr/bin/chcon -t initrc_exec_t $javabridgeservice; fi
 )
}

j2ee=no
# j2ee/servlet
/bin/rm $v -f ${ini}/java-servlet.ini 2>/dev/null
if test -f /etc/selinux/config && test -f /usr/sbin/semodule; then 
    /usr/sbin/semodule -r javabridge 2>/dev/null
    /usr/sbin/semodule -r javabridge_tomcat 2>/dev/null
fi
if test -f modules/JavaBridge.war; then
    echo ""
    echo "Do you want to start the Servlet/J2EE back-end as a service?";
    echo "Say no, if you want to start the service automatically with Apache."
    echo "If unsure, say no";
    echo -n "install j2ee back-end (yes/no): "; read j2ee;
    if test "X$j2ee" != "Xno"; then
      webapps="`locate /webapps | fgrep tomcat | grep 'webapps$' | head -1`"
      echo ""
      echo "Enter the location of the autodeploy folder (e.g.: /opt/tomcat/webapps):";
      echo -n "autodeploy ($webapps): "; read webapps2;
      if test X$webapps2 != X; then webapps=$webapps2; fi
      /bin/rm $v -rf ${webapps}/JavaBridge.war ${webapps}/JavaBridge
      /bin/cp $v modules/JavaBridge.war $webapps;
      echo "Installed in $webapps.";
      check_document_root || exit 5
      if test -d ${document_root} && ! test -e ${document_root}/JavaBridge; then
        ln -s $webapps/JavaBridge ${document_root}/;
        echo "Installed in ${document_root}. "
      fi
      if test X$ini != X; then  
	  /bin/cp $v java-servlet.ini $ini; 
      else
	  if test X$overwrite = "Xyes"; then
	      /bin/cat java-servlet.ini >>"$phpini"; 
	  fi
      fi
      if test -f /etc/selinux/config && test -f /usr/sbin/semodule; then 
        if test X$v = X; then
            install_sel_module >/dev/null
	    (cd security/module; /usr/bin/make; /usr/sbin/semodule -i php-java-bridge-tomcat.pp) >/dev/null
	else
            install_sel_module;
	    (cd security/module; /usr/bin/make; /usr/sbin/semodule -i php-java-bridge-tomcat.pp) 
	fi
      fi
      echo ""
      echo "The J2EE back-end has been desployed into the J2EE server. Now start the J2EE"
      echo "server and the web server, for example with the commands (Linux):"
      echo ""
      echo "    service tomcat5 restart"
      echo "    service httpd restart"
      echo ""
      echo " or (Unix):"
      echo ""
      echo "    JAVA_HOME=$sdk_home `dirname $webapps`/bin/catalina.sh stop"
      echo "    JAVA_HOME=$sdk_home `dirname $webapps`/bin/catalina.sh start"
      echo "    apachectl restart"
      echo ""
     
      echo "Browse to http://localhost:<PORT>/JavaBridge to see the test page from the"
      echo "J2EE server. PORT is 8080 for tomcat, 9080 for WebSphere and 8888 for Oracle."
      echo "Browse to http://localhost/JavaBridge to see the test page from the web server."
      echo ""
    fi
else
    install_sel_module;
fi

echo "PHP/Java Bridge installed."
if test -d /etc/selinux; then
if /usr/sbin/selinuxenabled; then
  if test -f /etc/selinux/config && test -f /usr/sbin/semodule; then
    echo "SEL Security: \"javabridge\" policy module installed."
  fi
else
  echo "You are running a SELinx system. Please install the policy sources"
  echo "or install the files from the RPM distribution download."
  echo "Please see the README document for details".
fi
fi

echo ""
echo "Now type \"${php} test.php\" to check the installation."
echo ""

exit 0
