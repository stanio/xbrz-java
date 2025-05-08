## [Publishing By Using the Maven Plugin](https://central.sonatype.org/publish/publish-portal-maven/)

    mvn versions:set -DnewVersion=1.2.3
    mvn versions:set-scm-tag -DnewTag=1.2.3

    git commit -m "Set stable 1.2.3 version for release"

    git tag 1.2.3 -m "Release 1.2.3 to Maven Central"

    mvn clean deploy -P release

## [Publishing Your Components – Component Validation](https://central.sonatype.org/publish/publish-portal-guide/#component-validation)

## Update site Apidocs

    mvn clean package -P apidoc javadoc:aggregate-jar

    rm -rf docs/apidocs/
    cp -r target/apidocs/ docs/
    git add docs/apidocs/
    git commit
