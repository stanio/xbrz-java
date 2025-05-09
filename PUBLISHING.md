## [Publishing By Using the Maven Plugin](https://central.sonatype.org/publish/publish-portal-maven/)

    mvn versions:set -DnewVersion=1.2.3
    mvn versions:set-scm-tag -DnewTag=1.2.3
    git commit -m "Set stable 1.2.3 version for release"

    # Update site Apidocs

    mvn clean
    link-jdk-sources
    mvn package -P apidoc javadoc:aggregate-jar -DskipTests

    rm -rf docs/apidocs/
    cp -r target/reports/apidocs/ docs/
    git add docs/apidocs/
    git commit -m "Update the site apidocs for the 1.2.3 release"

    # Tag sources, deploy, and push upstream

    git tag 1.2.3 -m "Release 1.2.3 to Maven Central"

    mvn deploy -P release

    git push origin 1.2.3 master

## [Publishing Your Components – Component Validation](https://central.sonatype.org/publish/publish-portal-guide/#component-validation)
