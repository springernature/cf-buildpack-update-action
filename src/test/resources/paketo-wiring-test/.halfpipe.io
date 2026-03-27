team: my-team
pipeline: my-pipeline

tasks:
  - type: buildpack
    name: deploy
    buildpacks:
      - paketobuildpacks/java:21.4.0
