team: my-team
pipeline: my-pipeline

tasks:
  - type: buildpack
    name: deploy
    buildpacks:
      - paketobuildpacks/java:21.4.0
      - paketobuildpacks/nodejs:1.3.0

  - type: run
    name: test
    script: ./test.sh
