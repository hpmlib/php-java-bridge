m4_include(tests.m4/function_checks.m4)
m4_include(tests.m4/threads.m4)
m4_include(tests.m4/java_check_broken_stdio_buffering.m4)
m4_include(tests.m4/java_check_struct_ucred.m4)
m4_include(tests.m4/java_check_abstract_namespace.m4)
m4_include(tests.m4/java_check_broken_gcc_installation.m4)
m4_include(tests.m4/java_check_jni.m4)
m4_include(tests.m4/fast_tcp_sockets.m4)

PHP_ARG_WITH(java, for java support,
[  --with-java[[=JAVA_HOME[[,JRE_HOME]]]] 
                          include java support. If JRE_HOME is specified, 
                          the run-time java location will be compiled into 
                          the binary. Otherwise the java executable will 
                          be searched using the PATH environment variable
                          Example: --with-java=/opt/jdk1.4,/usr/java/jre1.6])
AC_ARG_WITH(mono,  [  --with-mono[[=/path/to/ikvmc.exe]] 
                          include mono support
                          Example: --with-mono=$HOME/bin/ikvmc.exe], PHP_MONO="$withval", PHP_MONO="no")
PHP_ARG_ENABLE(servlet, for java servlet support, [  --enable-servlet[[=JAR]]
                          include java servlet support. JAR must be the 
                          location of j2ee.jar or servlet.jar; creates 
                          JavaBridge.war])
PHP_ARG_ENABLE(script, for java script support, [  --enable-script[[=JAR]] 
                          include java script support. If you use a 
                          JDK < 1.6 JAR must be the location of 
                          script-api.jar; creates php-script.jar])
PHP_ARG_ENABLE(faces, for java server faces support, [  --enable-faces[[=JAR]]
                          include java server faces support. JAR must be 
                          the location of jsf-api.jar; creates 
                          php-faces.jar])
AC_ARG_ENABLE(backend, [  --disable-backend] 
                          do not create the JavaBridge.jar back-end, PHP_BACKEND="$enableval", PHP_BACKEND="yes")


if test "$PHP_JAVA" != "no" || test "$PHP_MONO" != "no"  ; then
       JAVA_FUNCTION_CHECKS
       PTHREADS_CHECK
       PTHREADS_ASSIGN_VARS
       PTHREADS_FLAGS
       JAVA_CHECK_BROKEN_STDIO_BUFFERING
       JAVA_CHECK_ABSTRACT_NAMESPACE
       JAVA_CHECK_STRUCT_UCRED
       CHECK_FAST_TCP_SOCKETS

# find includes eg. -I/opt/jdk1.4/include -I/opt/jdk1.4/include/linux
        if test "$PHP_JAVA" != "yes"; then
         # --with-java=/opt/compiletime/jdk,/usr/runtime/jre
         PHP_JRE="`echo $PHP_JAVA | LANG=C awk -F, '{print $2}'`"
         PHP_JAVA="`echo $PHP_JAVA | LANG=C awk -F, '{print $1}'`"

	 JAVA_INCLUDES=`for i in \`find $PHP_JAVA/include -follow -type d -print\`; do echo -n "-I$i "; done`
	 PHP_EVAL_INCLINE($JAVA_INCLUDES)
 	 JAVA_CHECK_JNI
	 COND_GCJ=0
	else
	 JAVA_CHECK_JNI
	 COND_GCJ=1
	fi

        if test "$PHP_MONO" != "no";then 
# create mono.so, compile with -DEXTENSION_DIR="\"$(EXTENSION_DIR)\""
	PHP_NEW_EXTENSION(mono, php_java_snprintf.c java.c java_bridge.c client.c parser.c protocol.c bind.c init_cfg.c ,$ext_shared,,[-DEXTENSION_DIR=\"\\\\\"\\\$(EXTENSION_DIR)\\\\\"\"])
          EXTENSION_NAME=MONO
	  PHP_JAVA_BIN="/usr/bin/mono"
	  COND_GCJ=0
          PHP_JAVA=${EXTENSION_DIR}
          PHP_JRE=${EXTENSION_DIR}
        else 
# create java.so, compile with -DEXTENSION_DIR="\"$(EXTENSION_DIR)\""
	PHP_NEW_EXTENSION(java, php_java_snprintf.c php_java_strtod.c java.c java_bridge.c client.c parser.c protocol.c bind.c init_cfg.c ,$ext_shared,,[-DEXTENSION_DIR=\"\\\\\"\\\$(EXTENSION_DIR)\\\\\"\"])
          EXTENSION_NAME=JAVA
	  if test X$PHP_JRE = X; then
		  PHP_JAVA_BIN="java"
	  else
		  PHP_JAVA_BIN="${PHP_JRE}/bin/java"
          fi
        fi

       JAVA_CHECK_BROKEN_GCC_INSTALLATION
       if test "$have_broken_gcc_installation" = "yes"; then
         AC_MSG_WARN([Your GCC installation may be broken. It uses two different static libraries but only one dynamic library for -m32 and -m64 builds.])
	  sleep 10
       fi

# create init_cfg.c from the template (same as AC_CONFIG_FILES)
	BRIDGE_VERSION="`cat $ext_builddir/VERSION`"
        for i in init_cfg.c init_cfg.h; do 
	  sed "s*@PHP_JAVA@*${PHP_JRE}*
	     s*@COND_GCJ@*${COND_GCJ}*
             s*@PHP_JAVA_BIN@*${PHP_JAVA_BIN}*
             s*@EXTENSION@*${EXTENSION_NAME}*
             s*@BRIDGE_VERSION@*${BRIDGE_VERSION}*" \
            <$ext_builddir/${i}.in >$ext_builddir/${i}
        done

if test "$PHP_BACKEND" = "yes" ; then
# bootstrap the server's configure script
	if test -d ext/java/server; then
	    AC_CONFIG_SUBDIRS(ext/java/server)
        else
	    AC_CONFIG_SUBDIRS(server)
        fi
        for i in ${ext_builddir}/server/configure.gnu security/php-java-bridge.fc security/update_policy.sh; do
          sed "s*@EXTENSION_DIR@*${EXTENSION_DIR}*
               s*@phplibdir@*`pwd`/modules*" \
            <${i}.in >${i}
        done

# an artificial target so that the server/ part gets compiled
	PHP_ADD_MAKEFILE_FRAGMENT
	PHP_SUBST(JAVA_SHARED_LIBADD)
	PHP_MODULES="$PHP_MODULES \$(phplibdir)/libnatcJavaBridge.la"
else 
        if test "$have_fast_tcp_socket" = "no"; then
           echo "Fast TCP sockets not available on this OS, J2EE back-end not available."
           echo "Build the back-end with unix domain sockets instead (requires that your OS supports JNI)."
        fi
fi

fi
