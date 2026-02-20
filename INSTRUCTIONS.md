# Starred Repositories Updater

This project automatically generates a markdown file with a list of your GitHub starred repositories using a GitHub Action and Babashka (Clojure).

## How it works

1. A GitHub Action workflow runs on a schedule (daily), on push to `main` when the script changes, or manually.
2. The workflow uses Babashka to fetch your starred repositories via GitHub API.
3. It generates a markdown file `README.md` with a table of repositories.
4. The updated file is automatically committed back to the repository.

## Setup

1. **Fork or clone** this repository.
2. **No additional secrets needed** if you want to use the default `GITHUB_TOKEN` provided by GitHub Actions (it will have access to the repository and the user associated with the token).
3. The workflow will automatically run and create `README.md`.

## Customization

- To change the schedule, edit `.github/workflows/update-starred-repos.yml` and modify the `cron` expression.
- To change the output filename, edit `scripts/update_starred_repos.clj` and modify the `output-file` variable.
- To modify the markdown format, edit the `generate-markdown` function in the script.

## Manual run

You can manually trigger the workflow from the "Actions" tab in your GitHub repository.

## Local development

You can run the script locally if you have Babashka installed:

```bash
bb scripts/update_starred_repos.clj
```

Make sure to set the `GITHUB_TOKEN` environment variable with a personal access token if you want to avoid rate limiting.

```bash
export GITHUB_TOKEN=your_token_here
```

## License

[UNLICENSE](UNLICENSE)
