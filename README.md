# Buildpack update action

Create pull requests to update Cloud Foundry buildpacks in manifest files.

## Example usage

    on:
      schedule:
        - cron: '0 4 * * 1-5' # Every workday at 0500 UTC
      workflow_dispatch:
    
    jobs:
      buildpack_updates_job:
        runs-on: ee-runner
        name: buildpack updates
        steps:
          - name: Check out the repo
            uses: actions/checkout@v2
          - name: run buildpack-update-action
            uses: springernature/buildpack-update-action@v0.0.19
            env:
              GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
              AUTHOR_EMAIL: your-team-email-address@springernature.com
              AUTHOR_NAME: Buildpack Update Action

## Roadmap

* enhance documentation
* create a proper release
  * have an automated release process? 
* improve build time
* make it configurable, see [Dependabot config](https://docs.github.com/en/code-security/supply-chain-security/keeping-your-dependencies-updated-automatically/configuration-options-for-dependency-updates) for ideas
