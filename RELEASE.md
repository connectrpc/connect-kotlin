# Releasing connect-kotlin

Connect-Kotlin’s repository has a release workflow which publishes connect-kotlin
artifacts to public maven. The release process was intentionally designed to be
as friction-free as possible — the result is that you just create/tag a release
in GitHub, and everything else is automated after that.

Using the Github UI, create a new release like so:
* Under “Choose a tag”, type in “vX.Y.Z” to create a new tag for the release upon publish.
  Note: The release job does infer the version from the release string and expects versions
  to start with `v` (e.g. `v0.1.0`).
* Target the main branch.
* Title the Release the same as the tag: “vX.Y.Z”.
* Click “set as latest release”.
* Set the last version as the “Previous tag”.
* Click “Generate release notes” to autogenerate release notes.
* Edit the release notes.
   * Tweak the change description for each if necessary so it summarizes the salient
     aspect(s) of the change in a single sentence. Detail is not needed as a user could
     follow the link to the relevant PR. (Potentially take a pass at PR descriptions
     and revise to increase clarity for users that visit the PRs from the release notes.)
   * Related commits can be grouped together with a single entry that has links to all
     relevant PRs (and attributes all relevant contributors).
   * A summary may be added if warranted.
   * The items in the list should be broken up into sub-categories. The typical
     sub-categories to use follow:
      * **API Improvements**: If the API was changed in any backwards-incompatible ways
        to improve developer experience, mention those here. Details on breaking changes
        should accompany the change log with a separate section titled "API Updates".
        (This category should go away once we reach a stable API and a v1.0.)
      * **Enhancements**: New features or additions/improvements to existing features.
      * **Bugfixes**: Self-explanatory -- fixes to defects.
      * **Other Changes**: Other noteworthy changes in the codebase or tests. In some
        way these could be considered enhancements, but they are more for maintenance
        and aid future contributors vs. providing new features to users. Use your
        best judgement when deciding if something warrants appearing here. Things like
        dependency updates and the like do _not_ warrant appearing here and should be
        removed from the auto-generated release notes.
   * If there were API changes that are not backwards compatible, add an "API Updates"
     section that enumerates the changes via a bullet list per class/type that was
     incompatibly changed.
   * An example of past release notes that contain many of these sections can be
     reviewed here: https://github.com/connectrpc/connect-kotlin/releases/tag/v0.5.0
* Click "Save Draft" and then share the link to have the notes reviewed by at least one
  other [maintainer](https://github.com/connectrpc/connect-kotlin/blob/main/MAINTAINERS.md).
* After the notes are approved (after some potential iteration and revision), you can
  finally create the release by clicking "Publish Release".

After the GitHub release has been created, you can verify that the rest of the process
completes successfully by following the corresponding run of the "release" GitHub
workflow: https://github.com/connectrpc/connect-kotlin/actions/workflows/release.yml

Once the workflow is complete, the artifacts have been uploaded to
https://s01.oss.sonatype.org/content/repositories/releases/com/connectrpc/.
After some delay, they will be asynchronously published to Maven Central:
https://repo1.maven.org/maven2/com/connectrpc/.
