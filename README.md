# Buildpack update action

Create pull requests to update Cloud Foundry buildpacks in manifest files.

## Why?

Aiming for reproducible deployments it's a necessary step to pin a buildpack in a project to a specific version in the Cloud Foundry manifest, so it will always use the one you specify.

The disadvantage of pinning is that any improvement in a newer version is not automatically taken over to the project.

With this GitHub action a pull request will be created if there is a newer version of a buildpack available. That way the project can stay up-to-date but with a conscious and deliberate change, traceable in version control.

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
            uses: springernature/buildpack-update-action@v1.0.1
            env:
              GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
              AUTHOR_EMAIL: your-team-email-address@springernature.com
              AUTHOR_NAME: Buildpack Update Action

## Development

Before submitting any pull requests, please ensure that you have adhered to the [contribution guidelines][contrib].

## Roadmap

* enhance documentation
* have an automated release process? 
* improve build time
* make it configurable, see [Dependabot config](https://docs.github.com/en/code-security/supply-chain-security/keeping-your-dependencies-updated-automatically/configuration-options-for-dependency-updates) for ideas

## License

[GPL 3][license]

Copyright Springer Nature

[contrib]: CONTRIBUTING.md
[history]: HISTORY.md
[license]: LICENSE 
