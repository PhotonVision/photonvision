# Notebook

In the first cell of the RKNN conversion notebook, the installation script uses a structured list of dictionaries to define the download URLs and filenames for required scripts. Each dictionary includes a `url` (a permalink to a specific commit) and the corresponding `filename`.

Please ensure that all URLs in this array use permalinks—that is, links pointing to a specific commit hash rather than a branch name (e.g., main). This guarantees that the correct version of each script is always fetched, and prevents unexpected changes if the repository is updated in the future.

You typically won’t need to update these permalinks unless one of the referenced scripts is modified. In that case, update the commit hash in the URLs accordingly.
