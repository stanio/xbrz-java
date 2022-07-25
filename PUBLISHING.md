## [Deploying to OSSRH with Apache Maven](https://central.sonatype.org/publish/publish-maven/)

## [Performing a Release Deployment](https://central.sonatype.org/publish/publish-maven/#performing-a-release-deployment)

    mvn versions:set -DnewVersion=1.2.3

    git commit -m "Set stable 1.2.3 version for release"

    git tag 1.2.3 -m "Release 1.2.3 to Maven Central"

    mvn clean deploy -P release

## [Releasing Deployment from OSSRH to the Central Repository](https://central.sonatype.org/publish/release/)

-   [Locate and Examine Your Staging Repository](https://central.sonatype.org/publish/release/#locate-and-examine-your-staging-repository)
-   [Close and Drop or Release Your Staging Repository](https://central.sonatype.org/publish/release/#close-and-drop-or-release-your-staging-repository)

## Update site Apidocs

    mvn clean package -P apidoc javadoc:aggregate-jar

    rm -rf docs/apidocs/
    cp -r target/apidocs/ docs/
    git add docs/apidocs/
    git commit
