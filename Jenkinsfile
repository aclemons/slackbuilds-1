#!/usr/bin/env groovy

// Copyright 2019 Andrew Clemons, Wellington New Zealand
// All rights reserved.
//
// Redistribution and use of this script, with or without modification, is
// permitted provided that the following conditions are met:
//
// 1. Redistributions of this script must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
//  THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
//  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO
//  EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
//  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
//  OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
//  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
//  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

node('master') {
    stage('build') {
        checkout scm

        def userId = sh(returnStdout: true, script: '#!/bin/sh -e\nid -u jenkins').trim()
        def groupId = sh(returnStdout: true, script: '#!/bin/sh -e\nid -g jenkins').trim()

        dir ('log') {
          sh '#!/bin/sh -e\necho "Created log directory: $(pwd)"'
        }

        def packageName = env.PACKAGE_NAME
        if (packageName == null) {
            packageName = ''
        }

        def buildArch = env.BUILD_ARCH
        if (buildArch == null) {
            buildArch = ''
        }

        def optRepo = env.OPT_REPO

        if (optRepo == null) {
            optRepo = 'SBo'
        }

        docker.image(env.SLACKREPO_DOCKER_IMAGE).inside("-u 0 --privileged -v ${env.SLACKREPO_DIR}:/var/lib/slackrepo/${optRepo} -v ${env.SLACKREPO_SOURCES}:/var/lib/slackrepo/${optRepo}/source") {
            ansiColor('xterm') {
                try {
                    sh """#!/bin/bash
set -e
set -o pipefail

sed -i '/^PKGBACKUP/d' /etc/slackrepo/slackrepo_${optRepo}.conf
sed -i "/^LINT/s/'n'/'y'/" /etc/slackrepo/slackrepo_${optRepo}.conf

if [[ "${buildArch}" != "" ]] ; then
  sed -i "s/^ARCH.*\$/ARCH=${buildArch}/" /etc/slackrepo/slackrepo_${optRepo}.conf
fi

# use the sources from the build
CWD="\$(pwd)"
(
  cd /var/lib/slackrepo/${optRepo}
  ln -sf "\$CWD" slackbuilds
)

# capture logs in the build
(
  cd /var/log/
  rm -rf slackrepo
  ln -sf "\$CWD/log" slackrepo
)

mkdir -p tmp
su -l -c 'slackrepo build ${packageName}' | tee tmp/build

sed -i -r "s/\\x1B\\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]//g" tmp/build
"""
                } finally {
                    sh "#!/bin/sh -e\nchown -R ${userId}:${groupId} tmp log"
                }
            }

            archiveArtifacts artifacts: 'tmp/*, log/**/*'

            try {
                sh """#!/bin/bash
set -e
set -o pipefail

LANG=en_US.UTF-8 slackrepo_parse.rb ${env.JOB_NAME} tmp/checkstyle.xml tmp/junit.xml tmp/build
"""

            } finally {
                sh "#!/bin/sh -e\nchown -R ${userId}:${groupId} tmp log"
            }
        }

        junit 'tmp/junit.xml'
        recordIssues blameDisabled: true, enabledForFailure: true, tools: [checkStyle(id: 'slackrepo', name: 'Slackrepo', pattern: 'tmp/checkstyle.xml')]
    }
}
