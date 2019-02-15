#!/bin/bash

set -e
set -o pipefail

JENKINSUID=${JENKINSUID:-1000}
JENKINSGUID=${JENKINSGUID:-1000}

cleanup() {
  chown -R "$JENKINSUID:$JENKINSGUID" tmp log
}
trap "cleanup" SIGINT SIGTERM SIGHUP SIGQUIT EXIT

PROJECT=${PROJECT:-}
OPT_REPO=${OPT_REPO:-}
BUILD_ARCH=${BUILD_ARCH:-}

sed -i '/stty sane/d' /usr/sbin/slackrepo
sed -i '/^PKGBACKUP/d' "/etc/slackrepo/slackrepo_$OPT_REPO.conf"
sed -i "/^LINT/s/'n'/'y'/" "/etc/slackrepo/slackrepo_$OPT_REPO.conf"

if [[ "$BUILD_ARCH" != "" ]] ; then
  sed -i "s/^ARCH.*$/ARCH=$BUILD_ARCH/" "/etc/slackrepo/slackrepo_$OPT_REPO.conf"
fi

# use the sources from the build
CWD="$(pwd)"
(
  cd "/var/lib/slackrepo/$OPT_REPO"
  ln -sf "$CWD" slackbuilds
)

# capture logs in the build
(
  cd /var/log/
  rm -rf slackrepo
  ln -sf "$CWD/log" slackrepo
)

mkdir -p "tmp/$PROJECT"

su -l -c "slackrepo build $PROJECT" | tee "tmp/$PROJECT/build"

sed -i -r "s/\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]//g" "tmp/$PROJECT/build"

LANG=en_US.UTF-8 slackrepo_parse.rb JOB_NAME "tmp/$PROJECT/checkstyle.xml" "tmp/$PROJECT/junit.xml" "tmp/$PROJECT/build"
