# buildpack update action

Create pull requests to update Cloud Foundry buildpacks in manifest files

## Example usage

    on: [push]
    
    jobs:
      buildpack_updates_job:
        runs-on: ee-runner
        name: buildpack updates
        steps:
          - name: Check out the repo
            uses: actions/checkout@v2
          - name: run buildpack-update-action
            uses: springernature/buildpack-update-action@v17
            env:
              GITHUB_TOKEN: ${{ secrets.PERSONAL_GITHUB_TOKEN }}
              AUTHOR_EMAIL: team-payzilla@springernature.com
              AUTHOR_NAME: buildpack-update-action
