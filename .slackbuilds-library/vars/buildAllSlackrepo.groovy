def call() {
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

    def projects = sh(returnStdout: true, script: "find . -type f -mindepth 2 -name '*.SlackBuild' | grep -v README.SlackBuild | xargs -I xx dirname xx | sed 's/\\.\\///' | sort").split()

    echo "Found ${projects.size()} projects to build"

    try {
        projects.each {
            echo "Building ${it}"

            docker.image(env.SLACKREPO_DOCKER_IMAGE).inside("-u 0 --privileged -v ${env.SLACKREPO_DIR}:/var/lib/slackrepo/${optRepo} -v ${env.SLACKREPO_SOURCES}:/var/lib/slackrepo/${optRepo}/source") {
                ansiColor('xterm') {
                    withEnv(["PROJECT=${it}", "JENKINSUID=${userId}", "JENKINSGUID=${groupId}", "BUILD_ARCH=${buildArch}", "OPT_REPO=${optRepo}"]) {
                        sh(returnStatus: true, script: libraryResource('build_with_slackrepo.sh'))
                    }
                }

            }

            junit allowEmptyResults: true, testResults: "tmp/${it}/junit.xml"
        }
    } finally {
        recordIssues blameDisabled: true, enabledForFailure: true, tools: [checkStyle(id: 'slackrepo', name: 'Slackrepo', pattern: 'tmp/**/checkstyle.xml')]
        archiveArtifacts artifacts: 'log/**/*'
    }
}
