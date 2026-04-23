# History

## [1.1.0](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.10...v1.1.0)

* Add support for updating Paketo buildpacks in Halfpipe pipelines (`.halfpipe.io` files)
* Add support for updating Paketo buildpacks in GitHub Actions workflows
* Document CNB buildpack scanning in README
* Replace semver library to allow non-strict version parsing
* Dependency updates

## [1.0.10](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.9...v1.0.10)

* Use current version of `actions/checkout` in example
* Fix duplicate execution of checks
* Run tests on all PRs against main
* Dependency updates

## [1.0.9](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.8...v1.0.9)

* Add explicit dependency on `junit-platform-launcher` to prepare for Gradle 9.0
* Dependency updates

## [1.0.8](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.7...v1.0.8)

* Add documentation for GitHub job summary
* Add CodeQL security scanning
* Add Gradle dependency submission
* Add Gradle wrapper validation and auto-update actions

## [1.0.7](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.6...v1.0.7)

* Upgrade to JDK 17
* Log process stdout and stderr as they happen rather than waiting for completion
* Dependency updates

## [1.0.6](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.5...v1.0.6)

* Add GitHub job summary, disabled by default and enabled by setting `GITHUB_STEP_SUMMARY_ENABLED=true`

## [1.0.5](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.4...v1.0.5)

* Exit with non-zero exit code on failure to publish
* Print errors to standard error output stream

## [1.0.4](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.3...v1.0.4)

* Add GitHub action branding
* Add Dependabot configuration
* Add documentation for keeping the action up-to-date with Dependabot

## [1.0.3](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.2...v1.0.3)

* Rename to `cf-buildpack-update-action`
* Add support for parsing built-in CF buildpacks
* Extend manifest file detection to include YAML files with `cf` in the name

## [1.0.2](https://github.com/springernature/cf-buildpack-update-action/compare/v1.0.1...v1.0.2)

* Make version parsing stricter to avoid attempting updates for buildpacks pinned to a branch or non-semver tag
* Open sourced under GPL 3

## 1.0.1

* Initial release
