#!/usr/bin/env sh

# APP_BASE_NAME'i ayarla
APP_BASE_NAME=`basename "$0"`

# Geçerli dizini al
DIRNAME=`dirname "$0"`
[ -z "\$DIRNAME" ] && DIRNAME="."
APP_HOME=`cd "$DIRNAME" >/dev/null; pwd`

# Gradle'ı çalıştıracak java komutunu bul
if [ -n "\$JAVA_HOME" ] ; then
    JAVACMD="\$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Eğer gradle-wrapper.jar yoksa indir veya Gradle'ı tetikle
exec "\$JAVACMD" -jar "\(APP_HOME/app/build.gradle.kts" "\)@"
