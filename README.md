This repository contains the SubKt template that was used for the release process at (a now defunct) fansubbing group called Good Job! Media.
For more information on SubKt templates, please check out the [official documentation](https://github.com/Myaamori/SubKt?tab=readme-ov-file#table-of-contents).

It also additionally contains a CI/CD GitHub action that merges, uploads as artifacts and publishes scripts to a batch repository. (Thanks to @rcombs for helping out with parts of the code)

Use `-Prelease=xxxx` to switch between `TV`, `BD`, and any other defined versions.

Note: The batch releasing process in the SubKt template has not been used in a production setting so you may encounter some unexpected bugs.