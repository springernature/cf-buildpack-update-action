# Buildpack update action

Create pull requests to update Cloud Foundry buildpacks in manifest files.

## Why?

Aiming for reproducible deployments it's a necessary step to pin a buildpack in a project to a specific version in the
Cloud Foundry manifest, so it will always use the one you specify.

The disadvantage of pinning is that any improvement in a newer version is not automatically taken over to the project.

With this GitHub action a pull request will be created if there is a newer version of a buildpack available. That way
the project can stay up-to-date but with a conscious and deliberate change, traceable in version control.

## Example usage

Create a file in your repo called `.github/workflows/buildpack-update.yml` and in it put this code (remember to update `your-team-email-address@springernature.com` to one that is correct for your team)
 
    name: buildpack-update
    on:
      schedule:
        - cron: '0 4 * * 1-5' # Every workday at 04:00 UTC
      workflow_dispatch:
    
    jobs:
      buildpack_updates_job:
        runs-on: ee-runner
        timeout-minutes: 30
        name: buildpack updates
        steps:
          - name: Check out the repo
            uses: actions/checkout@v4
          - name: run cf-buildpack-update-action
            uses: springernature/cf-buildpack-update-action@v1.0.10
            env:
              GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
              AUTHOR_EMAIL: your-team-email-address@springernature.com
              AUTHOR_NAME: Buildpack Update Action
              GITHUB_STEP_SUMMARY_ENABLED: true

This should be picked up automatically in GitHub as a new Action and produce a PR (Pull Request) with the buildpack
version changes whenever a new version is available.
Just accept and merge the PR and you will be up-to-date.

### GitHub token and running automated tests
From GitHub [documentation](https://docs.github.com/en/actions/using-workflows/triggering-a-workflow#triggering-a-workflow-from-a-workflow):  
*If you do want to trigger a workflow from within a workflow run, you can use a GitHub App installation access token or a personal access token instead of GITHUB_TOKEN to trigger events that require a token.*
So, if the opened PR should run some automated tests, you will need a PAT (Personal Access token) or a GitHub app installation access token instead of the normal GitHub token.

### GitHub step summary

When setting `GITHUB_STEP_SUMMARY_ENABLED` to `true` (default is `false`) a job summary is created, 
see [example output](https://github.com/springernature/dpas/actions/runs/3691628035/attempts/1#summary-10080794385).

## Keep *your* action up-to-date

You can configure dependabot to keep your action which uses `cf-buildpack-update-action` up-to-date for every new
version on `cf-buildpack-update-action`.

[Enabling Dependabot version updates for actions — Keeping your actions up to date with Dependabot - GitHub Docs](https://docs.github.com/en/code-security/supply-chain-security/keeping-your-dependencies-updated-automatically/keeping-your-actions-up-to-date-with-dependabot#enabling-dependabot-version-updates-for-actions)


> **Enabling Dependabot version updates for actions**
> 1. Create a *dependabot.yml* configuration file. If you have already enabled Dependabot version updates for other
     ecosystems or package managers, simply open the existing *dependabot.yml* file.
> 1. Specify `"github-actions"` as a `package-ecosystem` to monitor.
> 1. Set the `directory` to `"/"` to check for workflow files in `.github/workflows`.
> 1. Set a `schedule.interval` to specify how often to check for new versions.
> 1. Check the *dependabot.yml* configuration file in to the `.github` directory of the repository. If you have edited
     an existing file, save your changes.

## Development

Before submitting any pull requests, please ensure that you have adhered to the [contribution guidelines][contrib].

## Roadmap

* enhance documentation
* have an automated release process?
* improve build time
* make it configurable,
  see [Dependabot config](https://docs.github.com/en/code-security/supply-chain-security/keeping-your-dependencies-updated-automatically/configuration-options-for-dependency-updates)
  for ideas

## License

[GPL 3][license]

Copyright Springer Nature

[contrib]: CONTRIBUTING.md

[history]: HISTORY.md

[license]: LICENSE 
